use std::{
    collections::{BTreeMap, HashMap},
    convert::{TryFrom, TryInto},
    io::Cursor,
    ops::Deref,
};

use base64::{decode_config, encode, STANDARD_NO_PAD};
use js_int::UInt;
use ruma::{
    api::{
        client::r0::{
            backup::add_backup_keys::Response as KeysBackupResponse,
            keys::{
                claim_keys::Response as KeysClaimResponse, get_keys::Response as KeysQueryResponse,
                upload_keys::Response as KeysUploadResponse,
                upload_signatures::Response as SignatureUploadResponse,
            },
            sync::sync_events::{DeviceLists as RumaDeviceLists, ToDevice},
            to_device::send_event_to_device::Response as ToDeviceResponse,
        },
        IncomingResponse,
    },
    events::{
        key::verification::VerificationMethod, room::encrypted::RoomEncryptedEventContent,
        AnyMessageEventContent, EventContent, SyncMessageEvent,
    },
    DeviceKeyAlgorithm, EventId, RoomId, UserId,
};
use serde::{Deserialize, Serialize};
use serde_json::{value::RawValue, Value};
use tokio::runtime::Runtime;

use matrix_sdk_common::{deserialized_responses::AlgorithmInfo, uuid::Uuid};
use matrix_sdk_crypto::{
    backups::{MegolmV1BackupKey as RustBackupKey, RecoveryKey},
    decrypt_key_export, encrypt_key_export,
    matrix_qrcode::QrVerificationData,
    olm::ExportedRoomKey,
    EncryptionSettings, LocalTrust, OlmMachine as InnerMachine, UserIdentities,
    Verification as RustVerification,
};

use crate::{
    error::{CryptoStoreError, DecryptionError, SecretImportError, SignatureError},
    responses::{response_from_string, OutgoingVerificationRequest, OwnedResponse},
    BackupKeys, BootstrapCrossSigningResult, ConfirmVerificationResult, CrossSigningKeyExport,
    CrossSigningStatus, DecodeError, DecryptedEvent, Device, DeviceLists, KeyImportError,
    KeysImportResult, MegolmV1BackupKey, ProgressListener, QrCode, Request, RequestType,
    RequestVerificationResult, RoomKeyCounts, ScanResult, SignatureUploadRequest, StartSasResult,
    UserIdentity, Verification, VerificationRequest, parse_user_id,
};

/// A high level state machine that handles E2EE for Matrix.
pub struct OlmMachine {
    inner: InnerMachine,
    runtime: Runtime,
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
    pub fn new(user_id: &str, device_id: &str, path: &str) -> Result<Self, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;
        let device_id = device_id.into();
        let runtime = Runtime::new().expect("Couldn't create a tokio runtime");

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

    /// Get the display name of our own device.
    pub fn display_name(&self) -> Result<Option<String>, CryptoStoreError> {
        Ok(self.runtime.block_on(self.inner.display_name())?)
    }

    /// Get a cross signing user identity for the given user ID.
    pub fn get_identity(&self, user_id: &str) -> Result<Option<UserIdentity>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

        Ok(
            if let Some(identity) = self.runtime.block_on(self.inner.get_identity(&user_id))? {
                Some(self.runtime.block_on(UserIdentity::from_rust(identity))?)
            } else {
                None
            },
        )
    }

    /// Check if a user identity is considered to be verified by us.
    pub fn is_identity_verified(&self, user_id: &str) -> Result<bool, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

        Ok(
            if let Some(identity) = self.runtime.block_on(self.inner.get_identity(&user_id))? {
                match identity {
                    UserIdentities::Own(i) => i.is_verified(),
                    UserIdentities::Other(i) => i.verified(),
                }
            } else {
                false
            },
        )
    }

    /// Manually the user with the given user ID.
    ///
    /// This method will attempt to sign the user identity using either our
    /// private cross signing key, for other user identities, or our device keys
    /// for our own user identity.
    ///
    /// This methid can fail if we don't have the private part of our user-signing
    /// key.
    ///
    /// Returns a request that needs to be sent out for the user identity to be
    /// marked as verified.
    pub fn verify_identity(&self, user_id: &str) -> Result<SignatureUploadRequest, SignatureError> {
        let user_id = Box::<UserId>::try_from(user_id)?;

        let user_identity = self.runtime.block_on(self.inner.get_identity(&user_id))?;

        if let Some(user_identity) = user_identity {
            Ok(match user_identity {
                UserIdentities::Own(i) => self.runtime.block_on(i.verify())?,
                UserIdentities::Other(i) => self.runtime.block_on(i.verify())?,
            }
            .into())
        } else {
            Err(SignatureError::UnknownUserIdentity(user_id.to_string()))
        }
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
        let user_id = parse_user_id(user_id)?;

        Ok(self
            .runtime
            .block_on(self.inner.get_device(&user_id, device_id.into()))?
            .map(|d| d.into()))
    }

    /// Manually the device of the given user with the given device ID.
    ///
    /// This method will attempt to sign the device using our private cross
    /// signing key.
    ///
    /// This method will always fail if the device belongs to someone else, we
    /// can only sign our own devices.
    ///
    /// It can also fail if we don't have the private part of our self-signing
    /// key.
    ///
    /// Returns a request that needs to be sent out for the device to be marked
    /// as verified.
    pub fn verify_device(
        &self,
        user_id: &str,
        device_id: &str,
    ) -> Result<SignatureUploadRequest, SignatureError> {
        let user_id = Box::<UserId>::try_from(user_id)?;
        let device = self
            .runtime
            .block_on(self.inner.get_device(&user_id, device_id.into()))?;

        if let Some(device) = device {
            Ok(self.runtime.block_on(device.verify())?.into())
        } else {
            Err(SignatureError::UnknownDevice(
                user_id.to_string(),
                device_id.to_string(),
            ))
        }
    }

    /// Mark the device of the given user with the given device id as trusted.
    pub fn mark_device_as_trusted(
        &self,
        user_id: &str,
        device_id: &str,
    ) -> Result<(), CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

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
        let user_id = parse_user_id(user_id)?;

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

        let response = response_from_string(response_body);

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
            RequestType::SignatureUpload => {
                SignatureUploadResponse::try_from_http_response(response).map(Into::into)
            }
            RequestType::KeysBackup => {
                KeysBackupResponse::try_from_http_response(response).map(Into::into)
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
        unused_fallback_keys: Option<Vec<String>>,
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

        let unused_fallback_keys: Option<Vec<DeviceKeyAlgorithm>> =
            unused_fallback_keys.map(|u| u.into_iter().map(DeviceKeyAlgorithm::from).collect());

        let events = self.runtime.block_on(self.inner.receive_sync_changes(
            events,
            &device_changes,
            &key_counts,
            unused_fallback_keys.as_deref(),
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
        let users: Vec<Box<UserId>> = users
            .into_iter()
            .filter_map(|u| Box::<UserId>::try_from(u).ok())
            .collect();

        self.runtime.block_on(
            self.inner
                .update_tracked_users(users.iter().map(Deref::deref)),
        );
    }

    /// Check if the given user is considered to be tracked.
    ///
    /// A user can be marked for tracking using the
    /// [`OlmMachine::update_tracked_users()`] method.
    pub fn is_user_tracked(&self, user_id: &str) -> Result<bool, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;
        Ok(self.inner.tracked_users().contains(&user_id))
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
        let users: Vec<Box<UserId>> = users
            .into_iter()
            .filter_map(|u| Box::<UserId>::try_from(u).ok())
            .collect();

        Ok(self
            .runtime
            .block_on(
                self.inner
                    .get_missing_sessions(users.iter().map(Deref::deref)),
            )?
            .map(|r| r.into()))
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
        let users: Vec<Box<UserId>> = users
            .into_iter()
            .filter_map(|u| Box::<UserId>::try_from(u).ok())
            .collect();

        let room_id = Box::<RoomId>::try_from(room_id)?;
        let requests = self.runtime.block_on(self.inner.share_group_session(
            &room_id,
            users.iter().map(Deref::deref),
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
        let room_id = Box::<RoomId>::try_from(room_id)?;
        let content: Box<RawValue> = serde_json::from_str(content)?;

        let content = AnyMessageEventContent::from_parts(event_type, &content)?;
        let encrypted_content = self
            .runtime
            .block_on(self.inner.encrypt(&room_id, content))
            .expect("Encrypting an event produced an error");

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

        let event: SyncMessageEvent<RoomEncryptedEventContent> = serde_json::from_str(event)?;
        let room_id = Box::<RoomId>::try_from(room_id)?;

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
        let event: SyncMessageEvent<RoomEncryptedEventContent> = serde_json::from_str(event)?;
        let room_id = Box::<RoomId>::try_from(room_id)?;

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

    fn import_keys_helper(
        &self,
        keys: Vec<ExportedRoomKey>,
        from_backup: bool,
        progress_listener: Box<dyn ProgressListener>,
    ) -> Result<KeysImportResult, KeyImportError> {
        let listener = |progress: usize, total: usize| {
            progress_listener.on_progress(progress as i32, total as i32)
        };

        let result = self
            .runtime
            .block_on(self.inner.import_keys(keys, from_backup, listener))?;

        Ok(KeysImportResult {
            imported: result.imported_count as i64,
            total: result.total_count as i64,
            keys: result
                .keys
                .into_iter()
                .map(|(r, m)| {
                    (
                        r.to_string(),
                        m.into_iter()
                            .map(|(s, k)| (s, k.into_iter().collect()))
                            .collect(),
                    )
                })
                .collect(),
        })
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
        self.import_keys_helper(keys, false, progress_listener)
    }

    /// Import room keys from the given serialized unencrypted key export.
    ///
    /// This method is the same as [`OlmMachine::import_keys`] but the
    /// decryption step is skipped and should be performed by the caller. This
    /// should be used if the room keys are comming from the server-side backup,
    /// the method will mark all imported room keys as backed up.
    ///
    /// # Arguments
    ///
    /// * `keys` - The serialized version of the unencrypted key export.
    ///
    /// * `progress_listener` - A callback that can be used to introspect the
    /// progress of the key import.
    pub fn import_decrypted_keys(
        &self,
        keys: &str,
        progress_listener: Box<dyn ProgressListener>,
    ) -> Result<KeysImportResult, KeyImportError> {
        let keys: Vec<Value> = serde_json::from_str(keys)?;

        let keys = keys
            .into_iter()
            .map(serde_json::from_value)
            .filter_map(|k| k.ok())
            .collect();

        self.import_keys_helper(keys, true, progress_listener)
    }

    /// Discard the currently active room key for the given room if there is
    /// one.
    pub fn discard_room_key(&self, room_id: &str) -> Result<(), CryptoStoreError> {
        let room_id = Box::<RoomId>::try_from(room_id)?;

        self.runtime
            .block_on(self.inner.invalidate_group_session(&room_id))?;

        Ok(())
    }

    /// Get all the verification requests that we share with the given user.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to fetch the
    /// verification requests.
    pub fn get_verification_requests(&self, user_id: &str) -> Vec<VerificationRequest> {
        let user_id = if let Ok(user_id) = Box::<UserId>::try_from(user_id) {
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

    /// Get a verification requests that we share with the given user with the
    /// given flow id.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to fetch the
    /// verification requests.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    pub fn get_verification_request(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Option<VerificationRequest> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;

        self.inner
            .get_verification_request(&user_id, flow_id)
            .map(|v| v.into())
    }

    /// Accept a verification requests that we share with the given user with the
    /// given flow id.
    ///
    /// This will move the verification request into the ready state.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to accept the
    /// verification requests.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    ///
    /// * `methods` - A list of verification methods that we want to advertise
    /// as supported.
    pub fn accept_verification_request(
        &self,
        user_id: &str,
        flow_id: &str,
        methods: Vec<String>,
    ) -> Option<OutgoingVerificationRequest> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;
        let methods = methods.into_iter().map(VerificationMethod::from).collect();

        if let Some(verification) = self.inner.get_verification_request(&user_id, flow_id) {
            verification.accept_with_methods(methods).map(|r| r.into())
        } else {
            None
        }
    }

    /// Get an m.key.verification.request content for the given user.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user which we would like to request to
    /// verify.
    ///
    /// * `methods` - The list of verification methods we want to advertise to
    /// support.
    pub fn verification_request_content(
        &self,
        user_id: &str,
        methods: Vec<String>,
    ) -> Result<Option<String>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

        let identity = self.runtime.block_on(self.inner.get_identity(&user_id))?;

        let methods = methods.into_iter().map(VerificationMethod::from).collect();

        Ok(if let Some(identity) = identity.and_then(|i| i.other()) {
            let content = self
                .runtime
                .block_on(identity.verification_request_content(Some(methods)));
            Some(serde_json::to_string(&content)?)
        } else {
            None
        })
    }

    /// Request a verification flow to begin with the given user in the given
    /// room.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user which we would like to request to
    /// verify.
    ///
    /// * `room_id` - The ID of the room that represents a DM with the given
    /// user.
    ///
    /// * `event_id` - The event ID of the `m.key.verification.request` event
    /// that we sent out to request the verification to begin. The content for
    /// this request can be created using the [verification_request_content()]
    /// method.
    ///
    /// * `methods` - The list of verification methods we advertised as
    /// supported in the `m.key.verification.request` event.
    ///
    /// [verification_request_content()]: #method.verification_request_content
    pub fn request_verification(
        &self,
        user_id: &str,
        room_id: &str,
        event_id: &str,
        methods: Vec<String>,
    ) -> Result<Option<VerificationRequest>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;
        let event_id = Box::<EventId>::try_from(event_id)?;
        let room_id = Box::<RoomId>::try_from(room_id)?;

        let identity = self.runtime.block_on(self.inner.get_identity(&user_id))?;

        let methods = methods.into_iter().map(VerificationMethod::from).collect();

        Ok(if let Some(identity) = identity.and_then(|i| i.other()) {
            let request = self.runtime.block_on(identity.request_verification(
                &room_id,
                &event_id,
                Some(methods),
            ));

            Some(request.into())
        } else {
            None
        })
    }

    /// Request a verification flow to begin with the given user's device.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user which we would like to request to
    /// verify.
    ///
    /// * `device_id` - The ID of the device that we wish to verify.
    ///
    /// * `methods` - The list of verification methods we advertised as
    /// supported in the `m.key.verification.request` event.
    pub fn request_verification_with_device(
        &self,
        user_id: &str,
        device_id: &str,
        methods: Vec<String>,
    ) -> Result<Option<RequestVerificationResult>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

        let methods = methods.into_iter().map(VerificationMethod::from).collect();

        Ok(
            if let Some(device) = self
                .runtime
                .block_on(self.inner.get_device(&user_id, device_id.into()))?
            {
                let (verification, request) = self
                    .runtime
                    .block_on(device.request_verification_with_methods(methods));

                Some(RequestVerificationResult {
                    verification: verification.into(),
                    request: request.into(),
                })
            } else {
                None
            },
        )
    }

    /// Request a verification flow to begin with our other devices.
    ///
    /// # Arguments
    ///
    /// `methods` - The list of verification methods we want to advertise to
    /// support.
    pub fn request_self_verification(
        &self,
        methods: Vec<String>,
    ) -> Result<Option<RequestVerificationResult>, CryptoStoreError> {
        let identity = self
            .runtime
            .block_on(self.inner.get_identity(self.inner.user_id()))?;

        let methods = methods.into_iter().map(VerificationMethod::from).collect();

        Ok(if let Some(identity) = identity.and_then(|i| i.own()) {
            let (verification, request) = self
                .runtime
                .block_on(identity.request_verification_with_methods(methods))?;
            Some(RequestVerificationResult {
                verification: verification.into(),
                request: request.into(),
            })
        } else {
            None
        })
    }

    /// Get a verification flow object for the given user with the given flow id.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to fetch the
    /// verification.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    pub fn get_verification(&self, user_id: &str, flow_id: &str) -> Option<Verification> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;

        self.inner
            .get_verification(&user_id, flow_id)
            .map(|v| match v {
                RustVerification::SasV1(s) => Verification::SasV1 { sas: s.into() },
                RustVerification::QrV1(qr) => Verification::QrCodeV1 { qrcode: qr.into() },
            })
    }

    /// Cancel a verification for the given user with the given flow id using
    /// the given cancel code.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to cancel the
    /// verification.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    ///
    /// * `cancel_code` - The error code for why the verification was cancelled,
    /// manual cancellatio usually happens with `m.user` cancel code. The full
    /// list of cancel codes can be found in the [spec]
    ///
    /// [spec]: https://spec.matrix.org/unstable/client-server-api/#mkeyverificationcancel
    pub fn cancel_verification(
        &self,
        user_id: &str,
        flow_id: &str,
        cancel_code: &str,
    ) -> Option<OutgoingVerificationRequest> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;

        if let Some(request) = self.inner.get_verification_request(&user_id, flow_id) {
            request.cancel().map(|r| r.into())
        } else if let Some(verification) = self.inner.get_verification(&user_id, flow_id) {
            match verification {
                RustVerification::SasV1(v) => {
                    v.cancel_with_code(cancel_code.into()).map(|r| r.into())
                }
                RustVerification::QrV1(v) => {
                    v.cancel_with_code(cancel_code.into()).map(|r| r.into())
                }
            }
        } else {
            None
        }
    }

    /// Confirm a verification was successful.
    ///
    /// This method should be called either if a short auth string should be
    /// confirmed as matching, or if we want to confirm that the other side has
    /// scanned our QR code.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to confirm the
    /// verification.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    pub fn confirm_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Result<Option<ConfirmVerificationResult>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

        Ok(
            if let Some(verification) = self.inner.get_verification(&user_id, flow_id) {
                match verification {
                    RustVerification::SasV1(v) => {
                        let (request, signature_request) = self.runtime.block_on(v.confirm())?;

                        request.map(|r| ConfirmVerificationResult {
                            request: r.into(),
                            signature_request: signature_request.map(|s| s.into()),
                        })
                    }
                    RustVerification::QrV1(v) => {
                        v.confirm_scanning().map(|r| ConfirmVerificationResult {
                            request: r.into(),
                            signature_request: None,
                        })
                    }
                }
            } else {
                None
            },
        )
    }

    /// Transition from a verification request into QR code verification.
    ///
    /// This method should be called when one wants to display a QR code so the
    /// other side can scan it and move the QR code verification forward.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to start the
    /// QR code verification.
    ///
    /// * `flow_id` - The ID of the verification request that initated the
    /// verification flow.
    pub fn start_qr_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Result<Option<QrCode>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

        if let Some(verification) = self.inner.get_verification_request(&user_id, flow_id) {
            Ok(self
                .runtime
                .block_on(verification.generate_qr_code())?
                .map(|qr| qr.into()))
        } else {
            Ok(None)
        }
    }

    /// Generate data that should be encoded as a QR code.
    ///
    /// This method should be called right before a QR code should be displayed,
    /// the returned data is base64 encoded (without padding) and needs to be
    /// decoded on the other side before it can be put through a QR code
    /// generator.
    ///
    /// *Note*: You'll need to call [start_qr_verification()] before calling this
    /// method, otherwise `None` will be returned.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to start the
    /// QR code verification.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    ///
    /// [start_qr_verification()]: #method.start_qr_verification
    pub fn generate_qr_code(&self, user_id: &str, flow_id: &str) -> Option<String> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;
        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|v| v.qr_v1().and_then(|qr| qr.to_bytes().map(encode).ok()))
    }

    /// Pass data from a scanned QR code to an active verification request and
    /// transition into QR code verification.
    ///
    /// This requires an active `VerificationRequest` to succeed, returns `None`
    /// if no `VerificationRequest` is found or if the QR code data is invalid.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to start the
    /// QR code verification.
    ///
    /// * `flow_id` - The ID of the verification request that initated the
    /// verification flow.
    ///
    /// * `data` - The data that was extracted from the scanned QR code as an
    /// base64 encoded string, without padding.
    pub fn scan_qr_code(&self, user_id: &str, flow_id: &str, data: &str) -> Option<ScanResult> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;
        let data = decode_config(data, STANDARD_NO_PAD).ok()?;
        let data = QrVerificationData::from_bytes(data).ok()?;

        if let Some(verification) = self.inner.get_verification_request(&user_id, flow_id) {
            if let Some(qr) = self
                .runtime
                .block_on(verification.scan_qr_code(data))
                .ok()?
            {
                let request = qr.reciprocate()?;

                Some(ScanResult {
                    qr: qr.into(),
                    request: request.into(),
                })
            } else {
                None
            }
        } else {
            None
        }
    }

    /// Transition from a verification request into short auth string based
    /// verification.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to start the
    /// SAS verification.
    ///
    /// * `flow_id` - The ID of the verification request that initated the
    /// verification flow.
    pub fn start_sas_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Result<Option<StartSasResult>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

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

    /// Start short auth string verification with a device without going
    /// through a verification request first.
    ///
    /// **Note**: This has been largely deprecated and the
    /// [request_verification_with_device()] method should be used instead.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to start the
    /// SAS verification.
    ///
    /// * `device_id` - The ID of device we would like to verify.
    ///
    /// [request_verification_with_device()]: #method.request_verification_with_device
    pub fn start_sas_with_device(
        &self,
        user_id: &str,
        device_id: &str,
    ) -> Result<Option<StartSasResult>, CryptoStoreError> {
        let user_id = parse_user_id(user_id)?;

        Ok(
            if let Some(device) = self
                .runtime
                .block_on(self.inner.get_device(&user_id, device_id.into()))?
            {
                let (sas, request) = self.runtime.block_on(device.start_verification())?;

                Some(StartSasResult {
                    sas: sas.into(),
                    request: request.into(),
                })
            } else {
                None
            },
        )
    }

    /// Accept that we're going forward with the short auth string verification.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to accept the
    /// SAS verification.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    pub fn accept_sas_verification(
        &self,
        user_id: &str,
        flow_id: &str,
    ) -> Option<OutgoingVerificationRequest> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;

        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|s| s.sas_v1())
            .and_then(|s| s.accept().map(|r| r.into()))
    }

    /// Get a list of emoji indices of the emoji representation of the short
    /// auth string.
    ///
    /// *Note*: A SAS verification needs to be started and in the presentable
    /// state for this to return the list of emoji indices, otherwise returns
    /// `None`.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to get the
    /// short auth string.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    pub fn get_emoji_index(&self, user_id: &str, flow_id: &str) -> Option<Vec<i32>> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;

        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|s| {
                s.sas_v1().and_then(|s| {
                    s.emoji_index()
                        .map(|v| v.iter().map(|i| (*i).into()).collect())
                })
            })
    }

    /// Get the decimal representation of the short auth string.
    ///
    /// *Note*: A SAS verification needs to be started and in the presentable
    /// state for this to return the list of decimals, otherwise returns
    /// `None`.
    ///
    /// # Arguments
    ///
    /// * `user_id` - The ID of the user for which we would like to get the
    /// short auth string.
    ///
    /// * `flow_id` - The ID that uniquely identifies the verification flow.
    pub fn get_decimals(&self, user_id: &str, flow_id: &str) -> Option<Vec<i32>> {
        let user_id = Box::<UserId>::try_from(user_id).ok()?;

        self.inner
            .get_verification(&user_id, flow_id)
            .and_then(|s| {
                s.sas_v1().and_then(|s| {
                    s.decimals()
                        .map(|v| [v.0.into(), v.1.into(), v.2.into()].to_vec())
                })
            })
    }

    /// Create a new private cross signing identity and create a request to
    /// upload the public part of it to the server.
    pub fn bootstrap_cross_signing(&self) -> Result<BootstrapCrossSigningResult, CryptoStoreError> {
        Ok(self
            .runtime
            .block_on(self.inner.bootstrap_cross_signing(true))?
            .into())
    }

    /// Get the status of the private cross signing keys.
    ///
    /// This can be used to check which private cross signing keys we have
    /// stored locally.
    pub fn cross_signing_status(&self) -> CrossSigningStatus {
        self.runtime
            .block_on(self.inner.cross_signing_status())
            .into()
    }

    /// Export all our private cross signing keys.
    ///
    /// The export will contain the seed for the ed25519 keys as a base64
    /// encoded string.
    ///
    /// This method returns `None` if we don't have any private cross signing keys.
    pub fn export_cross_signing_keys(&self) -> Option<CrossSigningKeyExport> {
        self.runtime
            .block_on(self.inner.export_cross_signing_keys())
            .map(|e| e.into())
    }

    /// Import our private cross signing keys.
    ///
    /// The export needs to contain the seed for the ed25519 keys as a base64
    /// encoded string.
    pub fn import_cross_signing_keys(
        &self,
        export: CrossSigningKeyExport,
    ) -> Result<(), SecretImportError> {
        self.runtime
            .block_on(self.inner.import_cross_signing_keys(export.into()))?;

        Ok(())
    }

    /// Activate the given backup key to be used with the given backup version.
    ///
    /// **Warning**: The caller needs to make sure that the given `BackupKey` is
    /// trusted, otherwise we might be encrypting room keys that a malicious
    /// party could decrypt.
    ///
    /// The [`OlmMachine::verify_backup`] method can be used to so.
    pub fn enable_backup_v1(
        &self,
        key: MegolmV1BackupKey,
        version: String,
    ) -> Result<(), DecodeError> {
        let backup_key = RustBackupKey::from_base64(&key.public_key)?;
        backup_key.set_version(version);

        self.runtime
            .block_on(self.inner.backup_machine().enable_backup_v1(backup_key))?;

        Ok(())
    }

    /// Are we able to encrypt room keys.
    ///
    /// This returns true if we have an active `BackupKey` and backup version
    /// registered with the state machine.
    pub fn backup_enabled(&self) -> bool {
        self.runtime.block_on(self.inner.backup_machine().enabled())
    }

    /// Disable and reset our backup state.
    ///
    /// This will remove any pending backup request, remove the backup key and
    /// reset the backup state of each room key we have.
    pub fn disable_backup(&self) -> Result<(), CryptoStoreError> {
        Ok(self
            .runtime
            .block_on(self.inner.backup_machine().disable_backup())?)
    }

    /// Encrypt a batch of room keys and return a request that needs to be sent
    /// out to backup the room keys.
    pub fn backup_room_keys(&self) -> Result<Option<Request>, CryptoStoreError> {
        let request = self
            .runtime
            .block_on(self.inner.backup_machine().backup())?;

        let request = request.map(|r| r.into());

        Ok(request)
    }

    /// Get the number of backed up room keys and the total number of room keys.
    pub fn room_key_counts(&self) -> Result<RoomKeyCounts, CryptoStoreError> {
        Ok(self
            .runtime
            .block_on(self.inner.backup_machine().room_key_counts())?
            .into())
    }

    /// Store the recovery key in the cryptostore.
    ///
    /// This is useful if the client wants to support gossiping of the backup
    /// key.
    pub fn save_recovery_key(
        &self,
        key: Option<String>,
        version: Option<String>,
    ) -> Result<(), CryptoStoreError> {
        let key = key
            .map(|k| RecoveryKey::from_base64(&k))
            .transpose()
            .ok()
            .flatten();
        Ok(self
            .runtime
            .block_on(self.inner.backup_machine().save_recovery_key(key, version))?)
    }

    /// Get the backup keys we have saved in our crypto store.
    pub fn get_backup_keys(&self) -> Result<Option<BackupKeys>, CryptoStoreError> {
        Ok(self
            .runtime
            .block_on(self.inner.backup_machine().get_backup_keys())?
            .try_into()
            .ok())
    }

    /// Sign the given message using our device key and if available cross
    /// signing master key.
    pub fn sign(&self, message: &str) -> HashMap<String, HashMap<String, String>> {
        self.runtime
            .block_on(self.inner.sign(message))
            .into_iter()
            .map(|(k, v)| {
                (
                    k.to_string(),
                    v.into_iter().map(|(k, v)| (k.to_string(), v)).collect(),
                )
            })
            .collect()
    }

    /// Check if the given backup has been verified by us or by another of our
    /// devices that we trust.
    pub fn verify_backup(&self, auth_data: &str) -> Result<bool, CryptoStoreError> {
        let auth_data = serde_json::from_str(auth_data)?;
        Ok(self
            .runtime
            .block_on(self.inner.backup_machine().verify_backup(auth_data))?)
    }
}
