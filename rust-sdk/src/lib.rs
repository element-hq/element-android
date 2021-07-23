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
mod verification;

pub use device::Device;
pub use error::{CryptoStoreError, DecryptionError, KeyImportError, MachineCreationError};
pub use logger::{set_logger, Logger};
pub use machine::{KeyRequestPair, OlmMachine};
pub use responses::{
    DeviceLists, KeysImportResult, OutgoingVerificationRequest, Request, RequestType,
};
pub use verification::{
    CancelInfo, QrCode, RequestVerificationResult, Sas, ScanResult, StartSasResult, Verification,
    VerificationRequest,
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

include!(concat!(env!("OUT_DIR"), "/olm.uniffi.rs"));
