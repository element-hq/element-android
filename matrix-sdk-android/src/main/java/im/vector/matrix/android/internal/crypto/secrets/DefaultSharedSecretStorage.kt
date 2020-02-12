/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.crypto.secrets

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.session.accountdata.AccountDataService
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.securestorage.Curve25519AesSha2KeySpec
import im.vector.matrix.android.api.session.securestorage.EncryptedSecretContent
import im.vector.matrix.android.api.session.securestorage.KeyInfo
import im.vector.matrix.android.api.session.securestorage.KeyInfoResult
import im.vector.matrix.android.api.session.securestorage.KeySigner
import im.vector.matrix.android.api.session.securestorage.SSSSKeyCreationInfo
import im.vector.matrix.android.api.session.securestorage.SSSSKeySpec
import im.vector.matrix.android.api.session.securestorage.SSSSPassphrase
import im.vector.matrix.android.api.session.securestorage.SecretStorageKeyContent
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageError
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageService
import im.vector.matrix.android.internal.crypto.keysbackup.generatePrivateKeyWithPassword
import im.vector.matrix.android.internal.crypto.keysbackup.util.computeRecoveryKey
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.olm.OlmPkDecryption
import org.matrix.olm.OlmPkEncryption
import org.matrix.olm.OlmPkMessage
import javax.inject.Inject

internal class DefaultSharedSecureStorage @Inject constructor(
        private val accountDataService: AccountDataService,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope
) : SharedSecretStorageService {

    override fun generateKey(keyId: String,
                             keyName: String,
                             keySigner: KeySigner,
                             callback: MatrixCallback<SSSSKeyCreationInfo>) {

        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            val pkDecryption = OlmPkDecryption()
            val pubKey: String
            val privateKey: ByteArray
            try {
                pubKey = pkDecryption.generateKey()
                privateKey = pkDecryption.privateKey()
            } catch (failure: Throwable) {
                return@launch Unit.also {
                    callback.onFailure(failure)
                }
            } finally {
                pkDecryption.releaseDecryption()
            }

            val storageKeyContent = SecretStorageKeyContent(
                    name = keyName,
                    algorithm = ALGORITHM_CURVE25519_AES_SHA2,
                    passphrase = null,
                    publicKey = pubKey
            )

            val signedContent = keySigner.sign(storageKeyContent.canonicalSignable())?.let {
                storageKeyContent.copy(
                        signatures = it
                )
            } ?: storageKeyContent

            accountDataService.updateAccountData(
                    "$KEY_ID_BASE.$keyId",
                    signedContent.toContent(),
                    object : MatrixCallback<Unit> {
                        override fun onFailure(failure: Throwable) {
                            callback.onFailure(failure)
                        }

                        override fun onSuccess(data: Unit) {
                            callback.onSuccess(SSSSKeyCreationInfo(
                                    keyId = keyId,
                                    content = storageKeyContent,
                                    recoveryKey = computeRecoveryKey(privateKey)
                            ))
                        }
                    }
            )
        }
    }

    override fun generateKeyWithPassphrase(keyId: String,
                                           keyName: String,
                                           passphrase: String,
                                           keySigner: KeySigner,
                                           progressListener: ProgressListener?,
                                           callback: MatrixCallback<SSSSKeyCreationInfo>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {

            val privatePart = generatePrivateKeyWithPassword(passphrase, progressListener)

            val pkDecryption = OlmPkDecryption()
            val pubKey: String
            try {
                pubKey = pkDecryption.setPrivateKey(privatePart.privateKey)
            } catch (failure: Throwable) {
                return@launch Unit.also {
                    callback.onFailure(failure)
                }
            } finally {
                pkDecryption.releaseDecryption()
            }

            val storageKeyContent = SecretStorageKeyContent(
                    algorithm = ALGORITHM_CURVE25519_AES_SHA2,
                    passphrase = SSSSPassphrase(algorithm = "m.pbkdf2", iterations = privatePart.iterations, salt = privatePart.salt),
                    publicKey = pubKey
            )

            val signedContent = keySigner.sign(storageKeyContent.canonicalSignable())?.let {
                storageKeyContent.copy(
                        signatures = it
                )
            } ?: storageKeyContent

            accountDataService.updateAccountData(
                    "$KEY_ID_BASE.$keyId",
                    signedContent.toContent(),
                    object : MatrixCallback<Unit> {
                        override fun onFailure(failure: Throwable) {
                            callback.onFailure(failure)
                        }

                        override fun onSuccess(data: Unit) {
                            callback.onSuccess(SSSSKeyCreationInfo(
                                    keyId = keyId,
                                    content = storageKeyContent,
                                    recoveryKey = computeRecoveryKey(privatePart.privateKey)
                            ))
                        }
                    }
            )

        }
    }

    override fun hasKey(keyId: String): Boolean {
        return accountDataService.getAccountData("$KEY_ID_BASE.$keyId") != null
    }

    override fun getKey(keyId: String): KeyInfoResult {
        val accountData = accountDataService.getAccountData("$KEY_ID_BASE.$keyId")
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(keyId))
        return SecretStorageKeyContent.fromJson(accountData.content)?.let {
            KeyInfoResult.Success(
                    KeyInfo(id = keyId, content = it)
            )
        } ?: KeyInfoResult.Error(SharedSecretStorageError.UnknownAlgorithm(keyId))
    }

    override fun setDefaultKey(keyId: String, callback: MatrixCallback<Unit>) {
        val existingKey = getKey(keyId)
        if (existingKey is KeyInfoResult.Success) {
            accountDataService.updateAccountData(DEFAULT_KEY_ID,
                    mapOf("key" to keyId),
                    callback
            )
        } else {
            callback.onFailure(SharedSecretStorageError.UnknownKey(keyId))
        }
    }

    override fun getDefaultKey(): KeyInfoResult {
        val accountData = accountDataService.getAccountData(DEFAULT_KEY_ID)
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(DEFAULT_KEY_ID))
        val keyId = accountData.content["key"] as? String
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(DEFAULT_KEY_ID))
        return getKey(keyId)
    }

    override fun storeSecret(name: String, secretBase64: String, keys: List<String>?, callback: MatrixCallback<Unit>) {

        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            val encryptedContents = HashMap<String, EncryptedSecretContent>()
            try {

                if (keys == null || keys.isEmpty()) {
                    //use default key
                    val key = getDefaultKey()
                    when (key) {
                        is KeyInfoResult.Success -> {
                            if (key.keyInfo.content.algorithm == ALGORITHM_CURVE25519_AES_SHA2) {
                                withOlmEncryption { olmEncrypt ->
                                    olmEncrypt.setRecipientKey(key.keyInfo.content.publicKey)
                                    val encryptedResult = olmEncrypt.encrypt(secretBase64)
                                    encryptedContents[key.keyInfo.id] = EncryptedSecretContent(
                                            ciphertext = encryptedResult.mCipherText,
                                            ephemeral = encryptedResult.mEphemeralKey,
                                            mac = encryptedResult.mMac
                                    )
                                }
                            } else {
                                // Unknown algorithm
                                callback.onFailure(SharedSecretStorageError.UnknownAlgorithm(key.keyInfo.content.algorithm ?: ""))
                                return@launch
                            }
                        }
                        is KeyInfoResult.Error   -> {
                            callback.onFailure(key.error)
                            return@launch
                        }
                    }
                } else {
                    keys.forEach {
                        val keyId = it
                        // encrypt the content
                        val key = getKey(keyId)
                        when (key) {
                            is KeyInfoResult.Success -> {
                                if (key.keyInfo.content.algorithm == ALGORITHM_CURVE25519_AES_SHA2) {
                                    withOlmEncryption { olmEncrypt ->
                                        olmEncrypt.setRecipientKey(key.keyInfo.content.publicKey)
                                        val encryptedResult = olmEncrypt.encrypt(secretBase64)
                                        encryptedContents[keyId] = EncryptedSecretContent(
                                                ciphertext = encryptedResult.mCipherText,
                                                ephemeral = encryptedResult.mEphemeralKey,
                                                mac = encryptedResult.mMac
                                        )
                                    }
                                } else {
                                    // Unknown algorithm
                                    callback.onFailure(SharedSecretStorageError.UnknownAlgorithm(key.keyInfo.content.algorithm ?: ""))
                                    return@launch
                                }
                            }
                            is KeyInfoResult.Error   -> {
                                callback.onFailure(key.error)
                                return@launch
                            }
                        }
                    }
                }

                accountDataService.updateAccountData(
                        type = name,
                        content = mapOf(
                                "encrypted" to encryptedContents
                        ),
                        callback = callback
                )
            } catch (failure: Throwable) {
                callback.onFailure(failure)
            }

        }

        // Add default key
    }

    override fun getAlgorithmsForSecret(name: String): List<KeyInfoResult> {
        val accountData = accountDataService.getAccountData(name)
                ?: return listOf(KeyInfoResult.Error(SharedSecretStorageError.UnknownSecret(name)))
        val encryptedContent = accountData.content[ENCRYPTED] as? Map<*, *>
                ?: return listOf(KeyInfoResult.Error(SharedSecretStorageError.SecretNotEncrypted(name)))

        val results = ArrayList<KeyInfoResult>()
        encryptedContent.keys.forEach {
            (it as? String)?.let { keyId ->
                results.add(getKey(keyId))
            }
        }
        return results
    }

    override fun getSecret(name: String, keyId: String?, secretKey: SSSSKeySpec, callback: MatrixCallback<String>) {
        val accountData = accountDataService.getAccountData(name) ?: return Unit.also {
            callback.onFailure(SharedSecretStorageError.UnknownSecret(name))
        }
        val encryptedContent = accountData.content[ENCRYPTED] as? Map<*, *> ?: return Unit.also {
            callback.onFailure(SharedSecretStorageError.SecretNotEncrypted(name))
        }
        val key = keyId?.let { getKey(it) } as? KeyInfoResult.Success ?: getDefaultKey() as? KeyInfoResult.Success ?: return Unit.also {
            callback.onFailure(SharedSecretStorageError.UnknownKey(name))
        }

        val encryptedForKey = encryptedContent[key.keyInfo.id] ?: return Unit.also {
            callback.onFailure(SharedSecretStorageError.SecretNotEncryptedWithKey(name, key.keyInfo.id))
        }

        val secretContent = EncryptedSecretContent.fromJson(encryptedForKey)
                ?: return Unit.also {
                    callback.onFailure(SharedSecretStorageError.ParsingError)
                }

        val algorithm = key.keyInfo.content
        if (ALGORITHM_CURVE25519_AES_SHA2 == algorithm.algorithm) {
            val keySpec = secretKey as? Curve25519AesSha2KeySpec ?: return Unit.also {
                callback.onFailure(SharedSecretStorageError.BadKeyFormat)
            }
            cryptoCoroutineScope.launch(coroutineDispatchers.main) {
                kotlin.runCatching {
                    // decryt from recovery key
                    val keyBytes = keySpec.privateKey
                    val decryption = OlmPkDecryption()
                    try {
                        decryption.setPrivateKey(keyBytes)
                        decryption.decrypt(OlmPkMessage().apply {
                            mCipherText = secretContent.ciphertext
                            mEphemeralKey = secretContent.ephemeral
                            mMac = secretContent.mac
                        })
                    } catch (failure: Throwable) {
                        throw failure
                    } finally {
                        decryption.releaseDecryption()
                    }
                }.foldToCallback(callback)
            }
        } else {
            callback.onFailure(SharedSecretStorageError.UnsupportedAlgorithm(algorithm.algorithm ?: ""))
        }
    }

    companion object {
        const val KEY_ID_BASE = "m.secret_storage.key"
        const val ENCRYPTED = "encrypted"
        const val DEFAULT_KEY_ID = "m.secret_storage.default_key"

        const val ALGORITHM_CURVE25519_AES_SHA2 = "m.secret_storage.v1.curve25519-aes-sha2"

        fun withOlmEncryption(block: (OlmPkEncryption) -> Unit) {
            val olmPkEncryption = OlmPkEncryption()
            try {
                block(olmPkEncryption)
            } catch (failure: Throwable) {
                throw failure
            } finally {
                olmPkEncryption.releaseEncryption()
            }
        }

        fun withOlmDecryption(block: (OlmPkDecryption) -> Unit) {
            val olmPkDecryption = OlmPkDecryption()
            try {
                block(olmPkDecryption)
            } catch (failure: Throwable) {
                throw failure
            } finally {
                olmPkDecryption.releaseDecryption()
            }
        }
    }
}


