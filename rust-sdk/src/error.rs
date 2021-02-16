use matrix_sdk_common::identifiers::Error as RumaIdentifierError;

use matrix_sdk_crypto::{store::CryptoStoreError as InnerStoreError, OlmError};

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
