mod error;
mod logger;
mod machine;

pub use error::{CryptoStoreError, DecryptionError, MachineCreationError};
pub use logger::{set_logger, Logger};
pub use machine::{
    DecryptedEvent, Device, DeviceLists, KeysImportResult, OlmMachine, Request, RequestType, Sas,
};

pub trait ProgressListener {
    fn on_progress(&self, progress: i32, total: i32);
}

include!(concat!(env!("OUT_DIR"), "/olm.uniffi.rs"));
