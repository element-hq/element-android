use std::collections::HashMap;

use matrix_sdk_crypto::Device as InnerDevice;

pub struct Device {
    pub user_id: String,
    pub device_id: String,
    pub keys: HashMap<String, String>,
}

impl From<InnerDevice> for Device {
    fn from(d: InnerDevice) -> Self {
        Device {
            user_id: d.user_id().to_string(),
            device_id: d.device_id().to_string(),
            keys: d
                .keys()
                .iter()
                .map(|(k, v)| (k.to_string(), v.to_string()))
                .collect(),
        }
    }
}
