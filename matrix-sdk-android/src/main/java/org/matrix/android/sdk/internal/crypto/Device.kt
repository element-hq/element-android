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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.rest.UnsignedDeviceInfo
import org.matrix.android.sdk.internal.crypto.verification.prepareMethods
import uniffi.olm.CryptoStoreException
import uniffi.olm.Device as InnerDevice
import uniffi.olm.OlmMachine
import uniffi.olm.SignatureException
import uniffi.olm.VerificationRequest

/** Class representing a device that supports E2EE in the Matrix world
 *
 * This class can be used to directly start a verification flow with the device
 * or to manually verify the device.
 */
internal class Device(
        private val machine: OlmMachine,
        private var inner: InnerDevice,
        private val sender: RequestSender,
        private val listeners: ArrayList<VerificationService.Listener>,
) {
    @Throws(CryptoStoreException::class)
    private suspend fun refreshData() {
        val device = withContext(Dispatchers.IO) {
            machine.getDevice(inner.userId, inner.deviceId)
        }

        if (device != null) {
            this.inner = device
        }
    }

    /**
     * Request an interactive verification to begin
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
        val result = withContext(Dispatchers.IO) {
            machine.requestVerificationWithDevice(inner.userId, inner.deviceId, stringMethods)
        }

        return if (result != null) {
            this.sender.sendVerificationRequest(result.request)
            result.verification
        } else {
            null
        }
    }

    /** Start an interactive verification with this device
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
        val result = withContext(Dispatchers.IO) {
            machine.startSasWithDevice(inner.userId, inner.deviceId)
        }

        return if (result != null) {
            this.sender.sendVerificationRequest(result.request)
            SasVerification(
                    this.machine, result.sas, this.sender, this.listeners,
            )
        } else {
            null
        }
    }

    /**
     * Mark this device as locally trusted
     *
     * This won't upload any signatures, it will only mark the device as trusted
     * in the local database.
     */
    @Throws(CryptoStoreException::class)
    suspend fun markAsTrusted() {
        withContext(Dispatchers.IO) {
            machine.markDeviceAsTrusted(inner.userId, inner.deviceId)
        }
    }

    /**
     * Manually verify this device
     *
     * This will sign the device with our self-signing key and upload the signatures
     * to the server.
     *
     * This will fail if the device doesn't belong to use or if we don't have the
     * private part of our self-signing key.
     */
    @Throws(SignatureException::class)
    suspend fun verify(): Boolean {
        val request = withContext(Dispatchers.IO) {
            machine.verifyDevice(inner.userId, inner.deviceId)
        }

        this.sender.sendSignatureUpload(request)

        return true
    }

    /**
     * Get the DeviceTrustLevel of this device
     */
    @Throws(CryptoStoreException::class)
    suspend fun trustLevel(): DeviceTrustLevel {
        refreshData()
        return DeviceTrustLevel(crossSigningVerified = inner.crossSigningTrusted, locallyVerified = inner.locallyTrusted)
    }

    /**
     * Convert this device to a CryptoDeviceInfo.
     *
     * This will not fetch out fresh data from the Rust side.
     **/
    internal fun toCryptoDeviceInfo(): CryptoDeviceInfo {
        val keys = this.inner.keys.map { (keyId, key) -> "$keyId:$this.inner.deviceId" to key }.toMap()

        return CryptoDeviceInfo(
                this.inner.deviceId,
                this.inner.userId,
                this.inner.algorithms,
                keys,
                // The Kotlin side doesn't need to care about signatures,
                // so we're not filling this out
                mapOf(),
                UnsignedDeviceInfo(this.inner.displayName),
                DeviceTrustLevel(crossSigningVerified = this.inner.crossSigningTrusted, locallyVerified = this.inner.locallyTrusted),
                this.inner.isBlocked,
                // TODO
                null)
    }
}
