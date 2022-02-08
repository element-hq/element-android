#![allow(missing_docs)]

use matrix_sdk_crypto::{
    store::CryptoStoreError as InnerStoreError, KeyExportError, MegolmError, OlmError,
    SecretImportError as RustSecretImportError, SignatureError as InnerSignatureError,
};
use ruma::identifiers::Error as RumaIdentifierError;

#[derive(Debug, thiserror::Error)]
pub enum KeyImportError {
    #[error(transparent)]
    Export(#[from] KeyExportError),
    #[error(transparent)]
    CryptoStore(#[from] InnerStoreError),
    #[error(transparent)]
    Json(#[from] serde_json::Error),
}

#[derive(Debug, thiserror::Error)]
pub enum SecretImportError {
    #[error(transparent)]
    CryptoStore(#[from] InnerStoreError),
    #[error(transparent)]
    Import(#[from] RustSecretImportError),
}

#[derive(Debug, thiserror::Error)]
pub enum SignatureError {
    #[error(transparent)]
    Signature(#[from] InnerSignatureError),
    #[error(transparent)]
    Identifier(#[from] RumaIdentifierError),
    #[error(transparent)]
    CryptoStore(#[from] InnerStoreError),
    #[error("Unknown device {0} {1}")]
    UnknownDevice(String, String),
    #[error("Unknown user identity {0}")]
    UnknownUserIdentity(String),
}

#[derive(Debug, thiserror::Error)]
pub enum CryptoStoreError {
    #[error(transparent)]
    CryptoStore(#[from] InnerStoreError),
    #[error(transparent)]
    OlmError(#[from] OlmError),
    #[error(transparent)]
    Serialization(#[from] serde_json::Error),
    #[error("The given string is not a valid user ID: source {0}, error {1}")]
    InvalidUserId(String, RumaIdentifierError),
    #[error(transparent)]
    Identifier(#[from] RumaIdentifierError),
}

#[derive(Debug, thiserror::Error)]
pub enum DecryptionError {
    #[error(transparent)]
    Serialization(#[from] serde_json::Error),
    #[error(transparent)]
    Identifier(#[from] RumaIdentifierError),
    #[error(transparent)]
    Megolm(#[from] MegolmError),
}
