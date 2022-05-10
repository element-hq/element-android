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

package org.matrix.android.sdk.internal.crypto.dehydration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.MXCryptoAlgorithms
import org.matrix.android.sdk.internal.crypto.ObjectSigner
import org.matrix.android.sdk.internal.crypto.crosssigning.canonicalSignable
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceKeys
import org.matrix.android.sdk.internal.crypto.model.rest.dehydration.DehydratedDeviceData
import org.matrix.android.sdk.internal.crypto.tasks.UploadDehydratedDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.UploadKeysTask
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmAccount
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

/**
 * The class is responsible for handling the dehydration mechanism
 */

private val loggerTag = LoggerTag("DehydrationManager", LoggerTag.CRYPTO)

@SessionScope
internal class DehydrationManager @Inject constructor(
        matrixConfiguration: MatrixConfiguration,
        private val uploadDehydratedDeviceTask: UploadDehydratedDeviceTask,
        private val cryptoCoroutineScope: CoroutineScope,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val objectSigner: ObjectSigner,
        private val uploadKeysTask: UploadKeysTask,
        private val credentials: Credentials
) {
    private companion object {
        private const val dehydratedDeviceAlgorithm = "org.matrix.msc2697.v1.olm.libolm_pickle"
        private const val deviceDisplayName: String = "Backup device"
    }

    private val isDehydrationEnabled = matrixConfiguration.isDehydrationEnabled
    private val inProgress = false

    /**
     * Upload and replace any existing dehydrated device
     */
    fun dehydrateDevice(dehydrationKey: ByteArray) = cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
        if (!isDehydrationEnabled) {
            Timber.tag(loggerTag.value).d("Dehydration is disabled")
            return@launch
        }
        if (inProgress) {
            Timber.tag(loggerTag.value).d("Dehydration already in progress")
            return@launch
        }

        runCatching {
            val account = createOlmAccount()
            val errorMessage = StringBuffer()
            val pickledAccount = account.pickle(dehydrationKey, errorMessage)

            if (errorMessage.isNotBlank()) {
                Timber.tag(loggerTag.value).d("Failed to create pickled account")
                return@launch
            }

            val dehydratedDevice = UploadDehydratedDeviceTask.Params(
                    displayName = deviceDisplayName,
                    deviceData = DehydratedDeviceData(
                            algorithm = dehydratedDeviceAlgorithm,
                            account = pickledAccount.toString(Charsets.UTF_8)
                    )
            )
            // Upload the dehydrated device
            val dehydratedDeviceId = uploadDehydratedDeviceTask.execute(dehydratedDevice).deviceId
            try {
                Timber.tag(loggerTag.value).d("Generating and uploading device keys")
                val deviceKeys = generateDeviceKeys(dehydratedDeviceId, account)
                val oneTimeJson = generateOneTimeKeys(account)
                val fallbackJson = generateFallbackKeys(account)
                val uploadDeviceKeysParams = UploadKeysTask.Params(deviceKeys, oneTimeJson, fallbackJson)
                uploadKeysTask.execute(uploadDeviceKeysParams)
                Timber.tag(loggerTag.value).d("Device dehydrated successfully!")
            } catch (ex: Exception) {
                Timber.tag(loggerTag.value).e(ex, "Generating & uploading device keys failed")
                return@launch
            }
        }.onFailure {
            Timber.tag(loggerTag.value).e(it, "Failed to upload dehydration device")
        }

    }

    /**
     * Generate one time keys from the given account
     */
    private fun generateOneTimeKeys(account: OlmAccount): Map<String, Any> {
        // Create one time keys
        val oneTimeJson = mutableMapOf<String, Any>()
        // ------
        // ????? Why it was here account.oneTimeKeys() in the PR
        // -----
        val curve25519Map = account.oneTimeKeys()[OlmAccount.JSON_KEY_ONE_TIME_KEY].orEmpty()
        curve25519Map.forEach { (key_id, value) ->
            val k = mutableMapOf<String, Any>()
            k["key"] = value
            val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, k)
            k["signatures"] = objectSigner.signObject(canonicalJson)
            oneTimeJson["signed_curve25519:$key_id"] = k
        }
        return oneTimeJson
    }

    /**
     * Generate one fallback keys from the given account
     */
    private fun generateFallbackKeys(account: OlmAccount): Map<String, Any> {
        val fallbackJson = mutableMapOf<String, Any>()
        val fallbackCurve25519Map = account.fallbackKey()[OlmAccount.JSON_KEY_ONE_TIME_KEY].orEmpty()
        fallbackCurve25519Map.forEach { (key_id, key) ->
            val k = mutableMapOf<String, Any>()
            k["key"] = key
            k["fallback"] = true
            val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, k)
            k["signatures"] = objectSigner.signObject(canonicalJson)
            fallbackJson["signed_curve25519:$key_id"] = k
        }
        return fallbackJson
    }

    /**
     * Generates device keys
     * @param dehydratedDeviceId the dehydrated device id
     * @param account the newly created olm account
     */
    private fun generateDeviceKeys(dehydratedDeviceId: String, account: OlmAccount): DeviceKeys {
        val deviceInfo = generateDeviceInfo(dehydratedDeviceId, account)
        val signature = account.signMessage(deviceInfo.canonicalSignable())
        return DeviceKeys(
                userId = deviceInfo.userId,
                deviceId = deviceInfo.deviceId,
                keys = deviceInfo.keys,
                algorithms = deviceInfo.algorithms,
                signatures = mapOf(
                        credentials.userId to mapOf(
                                "ed25519:$dehydratedDeviceId" to signature
                        )
                )
        )
    }

    /**
     * Generates a CryptoDeviceInfo object from provided dehydratedDeviceId
     */
    private fun generateDeviceInfo(dehydratedDeviceId: String, account: OlmAccount): CryptoDeviceInfo {
        val e2eKeys = account.identityKeys()
        return CryptoDeviceInfo(
                userId = credentials.userId,
                deviceId = dehydratedDeviceId,
                keys = mapOf(
                        "ed25519:$dehydratedDeviceId" to e2eKeys["ed25519"].toString(),
                        "curve25519:$dehydratedDeviceId" to e2eKeys["curve25519"].toString()
                ),
                algorithms = MXCryptoAlgorithms.supportedAlgorithms()
        )
    }

    /**
     * Creates a new OlmAccount
     */
    private fun createOlmAccount(): OlmAccount {
        val account = OlmAccount()
        val maxKeys = account.maxOneTimeKeys().toInt()
        account.generateOneTimeKeys(maxKeys / 2)
        account.generateFallbackKey()
        return account
    }

    /**
     * Rehydrate and claim an existing dehydrated device
     */
    fun rehydrateDevice() {
    }
}
