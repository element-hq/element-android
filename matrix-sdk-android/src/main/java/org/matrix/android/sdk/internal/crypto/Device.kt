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

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import uniffi.olm.CryptoStoreErrorException
import uniffi.olm.Device as InnerDevice
import uniffi.olm.OlmMachine

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
    /** Start an interactive verification with this device
     *
     * This sends out a m.key.verification.start event with the method set to
     * m.sas.v1 to this device using to-device messaging.
     */
    // TODO this has been deprecated in the spec, add a requestVerification() method
    // to this class and use that one instead
    @Throws(CryptoStoreErrorException::class)
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

    /** Mark this device as locally trusted
     *
     * This won't upload any signatures, it will only mark the device as trusted
     * in the local database.
     */
    @Throws(CryptoStoreErrorException::class)
    suspend fun markAsTrusted() {
        withContext(Dispatchers.IO) {
            machine.markDeviceAsTrusted(inner.userId, inner.deviceId)
        }
    }
}
