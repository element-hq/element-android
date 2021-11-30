#![allow(missing_docs)]

use std::{collections::HashMap, convert::TryFrom};

use http::Response;
use matrix_sdk_common::uuid::Uuid;
use serde_json::json;

use ruma::{
    api::client::r0::{
        backup::add_backup_keys::Response as KeysBackupResponse,
        keys::{
            claim_keys::{Request as KeysClaimRequest, Response as KeysClaimResponse},
            get_keys::Response as KeysQueryResponse,
            upload_keys::Response as KeysUploadResponse,
            upload_signatures::{
                Request as RustSignatureUploadRequest, Response as SignatureUploadResponse,
            },
        },
        sync::sync_events::DeviceLists as RumaDeviceLists,
        to_device::send_event_to_device::Response as ToDeviceResponse,
    },
    assign,
    events::EventContent,
    identifiers::UserId,
};

use matrix_sdk_crypto::{
    IncomingResponse, OutgoingRequest, OutgoingVerificationRequest as SdkVerificationRequest,
    RoomMessageRequest, ToDeviceRequest, UploadSigningKeysRequest as RustUploadSigningKeysRequest,
};

pub struct SignatureUploadRequest {
    pub body: String,
}

impl From<RustSignatureUploadRequest> for SignatureUploadRequest {
    fn from(r: RustSignatureUploadRequest) -> Self {
        Self {
            body: serde_json::to_string(&r.signed_keys)
                .expect("Can't serialize signature upload request"),
        }
    }
}

pub struct UploadSigningKeysRequest {
    pub master_key: String,
    pub self_signing_key: String,
    pub user_signing_key: String,
}

impl From<RustUploadSigningKeysRequest> for UploadSigningKeysRequest {
    fn from(r: RustUploadSigningKeysRequest) -> Self {
        Self {
            master_key: serde_json::to_string(
                &r.master_key.expect("Request didn't contain a master key"),
            )
            .expect("Can't serialize cross signing master key"),
            self_signing_key: serde_json::to_string(
                &r.self_signing_key
                    .expect("Request didn't contain a self-signing key"),
            )
            .expect("Can't serialize cross signing self-signing key"),
            user_signing_key: serde_json::to_string(
                &r.user_signing_key
                    .expect("Request didn't contain a user-signing key"),
            )
            .expect("Can't serialize cross signing user-signing key"),
        }
    }
}

pub struct BootstrapCrossSigningResult {
    pub upload_signing_keys_request: UploadSigningKeysRequest,
    pub signature_request: SignatureUploadRequest,
}

impl From<(RustUploadSigningKeysRequest, RustSignatureUploadRequest)>
    for BootstrapCrossSigningResult
{
    fn from(requests: (RustUploadSigningKeysRequest, RustSignatureUploadRequest)) -> Self {
        Self {
            upload_signing_keys_request: requests.0.into(),
            signature_request: requests.1.into(),
        }
    }
}

pub enum OutgoingVerificationRequest {
    ToDevice {
        request_id: String,
        event_type: String,
        body: String,
    },
    InRoom {
        request_id: String,
        room_id: String,
        event_type: String,
        content: String,
    },
}

impl From<SdkVerificationRequest> for OutgoingVerificationRequest {
    fn from(r: SdkVerificationRequest) -> Self {
        match r {
            SdkVerificationRequest::ToDevice(r) => r.into(),
            SdkVerificationRequest::InRoom(r) => Self::InRoom {
                request_id: r.txn_id.to_string(),
                room_id: r.room_id.to_string(),
                content: serde_json::to_string(&r.content)
                    .expect("Can't serialize message content"),
                event_type: r.content.event_type().to_string(),
            },
        }
    }
}

impl From<ToDeviceRequest> for OutgoingVerificationRequest {
    fn from(r: ToDeviceRequest) -> Self {
        Self::ToDevice {
            request_id: r.txn_id_string(),
            event_type: r.event_type.to_string(),
            body: serde_json::to_string(&r.messages).expect("Can't serialize to-device body"),
        }
    }
}

#[derive(Debug)]
pub enum Request {
    ToDevice {
        request_id: String,
        event_type: String,
        body: String,
    },
    KeysUpload {
        request_id: String,
        body: String,
    },
    KeysQuery {
        request_id: String,
        users: Vec<String>,
    },
    KeysClaim {
        request_id: String,
        one_time_keys: HashMap<String, HashMap<String, String>>,
    },
    RoomMessage {
        request_id: String,
        room_id: String,
        event_type: String,
        content: String,
    },
    SignatureUpload {
        request_id: String,
        body: String,
    },
    KeysBackup {
        request_id: String,
        version: String,
        rooms: String,
    },
}

impl From<OutgoingRequest> for Request {
    fn from(r: OutgoingRequest) -> Self {
        use matrix_sdk_crypto::OutgoingRequests::*;

        match r.request() {
            KeysUpload(u) => {
                let body = json!({
                    "device_keys": u.device_keys,
                    "one_time_keys": u.one_time_keys,
                });

                Request::KeysUpload {
                    request_id: r.request_id().to_string(),
                    body: serde_json::to_string(&body)
                        .expect("Can't serialize keys upload request"),
                }
            }
            KeysQuery(k) => {
                let users: Vec<String> = k.device_keys.keys().map(|u| u.to_string()).collect();
                Request::KeysQuery {
                    request_id: r.request_id().to_string(),
                    users,
                }
            }
            ToDeviceRequest(t) => Request::from(t),
            SignatureUpload(t) => Request::SignatureUpload {
                request_id: r.request_id().to_string(),
                body: serde_json::to_string(&t.signed_keys)
                    .expect("Can't serialize signature upload request"),
            },
            RoomMessage(r) => Request::from(r),
            KeysClaim(c) => (*r.request_id(), c.clone()).into(),
            KeysBackup(b) => Request::KeysBackup {
                request_id: r.request_id().to_string(),
                version: b.version.to_owned(),
                rooms: serde_json::to_string(&b.rooms)
                    .expect("Can't serialize keys backup request"),
            },
        }
    }
}

impl From<ToDeviceRequest> for Request {
    fn from(r: ToDeviceRequest) -> Self {
        Request::ToDevice {
            request_id: r.txn_id_string(),
            event_type: r.event_type.to_string(),
            body: serde_json::to_string(&r.messages).expect("Can't serialize to-device body"),
        }
    }
}

impl From<(Uuid, KeysClaimRequest)> for Request {
    fn from(request_tuple: (Uuid, KeysClaimRequest)) -> Self {
        let (request_id, request) = request_tuple;

        Request::KeysClaim {
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
        }
    }
}

impl From<&ToDeviceRequest> for Request {
    fn from(r: &ToDeviceRequest) -> Self {
        Request::ToDevice {
            request_id: r.txn_id_string(),
            event_type: r.event_type.to_string(),
            body: serde_json::to_string(&r.messages).expect("Can't serialize to-device body"),
        }
    }
}

impl From<&RoomMessageRequest> for Request {
    fn from(r: &RoomMessageRequest) -> Self {
        Self::RoomMessage {
            request_id: r.txn_id.to_string(),
            room_id: r.room_id.to_string(),
            event_type: r.content.event_type().to_string(),
            content: serde_json::to_string(&r.content).expect("Can't serialize message content"),
        }
    }
}

pub(crate) fn response_from_string(body: &str) -> Response<Vec<u8>> {
    Response::builder()
        .status(200)
        .body(body.as_bytes().to_vec())
        .expect("Can't create HTTP response")
}

pub enum RequestType {
    KeysQuery,
    KeysClaim,
    KeysUpload,
    ToDevice,
    SignatureUpload,
    KeysBackup,
}

pub struct DeviceLists {
    pub changed: Vec<String>,
    pub left: Vec<String>,
}

impl From<DeviceLists> for RumaDeviceLists {
    fn from(d: DeviceLists) -> Self {
        assign!(RumaDeviceLists::new(), {
            changed: d
                .changed
                .into_iter()
                .filter_map(|u| Box::<UserId>::try_from(u).ok())
                .collect(),
            left: d
                .left
                .into_iter()
                .filter_map(|u| Box::<UserId>::try_from(u).ok())
                .collect(),
        })
    }
}

pub struct KeysImportResult {
    /// The number of room keys that were imported.
    pub imported: i64,
    /// The total number of room keys that were found in the export.
    pub total: i64,
    /// The map of keys that were imported.
    ///
    /// It's a map from room id to a map of the sender key to a list of session
    /// ids.
    pub keys: HashMap<String, HashMap<String, Vec<String>>>,
}

pub(crate) enum OwnedResponse {
    KeysClaim(KeysClaimResponse),
    KeysUpload(KeysUploadResponse),
    KeysQuery(KeysQueryResponse),
    ToDevice(ToDeviceResponse),
    SignatureUpload(SignatureUploadResponse),
    KeysBackup(KeysBackupResponse),
}

impl From<KeysClaimResponse> for OwnedResponse {
    fn from(response: KeysClaimResponse) -> Self {
        OwnedResponse::KeysClaim(response)
    }
}

impl From<KeysQueryResponse> for OwnedResponse {
    fn from(response: KeysQueryResponse) -> Self {
        OwnedResponse::KeysQuery(response)
    }
}

impl From<KeysUploadResponse> for OwnedResponse {
    fn from(response: KeysUploadResponse) -> Self {
        OwnedResponse::KeysUpload(response)
    }
}

impl From<ToDeviceResponse> for OwnedResponse {
    fn from(response: ToDeviceResponse) -> Self {
        OwnedResponse::ToDevice(response)
    }
}

impl From<SignatureUploadResponse> for OwnedResponse {
    fn from(response: SignatureUploadResponse) -> Self {
        Self::SignatureUpload(response)
    }
}

impl From<KeysBackupResponse> for OwnedResponse {
    fn from(r: KeysBackupResponse) -> Self {
        Self::KeysBackup(r)
    }
}

impl<'a> From<&'a OwnedResponse> for IncomingResponse<'a> {
    fn from(r: &'a OwnedResponse) -> Self {
        match r {
            OwnedResponse::KeysClaim(r) => IncomingResponse::KeysClaim(r),
            OwnedResponse::KeysQuery(r) => IncomingResponse::KeysQuery(r),
            OwnedResponse::KeysUpload(r) => IncomingResponse::KeysUpload(r),
            OwnedResponse::ToDevice(r) => IncomingResponse::ToDevice(r),
            OwnedResponse::SignatureUpload(r) => IncomingResponse::SignatureUpload(r),
            OwnedResponse::KeysBackup(r) => IncomingResponse::KeysBackup(r),
        }
    }
}
