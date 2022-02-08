#![deny(
    dead_code,
    trivial_casts,
    trivial_numeric_casts,
    unused_extern_crates,
    unused_import_braces
)]

//! TODO

mod backup_recovery_key;
mod device;
mod error;
mod logger;
mod machine;
mod responses;
mod users;
mod verification;

use std::convert::TryFrom;

pub use backup_recovery_key::{
    BackupRecoveryKey, DecodeError, MegolmV1BackupKey, PassphraseInfo, PkDecryptionError,
};
pub use device::Device;
pub use error::{
    CryptoStoreError, DecryptionError, KeyImportError, SecretImportError, SignatureError,
};
pub use logger::{set_logger, Logger};
pub use machine::{KeyRequestPair, OlmMachine};
pub use responses::{
    BootstrapCrossSigningResult, DeviceLists, KeysImportResult, OutgoingVerificationRequest,
    Request, RequestType, SignatureUploadRequest, UploadSigningKeysRequest,
};
pub use users::UserIdentity;
pub use verification::{
    CancelInfo, ConfirmVerificationResult, QrCode, RequestVerificationResult, Sas, ScanResult,
    StartSasResult, Verification, VerificationRequest,
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

/// Struct holding the number of room keys we have.
pub struct RoomKeyCounts {
    /// The total number of room keys.
    pub total: i64,
    /// The number of backed up room keys.
    pub backed_up: i64,
}

/// Backup keys and information we load from the store.
pub struct BackupKeys {
    /// The recovery key as a base64 encoded string.
    pub recovery_key: String,
    /// The version that is used with the recovery key.
    pub backup_version: String,
}

impl std::convert::TryFrom<matrix_sdk_crypto::store::BackupKeys> for BackupKeys {
    type Error = ();

    fn try_from(keys: matrix_sdk_crypto::store::BackupKeys) -> Result<Self, Self::Error> {
        Ok(Self {
            recovery_key: keys.recovery_key.ok_or(())?.to_base64(),
            backup_version: keys.backup_version.ok_or(())?,
        })
    }
}

impl From<matrix_sdk_crypto::store::RoomKeyCounts> for RoomKeyCounts {
    fn from(count: matrix_sdk_crypto::store::RoomKeyCounts) -> Self {
        Self {
            total: count.total as i64,
            backed_up: count.backed_up as i64,
        }
    }
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

impl From<CrossSigningKeyExport> for matrix_sdk_crypto::CrossSigningKeyExport {
    fn from(e: CrossSigningKeyExport) -> Self {
        matrix_sdk_crypto::CrossSigningKeyExport {
            master_key: e.master_key,
            self_signing_key: e.self_signing_key,
            user_signing_key: e.user_signing_key,
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

fn parse_user_id(user_id: &str) -> Result<Box<ruma::UserId>, CryptoStoreError> {
    Box::<ruma::UserId>::try_from(user_id)
        .map_err(|e| CryptoStoreError::InvalidUserId(user_id.to_owned(), e))
}

include!(concat!(env!("OUT_DIR"), "/olm.uniffi.rs"));
