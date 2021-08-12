/*
 * Copyright (c) 2021 New Vector Ltd
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

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.MXCryptoAlgorithms
import org.matrix.android.sdk.internal.crypto.crosssigning.canonicalSignable
import org.matrix.android.sdk.internal.crypto.dehydration.model.DehydratedDevice
import org.matrix.android.sdk.internal.crypto.dehydration.model.DehydratedDeviceData
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceKeys
import org.matrix.android.sdk.internal.crypto.model.rest.KeysUploadResponse
import org.matrix.android.sdk.internal.crypto.tasks.UploadKeysTask
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmException
import timber.log.Timber
import javax.inject.Inject
import dagger.Lazy
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.util.awaitCallback

const val DehydrationAlgorithm = "org.matrix.msc2697.v1.olm.libolm_pickle"

sealed class DehydrationResult {

    data class Success(val deviceId: String) : DehydrationResult()
    object Canceled : DehydrationResult()

    abstract class Failure(val error: String?) : DehydrationResult()

    data class GenericError(val failure: Throwable) : Failure(failure.localizedMessage)
    data class MatrixError(val matrixError: MatrixError) : Failure(null)
    class FailedToPickleAccount(failureMsg: String) : Failure(failureMsg)
    class FailedToSetDehydratedDevice(failure: org.matrix.android.sdk.api.failure.Failure) : Failure(failure.localizedMessage)
    class FailedToSignDevice(failure: OlmException) : Failure(failure.localizedMessage)
    class FailedToUploadOneTimeKeys(failure: org.matrix.android.sdk.api.failure.Failure) : Failure(failure.localizedMessage)
}

sealed class RehydrationResult {
    data class Success(val deviceId: String) : RehydrationResult()
    object Canceled : RehydrationResult()

    abstract class Failure(val error: String?) : RehydrationResult()

    data class GenericError(val failure: Throwable) : Failure(failure.localizedMessage)
    data class MatrixError(val matrixError: MatrixError) : Failure(null)
    class FailedToGetDehydratedDevice(failure: org.matrix.android.sdk.api.failure.Failure) : Failure(failure.localizedMessage)
    class UnsupportedPickleAlgorithm(val algorithm: String) : Failure(null)
    class FailedToClaimDehydratedDevice(failure: org.matrix.android.sdk.api.failure.Failure) : Failure(failure.localizedMessage)
}

internal class DehydrationManager @Inject constructor(
        private val cryptoService: Lazy<DefaultCryptoService>,
        @UserId private val userId: String,
        private val setDehydratedDeviceTask: Lazy<SetDehydratedDeviceTask>,
        private val uploadKeysTask: Lazy<UploadKeysTask>,
        private val getDehydratedDeviceTask: Lazy<GetDehydratedDeviceTask>,
        private val claimDehydratedDeviceTask: Lazy<ClaimDehydratedDeviceTask>
): SessionLifecycleObserver {

    private var isSessionOpen = false

    override fun onSessionStarted(session: Session) {
        super.onSessionStarted(session)

        isSessionOpen = true
    }

    override fun onSessionStopped(session: Session) {
        super.onSessionStopped(session)

        isSessionOpen = false
    }

    suspend fun dehydrateDevice(deviceDisplayName: String, dehydrationKey: ByteArray): DehydrationResult {
        //TODO manage key here

        if (!isSessionOpen) {
            Timber.e("[DehydrationManager] dehydrateDevice: Cannot dehydrate device if session is not open.")
            return DehydrationResult.Canceled
        }

        val account = OlmAccount()
        val e2eKeys = account.identityKeys()

        val maxKeys = account.maxOneTimeKeys().toInt()
        account.generateOneTimeKeys(maxKeys / 2)

        //account.generateFallbackKey()

        Timber.d("[DehydrationManager] dehydrateDevice: account created $e2eKeys")

        val errorMessage = StringBuffer()
        val pickledAccount = account.pickle(dehydrationKey, errorMessage)
        if (errorMessage.isNotBlank()) {
            return DehydrationResult.FailedToPickleAccount(errorMessage.toString())
        }

        val dehydratedDevice = DehydratedDevice(
                displayName = deviceDisplayName,
                deviceData = DehydratedDeviceData(
                        algorithm = DehydrationAlgorithm,
                        account = pickledAccount.toString(Charsets.UTF_8)
                )
        )

        try {
            val deviceDehydrationParams = SetDehydratedDeviceTask.Params(dehydratedDevice)
            val deviceDehydrationResponse = setDehydratedDeviceTask.get().execute(deviceDehydrationParams)

            deviceDehydrationResponse.deviceId.let { deviceId ->
                val deviceInfo = CryptoDeviceInfo(
                        userId = userId,
                        deviceId = deviceId,
                        keys = mapOf(
                                "ed25519:$deviceId" to e2eKeys["ed25519"].toString(),
                                "curve25519:$deviceId" to e2eKeys["curve25519"].toString(),
                        ),
                        algorithms = MXCryptoAlgorithms.supportedAlgorithms()
                )

                try {
                    val signature = account.signMessage(deviceInfo.canonicalSignable())
                    val deviceKeys = DeviceKeys(
                            userId = deviceInfo.userId,
                            deviceId = deviceInfo.deviceId,
                            keys = deviceInfo.keys,
                            algorithms = deviceInfo.algorithms,
                            signatures = mapOf(
                                    userId to mapOf(
                                            "ed25519:$deviceId" to signature
                                    )
                            )
                    )

                    try {
                        awaitCallback<Unit> { callback ->
                            cryptoService.get().crossSigningService().trustDevice(deviceInfo.deviceId, callback)
                        }
                    } catch (failure: Throwable) {
                        Timber.w("[DehydrationManager] dehydrateDevice: failed to trust dehydrated device: ${failure.localizedMessage}");
                    }

                    return try {
                        uploadOneTimeKeys(deviceId, deviceKeys, account)
                        account.markOneTimeKeysAsPublished()
                        DehydrationResult.Success(deviceInfo.deviceId)
                    } catch (failure: Failure) {
                        DehydrationResult.FailedToUploadOneTimeKeys(failure)
                    }
                } catch (error: OlmException) {
                    return DehydrationResult.FailedToSignDevice(error)
                }
            }
        } catch (failure: Failure) {
            return DehydrationResult.FailedToSetDehydratedDevice(failure)
        }
    }

    suspend fun rehydrateDevice(dehydrationKey: ByteArray): RehydrationResult {
        if (isSessionOpen) {
            Timber.e("[DehydrationManager] rehydrateDevice: Cannot rehydrate device after session is open.")
            return RehydrationResult.Canceled
        }

        //TODO manage key here

        try {
            val dehydratedDevice = getDehydratedDeviceTask.get().execute(Unit)
            if (dehydratedDevice.deviceId.isNullOrBlank()) {
                Timber.d("[DehydrationManager] rehydrateDevice: No dehydrated device found.")
                return RehydrationResult.Canceled
            }

            if (dehydratedDevice.deviceData.algorithm != DehydrationAlgorithm) {
                Timber.e("[DehydrationManager] rehydrateDevice: Unsupported algorithm for dehydrated device ${dehydratedDevice.deviceData.algorithm}.")
                return RehydrationResult.UnsupportedPickleAlgorithm(dehydratedDevice.deviceData.algorithm)
            }

            val account = OlmAccount()
            account.unpickle(dehydratedDevice.deviceData.account.toByteArray(Charsets.UTF_8), dehydrationKey)

            Timber.d("[DehydrationManager] rehydrateDevice: account unpickled ${account.identityKeys()}")

            val claimResponse = claimDehydratedDeviceTask.get().execute(Unit)
            try {
                if (!claimResponse.success) {
                    Timber.d("[DehydrationManager] rehydrateDevice: device already claimed.")
                    return RehydrationResult.Canceled
                }

                Timber.d("[DehydrationManager] rehydrateDevice: exporting dehydrated device with ID ${dehydratedDevice.deviceId}")
                //TODO rehydrate exported OLM device
            } catch (failure: Failure) {
                Timber.e("[DehydrationManager] rehydrateDevice: claimDehydratedDeviceWithId failed with error: ${failure.localizedMessage}")
                return RehydrationResult.FailedToClaimDehydratedDevice(failure)
            }
        } catch (failure: Failure) {
            return if (failure is Failure.ServerError && failure.error.code == MatrixError.M_NOT_FOUND) {
                Timber.d("[DehydrationManager] rehydrateDevice: No dehydrated device found.")
                RehydrationResult.Canceled
            } else {
                Timber.e("[DehydrationManager] rehydrateDevice: dehydratedDeviceId failed with error: ${failure.localizedMessage}")
                RehydrationResult.FailedToGetDehydratedDevice(failure)
            }
        }

        return RehydrationResult.Canceled
    }

    @Throws
    private suspend fun uploadOneTimeKeys(deviceId: String, deviceKeys: DeviceKeys, account: OlmAccount): KeysUploadResponse {
        val oneTimeJson = mutableMapOf<String, Any>()

        val curve25519Map = account.oneTimeKeys()

        curve25519Map.forEach { (key_id, value) ->
            val k = mutableMapOf<String, Any>()
            k["key"] = value

            // the key is also signed
            val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, k)

            k["signatures"] = account.signMessage(canonicalJson)

            oneTimeJson["signed_curve25519:$key_id"] = k
        }

        // For now, we set the device id explicitly, as we may not be using the
        // same one as used in login.
        val uploadParams = UploadKeysTask.Params(deviceKeys, oneTimeJson, deviceId)
        return uploadKeysTask.get().execute(uploadParams)
    }
}
