use std::{
    collections::{BTreeMap, HashMap},
    convert::{TryFrom, TryInto},
    io::Cursor,
};

use base64::encode;
use js_int::UInt;
use ruma::{
    api::{
        client::r0::{
            keys::{
                claim_keys::Response as KeysClaimResponse, get_keys::Response as KeysQueryResponse,
                upload_keys::Response as KeysUploadResponse,
            },
            sync::sync_events::{DeviceLists as RumaDeviceLists, ToDevice},
            to_device::send_event_to_device::Response as ToDeviceResponse,
        },
        IncomingResponse,
    },
    events::{
        key::verification::VerificationMethod, room::encrypted::EncryptedEventContent,
        AnyMessageEventContent, EventContent, SyncMessageEvent,
    },
    DeviceKeyAlgorithm, RoomId, UserId,
};
use serde::{Deserialize, Serialize};
use serde_json::value::RawValue;
use tokio::runtime::Runtime;

use matrix_sdk_common::{deserialized_responses::AlgorithmInfo, uuid::Uuid};
use matrix_sdk_crypto::{
    decrypt_key_export, encrypt_key_export, EncryptionSettings, LocalTrust,
    OlmMachine as InnerMachine, QrVerification as InnerQr, Sas as InnerSas,
    Verification as RustVerification, VerificationRequest as InnerVerificationRequest,
};

use crate::{
    error::{CryptoStoreError, DecryptionError, MachineCreationError},
    responses::{response_from_string, OutgoingVerificationRequest, OwnedResponse},
    DecryptedEvent, Device, DeviceLists, KeyImportError, KeysImportResult, ProgressListener,
    Request, RequestType,
};

/// A high level state machine that handles E2EE for Matrix.
pub struct OlmMachine {
    inner: InnerMachine,
    runtime: Runtime,
}

pub enum Verification {
    SasV1 { sas: Sas },
    QrCodeV1 { qrcode: QrCode },
}

pub struct Sas {
    pub other_user_id: String,
    pub other_device_id: String,
    pub flow_id: String,
    pub room_id: Option<String>,
    pub have_we_confirmed: bool,
    pub is_cancelled: bool,
    pub is_done: bool,
    pub cancel_code: Option<String>,
    pub cancelled_by_us: Option<bool>,
    pub we_started: bool,
    pub can_be_presented: bool,
    pub supports_emoji: bool,
    pub timed_out: bool,
}

pub struct QrCode {
    pub other_user_id: String,
    pub flow_id: String,
    pub other_device_id: String,
    pub room_id: Option<String>,
    pub is_cancelled: bool,
    pub is_done: bool,
    pub we_started: bool,
    pub other_side_scanned: bool,
    pub has_been_confirmed: bool,
    pub cancel_code: Option<String>,
    pub cancelled_by_us: Option<bool>,
}

impl From<InnerQr> for QrCode {
    fn from(qr: InnerQr) -> Self {
        Self {
            other_user_id: qr.other_user_id().to_string(),
            flow_id: qr.flow_id().as_str().to_owned(),
            is_cancelled: qr.is_cancelled(),
            is_done: qr.is_done(),
            cancel_code: qr.cancel_code().map(|c| c.to_string()),
            cancelled_by_us: qr.cancelled_by_us(),
            we_started: qr.we_started(),
            other_side_scanned: qr.has_been_scanned(),
            has_been_confirmed: qr.has_been_confirmed(),
            other_device_id: qr.other_device_id().to_string(),
            room_id: qr.room_id().map(|r| r.to_string()),
        }
    }
}

pub struct StartSasResult {
    pub sas: Sas,
    pub request: OutgoingVerificationRequest,
}

impl From<InnerSas> for Sas {
    fn from(sas: InnerSas) -> Self {
        Self {
            other_user_id: sas.other_user_id().to_string(),
            other_device_id: sas.other_device_id().to_string(),
            flow_id: sas.flow_id().as_str().to_owned(),
            is_cancelled: sas.is_cancelled(),
            is_done: sas.is_done(),
            can_be_presented: sas.can_be_presented(),
            timed_out: sas.timed_out(),
            supports_emoji: sas.supports_emoji(),
            have_we_confirmed: sas.have_we_confirmed(),
            cancel_code: sas.cancel_code().map(|c| c.as_str().to_owned()),
            we_started: sas.we_started(),
            room_id: sas.room_id().map(|r| r.to_string()),
            cancelled_by_us: sas.cancelled_by_us(),
        }
    }
}

pub struct VerificationRequest {
    pub other_user_id: String,
    pub other_device_id: Option<String>,
    pub flow_id: String,
    pub is_cancelled: bool,
    pub is_done: bool,
    pub is_ready: bool,
    pub room_id: Option<String>,
    pub cancel_code: Option<String>,
    pub we_started: bool,
    pub is_passive: bool,
    pub their_methods: Option<Vec<String>>,
    pub our_methods: Option<Vec<String>>,
}

impl From<InnerVerificationRequest> for VerificationRequest {
    fn from(v: InnerVerificationRequest) -> Self {
        Self {
            other_user_id: v.other_user().to_string(),
            other_device_id: v.other_device_id().map(|d| d.to_string()),
            flow_id: v.flow_id().as_str().to_owned(),
            is_cancelled: v.is_cancelled(),
            is_done: v.is_done(),
            is_ready: v.is_ready(),
            room_id: v.room_id().map(|r| r.to_string()),
            cancel_code: v.cancel_code().map(|c| c.as_str().to_owned()),
            we_started: v.we_started(),
            is_passive: v.is_passive(),
            their_methods: v
                .their_supported_methods()
                .map(|v| v.into_iter().map(|m| m.to_string()).collect()),
            our_methods: v
                .our_supported_methods()
                .map(|v| v.into_iter().map(|m| m.to_string()).collect()),
        }
    }
}

/// A pair of outgoing room key requests, both of those are sendToDevice
/// requests.
pub struct KeyRequestPair {
    /// The optional cancellation, this is None if no previous key request was
    /// sent out for this key, thus it doesn't need to be cancelled.
    pub cancellation: Option<Request>,
    /// The actual key request.
    pub key_request: Request,
}

impl OlmMachine {
    /// Create a new `OlmMachine`
    ///
    /// # Arguments
    ///
    /// * `user_id` - The unique ID of the user that owns this machine.
    ///
    /// * `device_id` - The unique ID of the device that owns this machine.
    ///
    /// * `path` - The path where the state of the machine should be persisted.
    pub fn new(user_id: &str, device_id: &str, path: &str) -> Result<Self, MachineCreationError> {
        let user_id = UserId::try_from(user_id)?;
        let device_id = device_id.into();
        let runtime = Runtime::new().unwrap();

        Ok(OlmMachine {
            inner: runtime.block_on(InnerMachine::new_with_default_store(
                &user_id, device_id, path, None,
            ))?,
            runtime,
        })
    }

    /// Get the user ID of the owner of this `OlmMachine`.
    pub fn user_id(&self) -> String {
        self.inner.user_id().to_string()
    }

    /// Get the device ID of the device of this `OlmMachine`.
    pub fn device_id(&self) -> String {
        self.inner.device_id().to_string()
    }

    /// Get a `Device` from the store.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The id of the device owner.
    ///
    /// * `device_id` - The id of the device itself.
    pub fn get_device(
        &self,
        user_id: &str,
        device_id: &str,
    ) -> Result<Option<Device>, CryptoStoreError> {
        let user_id = UserId::try_from(user_id)?;

        Ok(self
            .runtime
            .block_on(self.inner.get_device(&user_id, device_id.into()))?
            .map(|d| d.into()))
    }

    pub fn mark_device_as_trusted(
        &self,
        user_id: &str,
        device_id: &str,
    ) -> Result<(), CryptoStoreError> {
        let user_id = UserId::try_from(user_id)?;

        let device = self
            .runtime
            .block_on(self.inner.get_device(&user_id, device_id.into()))?;

        if let Some(device) = device {
            self.runtime
                .block_on(device.set_local_trust(LocalTrust::Verified))?;
        }

        Ok(())
    }

    /// Get all devices of an user.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The id of the device owner.
    pub fn get_user_devices(&self, user_id: &str) -> Result<Vec<Device>, CryptoStoreError> {
        let user_id = UserId::try_from(user_id)?;

        Ok(self
            .runtime
            .block_on(self.inner.get_user_devices(&user_id))?
            .devices()
            .map(|d| d.into())
            .collect())
    }

    /// Get our own identity keys.
    pub fn identity_keys(&self) -> HashMap<String, String> {
        self.inner
            .identity_keys()
            .iter()
            .map(|(k, v)| (k.to_owned(), v.to_owned()))
            .collect()
    }

    /// Get the list of outgoing requests that need to be sent to the
    /// homeserver.
    ///
    /// After the request was sent out and a successful response was received
    /// the response body should be passed back to the state machine using the
    /// [mark_request_as_sent()](#method.mark_request_as_sent) method.
    ///
    /// **Note**: This method call should be locked per call.
    pub fn outgoing_requests(&self) -> Result<Vec<Request>, CryptoStoreError> {
        Ok(self
            .runtime
            .block_on(self.inner.outgoing_requests())?
            .into_iter()
            .map(|r| r.into())
            .collect())
    }

    /// Mark a request that was sent to the server as sent.
    ///
    /// # Arguments
    ///
    /// * `request_id` - The unique ID of the request that was sent out. This
    ///     needs to be an UUID.
    ///
    /// * `request_type` - The type of the request that was sent out.
    ///
    /// * `response_body` - The body of the response that was received.
    pub fn mark_request_as_sent(
        &self,
        request_id: &str,
        request_type: RequestType,
        response_body: &str,
    ) -> Result<(), CryptoStoreError> {
        let id = Uuid::parse_str(request_id).expect("Can't parse request id");

        let response = response_from_string(&response_body);

        let response: OwnedResponse = match request_type {
            RequestType::KeysUpload => {
                KeysUploadResponse::try_from_http_response(response).map(Into::into)
            }
            RequestType::KeysQuery => {
                KeysQueryResponse::try_from_http_response(response).map(Into::into)
            }
            RequestType::ToDevice => {
                ToDeviceResponse::try_from_http_response(response).map(Into::into)
            }
            RequestType::KeysClaim => {
                KeysClaimResponse::try_from_http_response(response).map(Into::into)
            }
        }
        .expect("Can't convert json string to response");

        self.runtime
            .block_on(self.inner.mark_request_as_sent(&id, &response))?;

        Ok(())
    }

    /// Let the state machine know about E2EE related sync changes that we
    /// received from the server.
    ///
    /// This needs to be called after every sync, ideally before processing
    /// any other sync changes.
    ///
    /// # Arguments
    ///
    /// * `events` - A serialized array of to-device events we received in the
    ///     current sync response.
    ///
    /// * `device_changes` - The list of devices that have changed in some way
    /// since the previous sync.
    ///
    /// * `key_counts` - The map of uploaded one-time key types and counts.
    pub fn receive_sync_changes(
        &self,
        events: &str,
        device_changes: DeviceLists,
        key_counts: HashMap<String, i32>,
    ) -> Result<String, CryptoStoreError> {
        let events: ToDevice = serde_json::from_str(events)?;
        let device_changes: RumaDeviceLists = device_changes.into();
        let key_counts: BTreeMap<DeviceKeyAlgorithm, UInt> = key_counts
            .into_iter()
            .map(|(k, v)| {
                (
                    DeviceKeyAlgorithm::from(k),
                    v.clamp(0, i32::MAX)
                        .try_into()
                        .expect("Couldn't convert key counts into an UInt"),
                )
            })
            .collect();

        let events = self.runtime.block_on(self.inner.receive_sync_changes(
            events,
            &device_changes,
            &key_counts,
        ))?;

        Ok(serde_json::to_string(&events)?)
    }

    /// Add the given list of users to be tracked, triggering a key query request
    /// for them.
    ///
    /// *Note*: Only users that aren't already tracked will be considered for an
    /// update. It's safe to call this with already tracked users, it won't
    /// result in excessive keys query requests.
    ///
    /// # Arguments
    ///
    /// `users` - The users that should be queued up for a key query.
    pub fn update_tracked_users(&self, users: Vec<String>) {
        let users: Vec<UserId> = users
            .into_iter()
            .filter_map(|u| UserId::try_from(u).ok())
            .collect();

        self.runtime
            .block_on(self.inner.update_tracked_users(users.iter()));
    }

    /// Generate one-time key claiming requests for all the users we are missing
    /// sessions for.
    ///
    /// After the request was sent out and a successful response was received
    /// the response body should be passed back to the state machine using the
    /// [mark_request_as_sent()](#method.mark_request_as_sent) method.
    ///
    /// This method should be called every time before a call to
    /// [`share_group_session()`](#method.share_group_session) is made.
    ///
    /// # Arguments
    ///
    /// * `users` - The list of users for which we would like to establish 1:1
    /// Olm sessions for.
    pub fn get_missing_sessions(
        &self,
        users: Vec<String>,
    ) -> Result<Option<Request>, CryptoStoreError> {
        let users: Vec<UserId> = users
            .into_iter()
            .filter_map(|u| UserId::try_from(u).ok())
            .collect();

        Ok(self
            .runtime
            .block_on(self.inner.get_missing_sessions(users.iter()))?
            .map(|(request_id, request)| Request::KeysClaim {
                request_id: request_id.to_string(),
                one_time_keys: request
                    .one_time_keys
                    .into_iter()
                    .map(|(u, d)| {
                        (
                            u.to_string(),
                            d.into_iter()
                                .map(|(k, v)| (k.to_string(), v.to_string()))
                                .collect(),
                        )
                    })
                    .collect(),
            }))
    }

    /// Share a room key with the given list of users for the given room.
    ///
    /// After the request was sent out and a successful response was received
    /// the response body should be passed back to the state machine using the
    /// [mark_request_as_sent()](#method.mark_request_as_sent) method.
    ///
    /// This method should be called every time before a call to
    /// [`encrypt()`](#method.encrypt) with the given `room_id` is made.
    ///
    /// # Arguments
    ///
    /// * `room_id` - The unique id of the room, note that this doesn't strictly
    /// need to be a Matrix room, it just needs to be an unique identifier for
    /// the group that will participate in the conversation.
    ///
    /// * `users` - The list of users which are considered to be members of the
    /// room and should receive the room key.
    pub fn share_room_key(
        &self,
        room_id: &str,
        users: Vec<String>,
    ) -> Result<Vec<Request>, CryptoStoreError> {
        let users: Vec<UserId> = users
            .into_iter()
            .filter_map(|u| UserId::try_from(u).ok())
            .collect();

        let room_id = RoomId::try_from(room_id)?;
        let requests = self.runtime.block_on(self.inner.share_group_session(
            &room_id,
            users.iter(),
            EncryptionSettings::default(),
        ))?;

        Ok(requests.into_iter().map(|r| (&*r).into()).collect())
    }

    /// Encrypt the given event with the given type and content for the given
    /// room.
    ///
    /// **Note**: A room key needs to be shared with the group of users that are
    /// members in the given room. If this is not done this method will panic.
    ///
    /// The usual flow to encrypt an event using this state machine is as
    /// follows:
    ///
    /// 1. Get the one-time key claim request to establish 1:1 Olm sessions for
    ///    the room members of the room we wish to participate in. This is done
    ///    using the [`get_missing_sessions()`](#method.get_missing_sessions)
    ///    method. This method call should be locked per call.
    ///
    /// 2. Share a room key with all the room members using the
    ///    [`share_group_session()`](#method.share_group_session). This method
    ///    call should be locked per room.
    ///
    /// 3. Encrypt the event using this method.
    ///
    /// 4. Send the encrypted event to the server.
    ///
    /// After the room key is shared steps 1 and 2 will become noops, unless
    /// there's some changes in the room membership or in the list of devices a
    /// member has.
    ///
    /// # Arguments
    ///
    /// * `room_id` - The unique id of the room where the event will be sent to.
    ///
    /// * `even_type` - The type of the event.
    ///
    /// * `content` - The serialized content of the event.
    pub fn encrypt(
        &self,
        room_id: &str,
        event_type: &str,
        content: &str,
    ) -> Result<String, CryptoStoreError> {
        let room_id = RoomId::try_from(room_id)?;
        let content: Box<RawValue> = serde_json::from_str(content)?;

        let content = AnyMessageEventContent::from_parts(event_type, &content)?;
        let encrypted_content = self
            .runtime
            .block_on(self.inner.encrypt(&room_id, content))
            .unwrap();

        Ok(serde_json::to_string(&encrypted_content)?)
    }

    /// Decrypt the given event that was sent in the given room.
    ///
    /// # Arguments
    ///
    /// * `event` - The serialized encrypted version of the event.
    ///
    /// * `room_id` - The unique id of the room where the event was sent to.
    pub fn decrypt_room_event(
        &self,
        event: &str,
        room_id: &str,
    ) -> Result<DecryptedEvent, DecryptionError> {
        // Element Android wants only the content and the type and will create a
        // decrypted event with those two itself, this struct makes sure we
        // throw away all the other fields.
        #[derive(Deserialize, Serialize)]
        struct Event<'a> {
            #[serde(rename = "type")]
            event_type: String,
            #[serde(borrow)]
            content: &'a RawValue,
        }

        let event: SyncMessageEvent<EncryptedEventContent> = serde_json::from_str(event)?;
        let room_id = RoomId::try_from(room_id)?;

        let decrypted = self
            .runtime
            .block_on(self.inner.decrypt_room_event(&event, &room_id))?;

        let encryption_info = decrypted
            .encryption_info
            .expect("Decrypted event didn't contain any encryption info");

        let event_json: Event = serde_json::from_str(decrypted.event.json().get())?;

        Ok(match &encryption_info.algorithm_info {
            AlgorithmInfo::MegolmV1AesSha2 {
                curve25519_key,
                sender_claimed_keys,
                forwarding_curve25519_key_chain,
            } => DecryptedEvent {
                clear_event: serde_json::to_string(&event_json)?,
                sender_curve25519_key: curve25519_key.to_owned(),
                claimed_ed25519_key: sender_claimed_keys
                    .get(&DeviceKeyAlgorithm::Ed25519)
                    .cloned(),
                forwarding_curve25519_chain: forwarding_curve25519_key_chain.to_owned(),
            },
        })
    }

    /// Request or re-request a room key that was used to encrypt the given
    /// event.
    ///
    /// # Arguments
    ///
    /// * `event` - The undecryptable event that we would wish to request a room
    /// key for.
    ///
    /// * `room_id` - The id of the room the event was sent to.
    pub fn request_room_key(
        &self,
        event: &str,
        room_id: &str,
    ) -> Result<KeyRequestPair, DecryptionError> {
        let event: SyncMessageEvent<EncryptedEventContent> = serde_json::from_str(event)?;
        let room_id = RoomId::try_from(room_id)?;

        let (cancel, request) = self
            .runtime
            .block_on(self.inner.request_room_key(&event, &room_id))?;

        let cancellation = cancel.map(|r| r.into());
        let key_request = request.into();

        Ok(KeyRequestPair {
            cancellation,
            key_request,
        })
    }

    /// Export all of our room keys.
    ///
    /// # Arguments
    ///
    /// * `passphrase` - The passphrase that should be used to encrypt the key
    /// export.
    ///
    /// * `rounds` - The number of rounds that should be used when expanding the
    /// passphrase into an key.
    pub fn export_keys(&self, passphrase: &str, rounds: i32) -> Result<String, CryptoStoreError> {
        let keys = self.runtime.block_on(self.inner.export_keys(|_| true))?;

        let encrypted = encrypt_key_export(&keys, passphrase, rounds as u32)
            .map_err(CryptoStoreError::Serialization)?;

        Ok(encrypted)
    }

    /// Import room keys from the given serialized key export.
    ///
    /// # Arguments
    ///
    /// * `keys` - The serialized version of the key export.
    ///
    /// * `passphrase` - The passphrase that was used to encrypt the key export.
    ///
    /// * `progress_listener` - A callback that can be used to introspect the
    /// progress of the key import.
    pub fn import_keys(
        &self,
        keys: &str,
        passphrase: &str,
        progress_listener: Box<dyn ProgressListener>,
    ) -> Result<KeysImportResult, KeyImportError> {
        let keys = Cursor::new(keys);
        let keys = decrypt_key_export(keys, passphrase)?;

        let listener = |progress: usize, total: usize| {
            progress_listener.on_progress(progress as i32, total as i32)
        };

        let result = self
            .runtime
            .block_on(self.inner.import_keys(keys, listener))?;

        Ok(KeysImportResult {
            total: result.1 as i32,
            imported: result.0 as i32,
        })
    }

    /// Discard the currently active room key for the given room if there is
    /// one.
    pub fn discard_room_key(&self, room_id: &str) -> Result<(), CryptoStoreError> {
        let room_id = RoomId::try_from(room_id)?;

        self.runtime
            .block_on(self.inner.invalidate_group_session(&room_id))?;

        Ok(())
    }

    pub fn get_verification_requests(&self, user_id: &str) -> Vec<VerificationRequest> {
        let user_id = if let Ok(user_id) = UserId::try_from(user_id) {
            user_id
        } else {
            return vec![];
        };

        self.inner
            .get_verification_requests(&user_id)
            .into_iter()
            .map(|v| v.into())
            .collect()
    }

    pub fn get_verification_request(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Option<VerificationRequest> {
        let user_id = UserId::try_from(user_id).ok()?;

        self.inner
            .get_verification_request(&user_id, flow_id)
            .map(|v| v.into())
    }

    pub fn accept_verification_request(
        &self,
        user_id: &str,
        flow_id: &str,
        methods: Vec<String>,
    ) -> Option<OutgoingVerificationRequest> {
        let user_id = UserId::try_from(user_id).ok()?;
        let methods = methods
            .into_iter()
            .map(|m| VerificationMethod::from(m))
            .collect();

        if let Some(verification) = self.inner.get_verification_request(&user_id, flow_id) {
            verification.accept_with_methods(methods).map(|r| r.into())
        } else {
            None
        }
    }

    pub fn request_verification(&self, user_id: &str) {
        let _user_id = UserId::try_from(user_id).unwrap();
        todo!()
    }

    pub fn get_verification(&self, user_id: &str, flow_id: &str) -> Option<Verification> {
        let user_id = UserId::try_from(user_id).ok()?;
        self.inner
            .get_verification(&user_id, flow_id)
            .map(|v| match v {
                RustVerification::SasV1(s) => Verification::SasV1 { sas: s.into() },
                RustVerification::QrV1(qr) => Verification::QrCodeV1 { qrcode: qr.into() },
            })
    }

    pub fn start_qr_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Result<Option<QrCode>, CryptoStoreError> {
        let user_id = UserId::try_from(user_id)?;

        if let Some(verification) = self.inner.get_verification_request(&user_id, flow_id) {
            Ok(self
                .runtime
                .block_on(verification.generate_qr_code())?
                .map(|qr| qr.into()))
        } else {
            Ok(None)
        }
    }

    pub fn generate_qr_code(&self, user_id: &str, flow_id: &str) -> Option<String> {
        let user_id = UserId::try_from(user_id).ok()?;
        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|v| {
                v.qr_v1()
                    .and_then(|qr| qr.to_bytes().map(|b| encode(b)).ok())
            })
    }

    pub fn start_sas_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Result<Option<StartSasResult>, CryptoStoreError> {
        let user_id = UserId::try_from(user_id)?;

        Ok(
            if let Some(verification) = self.inner.get_verification_request(&user_id, flow_id) {
                self.runtime
                    .block_on(verification.start_sas())?
                    .map(|(sas, r)| StartSasResult {
                        sas: sas.into(),
                        request: r.into(),
                    })
            } else {
                None
            },
        )
    }

    pub fn accept_sas_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Option<OutgoingVerificationRequest> {
        let user_id = UserId::try_from(user_id).ok()?;
        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|s| s.sas_v1())
            .and_then(|s| s.accept().map(|r| r.into()))
    }

    pub fn cancel_verification(
        &self,
        user_id: &str,
        flow_id: &str,
        cancel_code: &str,
    ) -> Option<OutgoingVerificationRequest> {
        let user_id = UserId::try_from(user_id).ok()?;

        if let Some(request) = self.inner.get_verification_request(&user_id, flow_id) {
            request.cancel().map(|r| r.into())
        } else if let Some(verification) = self.inner.get_verification(&user_id, flow_id) {
            match verification {
                RustVerification::SasV1(v) => {
                    v.cancel_with_code(cancel_code.into()).map(|r| r.into())
                }
                RustVerification::QrV1(v) => v.cancel().map(|r| r.into()),
            }
        } else {
            None
        }
    }

    pub fn confirm_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Result<Option<OutgoingVerificationRequest>, CryptoStoreError> {
        let user_id = UserId::try_from(user_id)?;

        Ok(
            if let Some(verification) = self.inner.get_verification(&user_id, flow_id) {
                match verification {
                    RustVerification::SasV1(v) => {
                        self.runtime.block_on(v.confirm())?.0.map(|r| r.into())
                    }
                    RustVerification::QrV1(v) => v.confirm_scanning().map(|r| r.into()),
                }
            } else {
                None
            },
        )
    }

    pub fn get_emoji_index(&self, user_id: &str, flow_id: &str) -> Option<Vec<i32>> {
        let user_id = UserId::try_from(user_id).ok()?;

        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|s| {
                s.sas_v1().and_then(|s| {
                    s.emoji_index()
                        .map(|v| v.iter().map(|i| (*i).into()).collect())
                })
            })
    }

    pub fn get_decimals(&self, user_id: &str, flow_id: &str) -> Option<Vec<i32>> {
        let user_id = UserId::try_from(user_id).ok()?;

        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|s| {
                s.sas_v1().and_then(|s| {
                    s.decimals()
                        .map(|v| [v.0.into(), v.1.into(), v.2.into()].to_vec())
                })
            })
    }
}
