use hmac::Hmac;
use pbkdf2::pbkdf2;
use rand::{distributions::Alphanumeric, thread_rng, Rng};
use sha2::Sha512;
use std::{collections::HashMap, iter};
use thiserror::Error;

use matrix_sdk_crypto::{
    backups::{OlmPkDecryptionError, RecoveryKey},
    store::CryptoStoreError as InnerStoreError,
};

/// The private part of the backup key, the one used for recovery.
pub struct BackupRecoveryKey {
    pub(crate) inner: RecoveryKey,
    passphrase_info: Option<PassphraseInfo>,
}

/// Error type for the decryption of backed up room keys.
#[derive(Debug, Error)]
pub enum PkDecryptionError {
    /// An internal libolm error happened during decryption.
    #[error("Error decryption a PkMessage {0}")]
    Olm(#[from] OlmPkDecryptionError),
}

#[derive(Debug, Error)]
pub enum DecodeError {
    /// An error happened while decoding the recovery key.
    #[error(transparent)]
    Decode(#[from] matrix_sdk_crypto::backups::DecodeError),
    /// An error happened in the storage layer,
    #[error(transparent)]
    CryptoStore(#[from] InnerStoreError),
}

/// Struct containing info about the way the backup key got derived from a
/// passphrase.
#[derive(Debug, Clone)]
pub struct PassphraseInfo {
    /// The salt that was used during key derivation.
    pub private_key_salt: String,
    /// The number of PBKDF rounds that were used for key derivation.
    pub private_key_iterations: i32,
}

/// The public part of the backup key.
pub struct MegolmV1BackupKey {
    /// The actuall base64 encoded public key.
    pub public_key: String,
    /// Signatures that have signed our backup key.
    pub signatures: HashMap<String, HashMap<String, String>>,
    /// The passphrase info, if the key was derived from one.
    pub passphrase_info: Option<PassphraseInfo>,
    /// Get the full name of the backup algorithm this backup key supports.
    pub backup_algorithm: String,
}

impl BackupRecoveryKey {
    const KEY_SIZE: usize = 32;
    const SALT_SIZE: usize = 32;
    const PBKDF_ROUNDS: i32 = 500_000;

    /// Create a new random [`BackupRecoveryKey`].
    pub fn new() -> Self {
        Self {
            inner: RecoveryKey::new()
                .expect("Can't gather enough randomness to create a recovery key"),
            passphrase_info: None,
        }
    }

    /// Try to create a [`BackupRecoveryKey`] from a base 64 encoded string.
    pub fn from_base64(key: String) -> Result<Self, DecodeError> {
        Ok(Self {
            inner: RecoveryKey::from_base64(&key)?,
            passphrase_info: None,
        })
    }

    /// Try to create a [`BackupRecoveryKey`] from a base 58 encoded string.
    pub fn from_base58(key: String) -> Result<Self, DecodeError> {
        Ok(Self {
            inner: RecoveryKey::from_base58(&key)?,
            passphrase_info: None,
        })
    }

    /// Create a new [`BackupRecoveryKey`] from the given passphrase.
    pub fn new_from_passphrase(passphrase: String) -> Self {
        let mut rng = thread_rng();
        let salt: String = iter::repeat(())
            .map(|()| rng.sample(Alphanumeric))
            .map(char::from)
            .take(Self::SALT_SIZE)
            .collect();

        Self::from_passphrase(passphrase, salt, Self::PBKDF_ROUNDS)
    }

    /// Restore a [`BackupRecoveryKey`] from the given passphrase.
    pub fn from_passphrase(passphrase: String, salt: String, rounds: i32) -> Self {
        let mut key = [0u8; Self::KEY_SIZE];
        let rounds = rounds as u32;

        pbkdf2::<Hmac<Sha512>>(passphrase.as_bytes(), salt.as_bytes(), rounds, &mut key);

        Self {
            inner: RecoveryKey::from_bytes(key),
            passphrase_info: Some(PassphraseInfo {
                private_key_salt: salt,
                private_key_iterations: rounds as i32,
            }),
        }
    }

    /// Get the public part of the backup key.
    pub fn megolm_v1_public_key(&self) -> MegolmV1BackupKey {
        let public_key = self.inner.megolm_v1_public_key();

        let signatures: HashMap<String, HashMap<String, String>> = public_key
            .signatures()
            .into_iter()
            .map(|(k, v)| {
                (
                    k.to_string(),
                    v.into_iter().map(|(k, v)| (k.to_string(), v)).collect(),
                )
            })
            .collect();

        MegolmV1BackupKey {
            public_key: public_key.to_base64(),
            signatures,
            passphrase_info: self.passphrase_info.clone(),
            backup_algorithm: public_key.backup_algorithm().to_owned(),
        }
    }

    /// Convert the recovery key to a base 58 encoded string.
    pub fn to_base58(&self) -> String {
        self.inner.to_base58()
    }

    /// Convert the recovery key to a base 64 encoded string.
    pub fn to_base64(&self) -> String {
        self.inner.to_base64()
    }

    /// Try to decrypt a message that was encrypted using the public part of the
    /// backup key.
    pub fn decrypt_v1(
        &self,
        ephemeral_key: String,
        mac: String,
        ciphertext: String,
    ) -> Result<String, PkDecryptionError> {
        self.inner
            .decrypt_v1(ephemeral_key, mac, ciphertext)
            .map_err(|e| e.into())
    }
}
