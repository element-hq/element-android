/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.android.sdk.internal.crypto.verification.SasVerification
import org.matrix.android.sdk.internal.crypto.verification.VerificationRequest
import org.matrix.android.sdk.internal.crypto.verification.prepareMethods
import org.matrix.rustcomponents.sdk.crypto.CryptoStoreException
import org.matrix.rustcomponents.sdk.crypto.SignatureException
import uniffi.matrix_sdk_crypto.LocalTrust
import org.matrix.rustcomponents.sdk.crypto.Device as InnerDevice

/** Class representing a device that supports E2EE in the Matrix world
 *
 * This class can be used to directly start a verification flow with the device
 * or to manually verify the device.
 */
internal class Device @AssistedInject constructor(
        @Assisted private var innerDevice: InnerDevice,
        olmMachine: OlmMachine,
        private val requestSender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val verificationRequestFactory: VerificationRequest.Factory,
        private val sasVerificationFactory: SasVerification.Factory
) {

    @AssistedFactory
    interface Factory {
        fun create(innerDevice: InnerDevice): Device
    }

    private val innerMachine = olmMachine.inner()

    @Throws(CryptoStoreException::class)
    private suspend fun refreshData() {
        val device = withContext(coroutineDispatchers.io) {
            innerMachine.getDevice(innerDevice.userId, innerDevice.deviceId, 30u)
        }

        if (device != null) {
            innerDevice = device
        }
    }

    /**
     * Request an interactive verification to begin.
     *
     * This sends out a m.key.verification.request event over to-device messaging to
     * to this device.
     *
     * If no specific device should be verified, but we would like to request
     * verification from all our devices, the
     * [org.matrix.android.sdk.internal.crypto.OwnUserIdentity.requestVerification]
     * method can be used instead.
     */
    @Throws(CryptoStoreException::class)
    suspend fun requestVerification(methods: List<VerificationMethod>): VerificationRequest? {
        val stringMethods = prepareMethods(methods)
        val result = withContext(coroutineDispatchers.io) {
            innerMachine.requestVerificationWithDevice(innerDevice.userId, innerDevice.deviceId, stringMethods)
        }
        return if (result != null) {
            try {
                requestSender.sendVerificationRequest(result.request)
                verificationRequestFactory.create(result.verification)
            } catch (failure: Throwable) {
                // innerMachine.cancelVerification(result.verification.otherUserId, result.verification.flowId, CancelCode.UserError.value)
                null
            }
        } else {
            null
        }
    }

    /**
     * Start an interactive verification with this device.
     *
     * This sends out a m.key.verification.start event with the method set to
     * m.sas.v1 to this device using to-device messaging.
     *
     * This method will soon be deprecated by [MSC3122](https://github.com/matrix-org/matrix-doc/pull/3122).
     * The [requestVerification] method should be used instead.
     *
     */
    @Throws(CryptoStoreException::class)
    suspend fun startVerification(): SasVerification? {
        val result = withContext(coroutineDispatchers.io) {
            innerMachine.startSasWithDevice(innerDevice.userId, innerDevice.deviceId)
        }
        return if (result != null) {
            try {
                requestSender.sendVerificationRequest(result.request)
                sasVerificationFactory.create(result.sas)
            } catch (failure: Throwable) {
                result.sas.cancel(CancelCode.UserError.value)
//                innerMachine.cancelVerification(result.sas.otherUserId, result.sas.flowId, CancelCode.UserError.value)
                null
            }
        } else {
            null
        }
    }

    /**
     * Mark this device as locally trusted.
     *
     * This won't upload any signatures, it will only mark the device as trusted
     * in the local database.
     */
    @Throws(CryptoStoreException::class)
    suspend fun markAsTrusted() {
        withContext(coroutineDispatchers.io) {
            innerMachine.setLocalTrust(innerDevice.userId, innerDevice.deviceId, LocalTrust.VERIFIED)
        }
    }

    /**
     * Manually verify this device.
     *
     * This will sign the device with our self-signing key and upload the signatures
     * to the server.
     *
     * This will fail if the device doesn't belong to use or if we don't have the
     * private part of our self-signing key.
     */
    @Throws(SignatureException::class)
    suspend fun verify(): Boolean {
        val request = withContext(coroutineDispatchers.io) {
            innerMachine.verifyDevice(innerDevice.userId, innerDevice.deviceId)
        }
        requestSender.sendSignatureUpload(request)
        return true
    }

    /**
     * Get the DeviceTrustLevel of this device.
     */
    @Throws(CryptoStoreException::class)
    suspend fun trustLevel(): DeviceTrustLevel {
        refreshData()
        return DeviceTrustLevel(crossSigningVerified = innerDevice.crossSigningTrusted, locallyVerified = innerDevice.locallyTrusted)
    }

    /**
     * Convert this device to a CryptoDeviceInfo.
     *
     * This will not fetch out fresh data from the Rust side.
     **/
    internal fun toCryptoDeviceInfo(): CryptoDeviceInfo {
        return CryptoDeviceInfo(
                deviceId = innerDevice.deviceId,
                userId = innerDevice.userId,
                algorithms = innerDevice.algorithms,
                keys = innerDevice.keys,
                // The Kotlin side doesn't need to care about signatures,
                // so we're not filling this out
                signatures = mapOf(),
                unsigned = UnsignedDeviceInfo(innerDevice.displayName),
                trustLevel = DeviceTrustLevel(
                        crossSigningVerified = innerDevice.crossSigningTrusted,
                        locallyVerified = innerDevice.locallyTrusted
                ),
                isBlocked = innerDevice.isBlocked,
                firstTimeSeenLocalTs = innerDevice.firstTimeSeenTs.toLong()
        )
    }
}
