/*
 * Copyright (c) 2022 New Vector Ltd
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
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCurve25519AuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
import org.matrix.android.sdk.internal.crypto.ObjectSigner
import org.matrix.android.sdk.internal.crypto.crosssigning.CrossSigningOlm
import org.matrix.android.sdk.internal.crypto.keysbackup.model.SignalableMegolmBackupAuthData
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmPkDecryption
import timber.log.Timber
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
            callback: MatrixCallback<MegolmBackupCreationInfo>
    ) = withContext(coroutineDispatchers.io) {
        when (algorithm) {
            MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP -> prepareCurve(password, progressListener, callback)
            /*
            MXCRYPTO_ALGORITHM_AES_256_BACKUP -> prepareAES(password, progressListener, callback)
             */
            else -> {
                callback.onFailure(IllegalStateException("Unknown algorithm"))
            }
        }
    }

    private fun prepareCurve(password: String?, progressListener: ProgressListener?, callback: MatrixCallback<MegolmBackupCreationInfo>) {
        val olmPkDecryption = OlmPkDecryption()
        try {
            val signalableMegolmBackupAuthData = if (password != null) {
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
                SignalableMegolmBackupAuthData(
                        publicKey = olmPkDecryption.setPrivateKey(generatePrivateKeyResult.privateKey),
                        privateKeySalt = generatePrivateKeyResult.salt,
                        privateKeyIterations = generatePrivateKeyResult.iterations
                )
            } else {
                val publicKey = olmPkDecryption.generateKey()
                SignalableMegolmBackupAuthData(
                        publicKey = publicKey
                )
            }

            val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, signalableMegolmBackupAuthData.signalableJSONDictionary())

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

            val signedMegolmBackupCurve25519AuthData = MegolmBackupCurve25519AuthData(
                    publicKey = signalableMegolmBackupAuthData.publicKey,
                    privateKeySalt = signalableMegolmBackupAuthData.privateKeySalt,
                    privateKeyIterations = signalableMegolmBackupAuthData.privateKeyIterations,
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

    /*
    private fun prepareAES(password: String?, progressListener: ProgressListener?, callback: MatrixCallback<MegolmBackupCreationInfo>) {
    }

     */
}
