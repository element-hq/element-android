use std::{collections::HashMap, convert::TryFrom, time::Duration};

use futures::{
    executor::block_on,
    future::{abortable, AbortHandle, Aborted},
    Future,
};
use http::Response;
use tokio::{runtime::Runtime, time::sleep};

use matrix_sdk_common::{
    api::r0::sync::sync_events::Response as SyncResponse,
    api::r0::to_device::send_event_to_device::METADATA,
    identifiers::{Error as RumaIdentifierError, UserId},
};
use matrix_sdk_crypto::{
    store::CryptoStoreError as InnerStoreError, OlmMachine as InnerMachine, ToDeviceRequest,
};

pub struct OlmMachine {
    inner: InnerMachine,
    runtime: Runtime,
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

pub struct Request {
    pub request_id: String,
    pub request_type: RequestType,
    pub request_body: String,
}

impl From<ToDeviceRequest> for Request {
    fn from(r: ToDeviceRequest) -> Self {
        Request {
            request_id: r.txn_id_string(),
            request_type: RequestType::ToDevice,
            request_body: serde_json::to_string(&r.messages).unwrap(),
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

    pub fn receive_sync_response(&self, response: &str) {
        let response = response_from_string(response);
        let mut response = SyncResponse::try_from(response).expect("Can't parse response");

        block_on(self.inner.receive_sync_response(&mut response)).unwrap();
    }
}

include!(concat!(env!("OUT_DIR"), "/olm.uniffi.rs"));
