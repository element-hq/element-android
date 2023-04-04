/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.keysbackup

import android.os.Handler
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_AES_256_BACKUP
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeyBackupConfig
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAes256AuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCurve25519AuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.ObjectSigner
import org.matrix.android.sdk.internal.crypto.crosssigning.CrossSigningOlm
import org.matrix.android.sdk.internal.crypto.tools.AesHmacSha2
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmPkDecryption
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject

internal class PrepareKeysBackupUseCase @Inject constructor(
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val objectSigner: ObjectSigner,
        private val credentials: Credentials,
        private val crossSigningOlm: CrossSigningOlm,
        private val uiHandler: Handler,
) {

    suspend operator fun invoke(
            algorithm: String,
            password: String?,
            progressListener: ProgressListener?,
            config: KeyBackupConfig,
            callback: MatrixCallback<MegolmBackupCreationInfo>
    ) = withContext(coroutineDispatchers.io) {
        if (!config.isAlgorithmSupported(algorithm)) return@withContext Unit.also {
            callback.onFailure(IllegalArgumentException("Unsupported algorithm"))
        }
        when (algorithm) {
            MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP -> prepareCurve(password, progressListener, callback)
            MXCRYPTO_ALGORITHM_AES_256_BACKUP -> prepareAES(password, progressListener, callback)
            else -> {
                callback.onFailure(IllegalStateException("Unknown algorithm"))
            }
        }
    }

    private fun prepareCurve(password: String?, progressListener: ProgressListener?, callback: MatrixCallback<MegolmBackupCreationInfo>) {
        val olmPkDecryption = OlmPkDecryption()
        try {
            val megolmBackupAuthData = if (password != null) {
                // Generate a private key from the password
                val backgroundProgressListener = if (progressListener == null) {
                    null
                } else {
                    object : ProgressListener {
                        override fun onProgress(progress: Int, total: Int) {
                            uiHandler.post {
                                try {
                                    progressListener.onProgress(progress, total)
                                } catch (e: Exception) {
                                    Timber.e(e, "prepareKeysBackupVersion: onProgress failure")
                                }
                            }
                        }
                    }
                }
                val generatePrivateKeyResult = generatePrivateKeyWithPassword(password, backgroundProgressListener)
                MegolmBackupCurve25519AuthData(
                        publicKey = olmPkDecryption.setPrivateKey(generatePrivateKeyResult.privateKey),
                        privateKeySalt = generatePrivateKeyResult.salt,
                        privateKeyIterations = generatePrivateKeyResult.iterations
                )
            } else {
                val publicKey = olmPkDecryption.generateKey()
                MegolmBackupCurve25519AuthData(
                        publicKey = publicKey
                )
            }
            val signatures = signKeyBackup(megolmBackupAuthData)

            val signedMegolmBackupCurve25519AuthData = megolmBackupAuthData.copy(
                    signatures = signatures
            )
            val creationInfo = MegolmBackupCreationInfo(
                    algorithm = MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP,
                    authData = signedMegolmBackupCurve25519AuthData,
                    recoveryKey = computeRecoveryKey(olmPkDecryption.privateKey())
            )
            uiHandler.post {
                callback.onSuccess(creationInfo)
            }
        } catch (failure: Throwable) {
            uiHandler.post {
                callback.onFailure(failure)
            }
        } finally {
            olmPkDecryption.releaseDecryption()
        }
    }

    private fun signKeyBackup(authData: MegolmBackupAuthData): MutableMap<String, MutableMap<String, String>> {
        val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, authData.toSignalableJsonDict())
        val signatures = mutableMapOf<String, MutableMap<String, String>>()

        val deviceSignature = objectSigner.signObject(canonicalJson)
        deviceSignature.forEach { (userID, content) ->
            signatures[userID] = content.toMutableMap()
        }

        // If we have cross signing add signature, will throw if cross signing not properly configured
        try {
            val crossSign = crossSigningOlm.signObject(CrossSigningOlm.KeyType.MASTER, canonicalJson)
            signatures[credentials.userId]?.putAll(crossSign)
        } catch (failure: Throwable) {
            // ignore and log
            Timber.w(failure, "prepareKeysBackupVersion: failed to sign with cross signing keys")
        }
        return signatures
    }

    private fun prepareAES(password: String?, progressListener: ProgressListener?, callback: MatrixCallback<MegolmBackupCreationInfo>) {
        try {
            val privateKey: ByteArray
            val authData: MegolmBackupAes256AuthData
            if (password == null) {
                privateKey = ByteArray(32).also {
                    SecureRandom().nextBytes(it)
                }
                authData = MegolmBackupAes256AuthData()
            } else {
                val result = generatePrivateKeyWithPassword(password, progressListener)
                privateKey = result.privateKey
                authData = MegolmBackupAes256AuthData(
                        privateKeySalt = result.salt,
                        privateKeyIterations = result.iterations
                )
            }
            val encInfo = AesHmacSha2.calculateKeyCheck(privateKey, null)
            val authDataWithKeyCheck = authData.copy(
                    iv = encInfo.initializationVector.toBase64NoPadding(),
                    mac = encInfo.mac.toBase64NoPadding()
            )

            val signatures = signKeyBackup(authDataWithKeyCheck)
            val creationInfo = MegolmBackupCreationInfo(
                    algorithm = MXCRYPTO_ALGORITHM_AES_256_BACKUP,
                    authData = authDataWithKeyCheck.copy(
                            signatures = signatures
                    ),
                    recoveryKey = computeRecoveryKey(privateKey)
            )

            uiHandler.post {
                callback.onSuccess(creationInfo)
            }
        } catch (failure: Throwable) {
            uiHandler.post {
                callback.onFailure(failure)
            }
        }
    }
}
