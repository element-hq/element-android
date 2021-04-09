use std::collections::HashMap;

use matrix_sdk_crypto::Device as InnerDevice;

pub struct Device {
    pub user_id: String,
    pub device_id: String,
    pub keys: HashMap<String, String>,
    pub algorithms: Vec<String>,
    pub display_name: Option<String>,
    pub is_blocked: bool,
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
            algorithms: d.algorithms().iter().map(|a| a.to_string()).collect(),
            display_name: d.display_name().clone(),
            is_blocked: d.is_blacklisted(),
        }
    }
}
