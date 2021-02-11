use std::{
    collections::{BTreeMap, HashMap},
    convert::TryFrom,
    time::Duration,
};

use futures::executor::block_on;
use http::Response;
use serde_json::json;
use tokio::{runtime::Runtime, time::sleep};

use matrix_sdk_common::{
    api::r0::{
        keys::{
            claim_keys::Response as KeysClaimResponse, get_keys::Response as KeysQueryResponse,
            upload_keys::Response as KeysUploadResponse,
        },
        sync::sync_events::{DeviceLists as RumaDeviceLists, ToDevice},
    },
    identifiers::{DeviceKeyAlgorithm, Error as RumaIdentifierError, UserId},
    uuid::Uuid,
    UInt,
};
use matrix_sdk_crypto::{
    store::CryptoStoreError as InnerStoreError, IncomingResponse, OlmError,
    OlmMachine as InnerMachine, OutgoingRequest, ToDeviceRequest,
};

pub struct OlmMachine {
    inner: InnerMachine,
    runtime: Runtime,
}

pub struct DeviceLists {
    pub changed: Vec<String>,
    pub left: Vec<String>,
}

impl Into<RumaDeviceLists> for DeviceLists {
    fn into(self) -> RumaDeviceLists {
        RumaDeviceLists {
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
        }
    }
}

enum OwnedResponse {
    KeysClaim(KeysClaimResponse),
    KeysUpload(KeysUploadResponse),
    KeysQuery(KeysQueryResponse),
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

impl<'a> Into<IncomingResponse<'a>> for &'a OwnedResponse {
    fn into(self) -> IncomingResponse<'a> {
        match self {
            OwnedResponse::KeysClaim(r) => IncomingResponse::KeysClaim(r),
            OwnedResponse::KeysQuery(r) => IncomingResponse::KeysQuery(r),
            OwnedResponse::KeysUpload(r) => IncomingResponse::KeysUpload(r),
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum MachineCreationError {
    #[error(transparent)]
    Identifier(#[from] RumaIdentifierError),
    #[error(transparent)]
    CryptoStore(#[from] InnerStoreError),
}

#[derive(Debug, thiserror::Error)]
pub enum CryptoStoreError {
    #[error(transparent)]
    CryptoStore(#[from] InnerStoreError),
    #[error(transparent)]
    OlmError(#[from] OlmError),
}

pub enum RequestType {
    KeysQuery,
    KeysUpload,
    ToDevice,
}

pub struct Device {
    pub user_id: String,
    pub device_id: String,
    pub keys: HashMap<String, String>,
}

pub struct Sas {
    pub other_user_id: String,
    pub other_device_id: String,
    pub flow_id: String,
    pub request: Request,
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
        body: String,
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
                let body = json!({
                    "device_keys": k.device_keys,
                });
                Request::KeysQuery {
                    request_id: r.request_id().to_string(),
                    body: serde_json::to_string(&body).expect("Can't serialize keys query request"),
                }
            }
            ToDeviceRequest(t) => Request::from(t),
            SignatureUpload(_) => todo!(),
            RoomMessage(_) => todo!(),
        }
    }
}

impl From<ToDeviceRequest> for Request {
    fn from(r: ToDeviceRequest) -> Self {
        Request::ToDevice {
            request_id: r.txn_id_string(),
            event_type: r.event_type.to_string(),
            body: serde_json::to_string(&r.messages).unwrap(),
        }
    }
}

impl From<&ToDeviceRequest> for Request {
    fn from(r: &ToDeviceRequest) -> Self {
        Request::ToDevice {
            request_id: r.txn_id_string(),
            event_type: r.event_type.to_string(),
            body: serde_json::to_string(&r.messages).unwrap(),
        }
    }
}

fn response_from_string(body: &str) -> Response<Vec<u8>> {
    Response::builder()
        .status(200)
        .body(body.as_bytes().to_vec())
        .expect("Can't create HTTP response")
}

impl OlmMachine {
    pub fn new(user_id: &str, device_id: &str, path: &str) -> Result<Self, MachineCreationError> {
        let user_id = UserId::try_from(user_id)?;
        let device_id = device_id.into();

        Ok(OlmMachine {
            inner: block_on(InnerMachine::new_with_default_store(
                &user_id,
                device_id,
                path,
                Some("DEFAULT_PASSPHRASE"),
            ))?,
            runtime: Runtime::new().unwrap(),
        })
    }

    pub fn user_id(&self) -> String {
        self.inner.user_id().to_string()
    }

    pub fn device_id(&self) -> String {
        self.inner.device_id().to_string()
    }

    pub fn get_device(&self, user_id: &str, device_id: &str) -> Option<Device> {
        let user_id = UserId::try_from(user_id).unwrap();

        block_on(self.inner.get_device(&user_id, device_id.into()))
            .unwrap()
            .map(|d| Device {
                user_id: d.user_id().to_string(),
                device_id: d.device_id().to_string(),
                keys: d
                    .keys()
                    .iter()
                    .map(|(k, v)| (k.to_string(), v.to_string()))
                    .collect(),
            })
    }

    pub fn get_user_devices(&self, user_id: &str) -> Vec<Device> {
        let user_id = UserId::try_from(user_id).unwrap();
        block_on(self.inner.get_user_devices(&user_id))
            .unwrap()
            .devices()
            .map(|d| Device {
                user_id: d.user_id().to_string(),
                device_id: d.device_id().to_string(),
                keys: d
                    .keys()
                    .iter()
                    .map(|(k, v)| (k.to_string(), v.to_string()))
                    .collect(),
            })
            .collect()
    }

    pub fn identity_keys(&self) -> HashMap<String, String> {
        self.inner
            .identity_keys()
            .iter()
            .map(|(k, v)| (k.to_owned(), v.to_owned()))
            .collect()
    }

    pub fn slow_user_id(&self) -> String {
        let machine = self.inner.clone();

        self.runtime.block_on(async {
            sleep(Duration::from_secs(10)).await;
            machine.user_id().to_string()
        })
    }

    pub fn mark_request_as_sent(
        &self,
        request_id: &str,
        request_type: RequestType,
        response_body: &str,
    ) -> Result<(), CryptoStoreError> {
        let id = Uuid::parse_str(request_id).expect("Can't parse request id");

        let response = response_from_string(&response_body);

        let response: OwnedResponse = match request_type {
            RequestType::KeysUpload => KeysUploadResponse::try_from(response).map(Into::into),
            RequestType::KeysQuery => KeysQueryResponse::try_from(response).map(Into::into),
            RequestType::ToDevice => KeysClaimResponse::try_from(response).map(Into::into),
        }
        .expect("Can't convert json string to response");

        block_on(self.inner.mark_request_as_sent(&id, &response))?;

        Ok(())
    }

    pub fn start_verification(&self, device: &Device) -> Result<Sas, CryptoStoreError> {
        let user_id = UserId::try_from(device.user_id.clone()).unwrap();
        let device_id = device.device_id.as_str().into();
        let device = block_on(self.inner.get_device(&user_id, device_id))?.unwrap();

        let (sas, request) = block_on(device.start_verification())?;

        Ok(Sas {
            other_user_id: sas.other_user_id().to_string(),
            other_device_id: sas.other_device_id().to_string(),
            flow_id: sas.flow_id().as_str().to_owned(),
            request: request.into(),
        })
    }

    pub fn outgoing_requests(&self) -> Vec<Request> {
        block_on(self.inner.outgoing_requests())
            .into_iter()
            .map(|r| r.into())
            .collect()
    }

    pub fn receive_sync_changes(
        &self,
        events: &str,
        device_changes: DeviceLists,
        key_counts: HashMap<String, u32>,
    ) {
        let events: ToDevice = serde_json::from_str(events).unwrap();
        let device_changes: RumaDeviceLists = device_changes.into();
        let key_counts: BTreeMap<DeviceKeyAlgorithm, UInt> = key_counts
            .into_iter()
            .map(|(k, v)| (DeviceKeyAlgorithm::from(k), v.into()))
            .collect();

        block_on(
            self.inner
                .receive_sync_changes(&events, &device_changes, &key_counts),
        )
        .unwrap();
    }
}

include!(concat!(env!("OUT_DIR"), "/olm.uniffi.rs"));
