use std::collections::HashMap;

use matrix_sdk_crypto::Device as InnerDevice;

/// An E2EE capable Matrix device.
pub struct Device {
    /// The device owner.
    pub user_id: String,
    /// The unique ID of the device.
    pub device_id: String,
    /// The published public identity keys of the devices
    ///
    /// A map from the key type (e.g. curve25519) to the base64 encoded key.
    pub keys: HashMap<String, String>,
    /// The supported algorithms of the device.
    pub algorithms: Vec<String>,
    /// The human readable name of the device.
    pub display_name: Option<String>,
    /// A flag indicating if the device has been blocked, blocked devices don't
    /// receive any room keys from us.
    pub is_blocked: bool,
    /// Is the device locally trusted
    pub locally_trusted: bool,
    /// Is our cross signing identity trusted and does the identity trust the
    /// device.
    pub cross_signing_trusted: bool,
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
            display_name: d.display_name().map(|d| d.to_owned()),
            is_blocked: d.is_blacklisted(),
            locally_trusted: d.is_locally_trusted(),
            cross_signing_trusted: d.is_cross_signing_trusted(),
        }
    }
}
