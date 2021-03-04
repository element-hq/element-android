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

package org.matrix.android.sdk.internal

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.MXEventDecryptionResult
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.session.sync.model.DeviceListResponse
import org.matrix.android.sdk.internal.session.sync.model.DeviceOneTimeKeysCountSyncResponse
import org.matrix.android.sdk.internal.session.sync.model.ToDeviceSyncResponse
import timber.log.Timber
import uniffi.olm.Device as InnerDevice
import uniffi.olm.DeviceLists
import uniffi.olm.Logger
import uniffi.olm.OlmMachine as InnerMachine
import uniffi.olm.Request
import uniffi.olm.RequestType
import uniffi.olm.Sas as InnerSas
import uniffi.olm.setLogger

class CryptoLogger() : Logger {
    override fun log(logLine: String) {
        Timber.d(logLine)
    }
}

fun setRustLogger() {
    setLogger(CryptoLogger() as Logger)
}

class Device(inner: InnerDevice, machine: InnerMachine) {
    private val machine: InnerMachine = machine
    private val inner: InnerDevice = inner

    fun userId(): String {
        return this.inner.userId
    }

    fun deviceId(): String {
        return this.inner.deviceId
    }

    fun keys(): Map<String, String> {
        return this.inner.keys
    }

    fun startVerification(): InnerSas {
        return this.machine.startVerification(this.inner)
    }
}

internal class OlmMachine(user_id: String, device_id: String, path: File) {
    private val inner: InnerMachine = InnerMachine(user_id, device_id, path.toString())

    fun userId(): String {
        return this.inner.userId()
    }

    fun deviceId(): String {
        return this.inner.deviceId()
    }

    fun identityKeys(): Map<String, String> {
        return this.inner.identityKeys()
    }

    suspend fun outgoingRequests(): List<Request> = withContext(Dispatchers.IO) {
        inner.outgoingRequests()
    }

    suspend fun updateTrackedUsers(users: List<String>) = withContext(Dispatchers.IO) {
        inner.updateTrackedUsers(users)
    }

    suspend fun receiveSyncChanges(
        toDevice: ToDeviceSyncResponse?,
        deviceChanges: DeviceListResponse?,
        keyCounts: DeviceOneTimeKeysCountSyncResponse?
    ) = withContext(Dispatchers.IO) {
            var counts: MutableMap<String, Int> = mutableMapOf()

            if (keyCounts?.signedCurve25519 != null) {
                counts.put("signed_curve25519", keyCounts.signedCurve25519)
            }

            val devices = DeviceLists(deviceChanges?.changed ?: listOf(), deviceChanges?.left ?: listOf())
            val adapter = MoshiProvider.providesMoshi().adapter<ToDeviceSyncResponse>(ToDeviceSyncResponse::class.java)
            val events = adapter.toJson(toDevice ?: ToDeviceSyncResponse())!!

            inner.receiveSyncChanges(events, devices, counts)
    }

    suspend fun markRequestAsSent(
        request_id: String,
        request_type: RequestType,
        response_body: String
    ) = withContext(Dispatchers.IO) {
        inner.markRequestAsSent(request_id, request_type, response_body)
    }

    suspend fun getDevice(user_id: String, device_id: String): Device? = withContext(Dispatchers.IO) {
        when (val device: InnerDevice? = inner.getDevice(user_id, device_id)) {
            null -> null
            else -> Device(device, inner)
        }
    }

    @Throws(MXCryptoError::class)
    suspend fun decryptRoomEvent(event: Event): MXEventDecryptionResult = withContext(Dispatchers.IO) {
        val adapter = MoshiProvider.providesMoshi().adapter<Event>(Event::class.java)
        val serializedEvent = adapter.toJson(event)

        try {
            val decrypted = inner.decryptRoomEvent(serializedEvent, event.roomId!!)

            val deserializationAdapter = MoshiProvider.providesMoshi().adapter<JsonDict>(Map::class.java)
            val clearEvent = deserializationAdapter.fromJson(decrypted.clearEvent)!!

            MXEventDecryptionResult(
                clearEvent,
                decrypted.senderCurve25519Key,
                decrypted.claimedEd25519Key,
                decrypted.forwardingCurve25519Chain
            )
        } catch (throwable: Throwable) {
            val reason = String.format(MXCryptoError.UNABLE_TO_DECRYPT_REASON, throwable.message, "m.megolm.v1.aes-sha2")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT, reason)
        }
    }
}
