use hmac::Hmac;
use pbkdf2::pbkdf2;
use rand::{distributions::Alphanumeric, thread_rng, Rng};
use sha2::Sha512;
use std::{collections::HashMap, iter};

use matrix_sdk_crypto::backups::RecoveryKey;

/// TODO
pub struct BackupRecoveryKey {
    pub(crate) inner: RecoveryKey,
    passphrase_info: Option<PassphraseInfo>,
}

/// TODO
#[derive(Debug, Clone)]
pub struct PassphraseInfo {
    /// TODO
    pub private_key_salt: String,
    /// TODO
    pub private_key_iterations: i32,
}

/// TODO
pub struct BackupKey {
    /// TODO
    pub public_key: String,
    /// TODO
    pub signatures: HashMap<String, HashMap<String, String>>,
    /// TODO
    pub passphrase_info: Option<PassphraseInfo>,
}

impl BackupRecoveryKey {
    const KEY_SIZE: usize = 32;
    const SALT_SIZE: usize = 32;
    const PBKDF_ROUNDS: u32 = 500_000;

    /// TODO
    pub fn new() -> Self {
        Self {
            inner: RecoveryKey::new()
                .expect("Can't gather enough randomness to create a recovery key"),
            passphrase_info: None,
        }
    }

    /// TODO
    pub fn from_base64(key: String) -> Self {
        Self {
            inner: RecoveryKey::from_base64(key).unwrap(),
            passphrase_info: None,
        }
    }

    /// TODO
    pub fn from_base58(key: String) -> Self {
        Self {
            inner: RecoveryKey::from_base58(&key).unwrap(),
            passphrase_info: None,
        }
    }

    /// TODO
    pub fn from_passphrase(passphrase: String) -> Self {
        let mut key = [0u8; Self::KEY_SIZE];

        let mut rng = thread_rng();
        let salt: String = iter::repeat(())
            .map(|()| rng.sample(Alphanumeric))
            .map(char::from)
            .take(Self::SALT_SIZE)
            .collect();

        pbkdf2::<Hmac<Sha512>>(
            passphrase.as_bytes(),
            salt.as_bytes(),
            Self::PBKDF_ROUNDS,
            &mut key,
        );

        Self {
            inner: RecoveryKey::from_bytes(key),
            passphrase_info: Some(PassphraseInfo {
                private_key_salt: salt,
                private_key_iterations: Self::PBKDF_ROUNDS as i32,
            }),
        }
    }

    /// TODO
    pub fn public_key(&self) -> BackupKey {
        let public_key = self.inner.public_key();

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

        BackupKey {
            public_key: public_key.encoded_key(),
            signatures,
            passphrase_info: self.passphrase_info.clone(),
        }
    }

    /// TODO
    pub fn to_base58(&self) -> String {
        self.inner.to_base58()
    }
}
