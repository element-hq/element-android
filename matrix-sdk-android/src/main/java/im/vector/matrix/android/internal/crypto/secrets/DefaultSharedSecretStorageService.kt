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
import im.vector.matrix.android.api.session.securestorage.IntegrityResult
import im.vector.matrix.android.api.session.securestorage.KeyInfo
import im.vector.matrix.android.api.session.securestorage.KeyInfoResult
import im.vector.matrix.android.api.session.securestorage.KeySigner
import im.vector.matrix.android.api.session.securestorage.SsssKeySpec
import im.vector.matrix.android.api.session.securestorage.SsssPassphrase
import im.vector.matrix.android.api.session.securestorage.SecretStorageKeyContent
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageError
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageService
import im.vector.matrix.android.api.session.securestorage.SsssKeyCreationInfo
import im.vector.matrix.android.internal.crypto.SSSS_ALGORITHM_CURVE25519_AES_SHA2
import im.vector.matrix.android.internal.crypto.keysbackup.generatePrivateKeyWithPassword
import im.vector.matrix.android.internal.crypto.keysbackup.util.computeRecoveryKey
import im.vector.matrix.android.internal.crypto.tools.withOlmDecryption
import im.vector.matrix.android.internal.crypto.tools.withOlmEncryption
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.olm.OlmPkMessage
import javax.inject.Inject

private data class Key(
        val publicKey: String,
        @Suppress("ArrayInDataClass")
        val privateKey: ByteArray
)

internal class DefaultSharedSecretStorageService @Inject constructor(
        private val accountDataService: AccountDataService,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope
) : SharedSecretStorageService {

    override fun generateKey(keyId: String,
                             keyName: String,
                             keySigner: KeySigner,
                             callback: MatrixCallback<SsssKeyCreationInfo>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            val key = try {
                withOlmDecryption { olmPkDecryption ->
                    val pubKey = olmPkDecryption.generateKey()
                    val privateKey = olmPkDecryption.privateKey()
                    Key(pubKey, privateKey)
                }
            } catch (failure: Throwable) {
                callback.onFailure(failure)
                return@launch
            }

            val storageKeyContent = SecretStorageKeyContent(
                    name = keyName,
                    algorithm = SSSS_ALGORITHM_CURVE25519_AES_SHA2,
                    passphrase = null,
                    publicKey = key.publicKey
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
                            callback.onSuccess(SsssKeyCreationInfo(
                                    keyId = keyId,
                                    content = storageKeyContent,
                                    recoveryKey = computeRecoveryKey(key.privateKey)
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
                                           callback: MatrixCallback<SsssKeyCreationInfo>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            val privatePart = generatePrivateKeyWithPassword(passphrase, progressListener)

            val pubKey = try {
                withOlmDecryption { olmPkDecryption ->
                    olmPkDecryption.setPrivateKey(privatePart.privateKey)
                }
            } catch (failure: Throwable) {
                callback.onFailure(failure)
                return@launch
            }

            val storageKeyContent = SecretStorageKeyContent(
                    algorithm = SSSS_ALGORITHM_CURVE25519_AES_SHA2,
                    passphrase = SsssPassphrase(algorithm = "m.pbkdf2", iterations = privatePart.iterations, salt = privatePart.salt),
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
                            callback.onSuccess(SsssKeyCreationInfo(
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
        return accountDataService.getAccountDataEvent("$KEY_ID_BASE.$keyId") != null
    }

    override fun getKey(keyId: String): KeyInfoResult {
        val accountData = accountDataService.getAccountDataEvent("$KEY_ID_BASE.$keyId")
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
        val accountData = accountDataService.getAccountDataEvent(DEFAULT_KEY_ID)
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(DEFAULT_KEY_ID))
        val keyId = accountData.content["key"] as? String
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(DEFAULT_KEY_ID))
        return getKey(keyId)
    }

    override fun storeSecret(name: String, secretBase64: String, keys: List<String>?, callback: MatrixCallback<Unit>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            val encryptedContents = HashMap<String, EncryptedSecretContent>()
            try {
                if (keys.isNullOrEmpty()) {
                    // use default key
                    when (val key = getDefaultKey()) {
                        is KeyInfoResult.Success -> {
                            if (key.keyInfo.content.algorithm == SSSS_ALGORITHM_CURVE25519_AES_SHA2) {
                                val encryptedResult = withOlmEncryption { olmEncrypt ->
                                    olmEncrypt.setRecipientKey(key.keyInfo.content.publicKey)
                                    olmEncrypt.encrypt(secretBase64)
                                }
                                encryptedContents[key.keyInfo.id] = EncryptedSecretContent(
                                        ciphertext = encryptedResult.mCipherText,
                                        ephemeral = encryptedResult.mEphemeralKey,
                                        mac = encryptedResult.mMac
                                )
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
                        when (val key = getKey(keyId)) {
                            is KeyInfoResult.Success -> {
                                if (key.keyInfo.content.algorithm == SSSS_ALGORITHM_CURVE25519_AES_SHA2) {
                                    val encryptedResult = withOlmEncryption { olmEncrypt ->
                                        olmEncrypt.setRecipientKey(key.keyInfo.content.publicKey)
                                        olmEncrypt.encrypt(secretBase64)
                                    }
                                    encryptedContents[keyId] = EncryptedSecretContent(
                                            ciphertext = encryptedResult.mCipherText,
                                            ephemeral = encryptedResult.mEphemeralKey,
                                            mac = encryptedResult.mMac
                                    )
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
        val accountData = accountDataService.getAccountDataEvent(name)
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

    override fun getSecret(name: String, keyId: String?, secretKey: SsssKeySpec, callback: MatrixCallback<String>) {
        val accountData = accountDataService.getAccountDataEvent(name) ?: return Unit.also {
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
        if (SSSS_ALGORITHM_CURVE25519_AES_SHA2 == algorithm.algorithm) {
            val keySpec = secretKey as? Curve25519AesSha2KeySpec ?: return Unit.also {
                callback.onFailure(SharedSecretStorageError.BadKeyFormat)
            }
            cryptoCoroutineScope.launch(coroutineDispatchers.main) {
                kotlin.runCatching {
                    // decrypt from recovery key
                    withOlmDecryption { olmPkDecryption ->
                        olmPkDecryption.setPrivateKey(keySpec.privateKey)
                        olmPkDecryption.decrypt(OlmPkMessage()
                                .apply {
                                    mCipherText = secretContent.ciphertext
                                    mEphemeralKey = secretContent.ephemeral
                                    mMac = secretContent.mac
                                }
                        )
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
    }

    override fun checkShouldBeAbleToAccessSecrets(secretNames: List<String>, keyId: String?): IntegrityResult {
        if (secretNames.isEmpty()) {
            return IntegrityResult.Error(SharedSecretStorageError.UnknownSecret("none"))
        }

        val keyInfoResult = if (keyId == null) {
            getDefaultKey()
        } else {
            getKey(keyId)
        }

        val keyInfo = (keyInfoResult as? KeyInfoResult.Success)?.keyInfo
                ?: return IntegrityResult.Error(SharedSecretStorageError.UnknownKey(keyId ?: ""))

        if (keyInfo.content.algorithm != SSSS_ALGORITHM_CURVE25519_AES_SHA2) {
            // Unsupported algorithm
            return IntegrityResult.Error(
                    SharedSecretStorageError.UnsupportedAlgorithm(keyInfo.content.algorithm ?: "")
            )
        }

        secretNames.forEach { secretName ->
            val secretEvent = accountDataService.getAccountDataEvent(secretName)
                    ?: return IntegrityResult.Error(SharedSecretStorageError.UnknownSecret(secretName))
            if ((secretEvent.content["encrypted"] as? Map<*, *>)?.get(keyInfo.id) == null) {
                return IntegrityResult.Error(SharedSecretStorageError.SecretNotEncryptedWithKey(secretName, keyInfo.id))
            }
        }

        return IntegrityResult.Success(keyInfo.content.passphrase != null)
    }
}
