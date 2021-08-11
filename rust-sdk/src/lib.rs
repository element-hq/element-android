#![deny(
    dead_code,
    missing_docs,
    trivial_casts,
    trivial_numeric_casts,
    unused_extern_crates,
    unused_import_braces,
    unused_qualifications
)]

//! TODO

mod device;
mod error;
mod logger;
mod machine;
mod responses;
mod users;
mod verification;

pub use device::Device;
pub use error::{CryptoStoreError, DecryptionError, KeyImportError, SignatureError, SecretImportError};
pub use logger::{set_logger, Logger};
pub use machine::{KeyRequestPair, OlmMachine};
pub use responses::{
    DeviceLists, KeysImportResult, OutgoingVerificationRequest, Request, RequestType, SignatureUploadRequest,
    BootstrapCrossSigningResult, UploadSigningKeysRequest,
};
pub use users::UserIdentity;
pub use verification::{
    CancelInfo, QrCode, RequestVerificationResult, Sas, ScanResult, StartSasResult, Verification,
    VerificationRequest, ConfirmVerificationResult,
};

/// Callback that will be passed over the FFI to report progress
pub trait ProgressListener {
    /// The callback that should be called on the Rust side
    ///
    /// # Arguments
    ///
    /// * `progress` - The current number of items that have been handled
    ///
    /// * `total` - The total number of items that will be handled
    fn on_progress(&self, progress: i32, total: i32);
}

/// An event that was successfully decrypted.
pub struct DecryptedEvent {
    /// The decrypted version of the event.
    pub clear_event: String,
    /// The claimed curve25519 key of the sender.
    pub sender_curve25519_key: String,
    /// The claimed ed25519 key of the sender.
    pub claimed_ed25519_key: Option<String>,
    /// The curve25519 chain of the senders that forwarded the Megolm decryption
    /// key to us. Is empty if the key came directly from the sender of the
    /// event.
    pub forwarding_curve25519_chain: Vec<String>,
}

/// Struct representing the state of our private cross signing keys, it shows
/// which private cross signing keys we have locally stored.
#[derive(Debug, Clone)]
pub struct CrossSigningStatus {
    /// Do we have the master key.
    pub has_master: bool,
    /// Do we have the self signing key, this one is necessary to sign our own
    /// devices.
    pub has_self_signing: bool,
    /// Do we have the user signing key, this one is necessary to sign other
    /// users.
    pub has_user_signing: bool,
}

/// A struct containing private cross signing keys that can be backed up or
/// uploaded to the secret store.
pub struct CrossSigningKeyExport {
    /// The seed of the master key encoded as unpadded base64.
    pub master_key: Option<String>,
    /// The seed of the self signing key encoded as unpadded base64.
    pub self_signing_key: Option<String>,
    /// The seed of the user signing key encoded as unpadded base64.
    pub user_signing_key: Option<String>,
}

impl From<matrix_sdk_crypto::CrossSigningKeyExport> for CrossSigningKeyExport {
    fn from(e: matrix_sdk_crypto::CrossSigningKeyExport) -> Self {
        Self {
            master_key: e.master_key.clone(),
            self_signing_key: e.self_signing_key.clone(),
            user_signing_key: e.user_signing_key.clone(),
        }
    }
}

impl Into<matrix_sdk_crypto::CrossSigningKeyExport> for CrossSigningKeyExport {
    fn into(self) -> matrix_sdk_crypto::CrossSigningKeyExport {
        matrix_sdk_crypto::CrossSigningKeyExport {
            master_key: self.master_key,
            self_signing_key: self.self_signing_key,
            user_signing_key: self.user_signing_key,
        }
    }
}

impl From<matrix_sdk_crypto::CrossSigningStatus> for CrossSigningStatus {
    fn from(s: matrix_sdk_crypto::CrossSigningStatus) -> Self {
        Self {
            has_master: s.has_master,
            has_self_signing: s.has_self_signing,
            has_user_signing: s.has_user_signing,
        }
    }
}

include!(concat!(env!("OUT_DIR"), "/olm.uniffi.rs"));
