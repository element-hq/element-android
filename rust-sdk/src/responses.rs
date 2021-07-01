use std::{collections::HashMap, convert::TryFrom};

use http::Response;
use serde_json::json;

use ruma::{
    api::client::r0::{
        keys::{
            claim_keys::Response as KeysClaimResponse, get_keys::Response as KeysQueryResponse,
            upload_keys::Response as KeysUploadResponse,
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
    RoomMessageRequest, ToDeviceRequest,
};

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
            SignatureUpload(_) => todo!("Uploading signatures isn't yet supported"),
            RoomMessage(r) => Request::from(r),
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
}

pub struct DeviceLists {
    pub changed: Vec<String>,
    pub left: Vec<String>,
}

impl Into<RumaDeviceLists> for DeviceLists {
    fn into(self) -> RumaDeviceLists {
        assign!(RumaDeviceLists::new(), {
            changed: self
                .changed
                .into_iter()
                .filter_map(|u| UserId::try_from(u).ok())
                .collect(),
            left: self
                .left
                .into_iter()
                .filter_map(|u| UserId::try_from(u).ok())
                .collect(),
        })
    }
}

pub struct KeysImportResult {
    pub total: i32,
    pub imported: i32,
}

pub(crate) enum OwnedResponse {
    KeysClaim(KeysClaimResponse),
    KeysUpload(KeysUploadResponse),
    KeysQuery(KeysQueryResponse),
    ToDevice(ToDeviceResponse),
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

impl<'a> Into<IncomingResponse<'a>> for &'a OwnedResponse {
    fn into(self) -> IncomingResponse<'a> {
        match self {
            OwnedResponse::KeysClaim(r) => IncomingResponse::KeysClaim(r),
            OwnedResponse::KeysQuery(r) => IncomingResponse::KeysQuery(r),
            OwnedResponse::KeysUpload(r) => IncomingResponse::KeysUpload(r),
            OwnedResponse::ToDevice(r) => IncomingResponse::ToDevice(r),
        }
    }
}
