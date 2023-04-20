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

package org.matrix.android.sdk.internal.crypto

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DevicesListResponse
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.DeleteDeviceParams
import org.matrix.android.sdk.internal.crypto.model.rest.DeleteDevicesParams
import org.matrix.android.sdk.internal.crypto.model.rest.KeyChangesResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysClaimBody
import org.matrix.android.sdk.internal.crypto.model.rest.KeysClaimResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysQueryBody
import org.matrix.android.sdk.internal.crypto.model.rest.KeysQueryResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysUploadResponse
import org.matrix.android.sdk.internal.crypto.model.rest.SendToDeviceBody
import org.matrix.android.sdk.internal.crypto.model.rest.SignatureUploadResponse
import org.matrix.android.sdk.internal.crypto.model.rest.UpdateDeviceInfoBody
import org.matrix.android.sdk.internal.crypto.model.rest.UploadSigningKeysBody
import org.matrix.android.sdk.internal.crypto.tasks.DefaultSendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask

class DefaultSendToDeviceTaskTest {

    private val users = listOf(
            "@alice:example.com" to listOf("D0", "D1"),
            "bob@example.com" to listOf("D2", "D3")
    )

    private val fakeEncryptedContent = mapOf(
            "algorithm" to "m.olm.v1.curve25519-aes-sha2",
            "sender_key" to "gMObR+/4dqL5T4DisRRRYBJpn+OjzFnkyCFOktP6Eyw",
            "ciphertext" to mapOf(
                    "tdwXf7006FDgzmufMCVI4rDdVPO51ecRTTT6HkRxUwE" to mapOf(
                            "type" to 0,
                            "body" to "AwogCA1ULEc0abGIFxMDIC9iv7ul3jqJSnapTHQ+8JJx"
                    )
            )
    )

    private val fakeStartVerificationContent = mapOf(
            "method" to "m.sas.v1",
            "from_device" to "MNQHVEISFQ",
            "key_agreement_protocols" to listOf(
                    "curve25519-hkdf-sha256",
                    "curve25519"
            ),
            "hashes" to listOf("sha256"),
            "message_authentication_codes" to listOf(
                    "org.matrix.msc3783.hkdf-hmac-sha256",
                    "hkdf-hmac-sha256",
                    "hmac-sha256"
            ),
            "short_authentication_string" to listOf(
                    "decimal",
                    "emoji"
            ),
            "transaction_id" to "4wNOpkHGwGZPXjkZToooCDWfb8hsf7vW"
    )

    @Test
    fun `tracing id should be added to to_device contents`() {
        val fakeCryptoAPi = FakeCryptoApi()

        val sendToDeviceTask = DefaultSendToDeviceTask(
                cryptoApi = fakeCryptoAPi,
                globalErrorReceiver = mockk(relaxed = true)
        )

        val contentMap = MXUsersDevicesMap<Any>()

        users.forEach { pairOfUserDevices ->
            val userId = pairOfUserDevices.first
            pairOfUserDevices.second.forEach {
                contentMap.setObject(userId, it, fakeEncryptedContent)
            }
        }

        val params = SendToDeviceTask.Params(
                eventType = EventType.ENCRYPTED,
                contentMap = contentMap
        )

        runBlocking {
            sendToDeviceTask.execute(params)
        }

        val generatedIds = mutableListOf<String>()
        users.forEach { pairOfUserDevices ->
            val userId = pairOfUserDevices.first
            pairOfUserDevices.second.forEach {
                val modifiedContent = fakeCryptoAPi.body!!.messages!![userId]!![it] as Map<*, *>
                Assert.assertNotNull("Tracing id should have been added", modifiedContent["org.matrix.msgid"])
                generatedIds.add(modifiedContent["org.matrix.msgid"] as String)

                assertEquals(
                        "The rest of the content should be the same",
                        fakeEncryptedContent.keys,
                        modifiedContent.toMutableMap().apply { remove("org.matrix.msgid") }.keys
                )
            }
        }

        assertEquals("Id should be unique per content", generatedIds.size, generatedIds.toSet().size)
        println("modified content ${fakeCryptoAPi.body}")
    }

    @Test
    fun `tracing id should not be added to verification start to_device contents`() {
        val fakeCryptoAPi = FakeCryptoApi()

        val sendToDeviceTask = DefaultSendToDeviceTask(
                cryptoApi = fakeCryptoAPi,
                globalErrorReceiver = mockk(relaxed = true)
        )
        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject("@alice:example.com", "MNQHVEISFQ", fakeStartVerificationContent)

        val params = SendToDeviceTask.Params(
                eventType = EventType.KEY_VERIFICATION_START,
                contentMap = contentMap
        )

        runBlocking {
            sendToDeviceTask.execute(params)
        }

        val modifiedContent = fakeCryptoAPi.body!!.messages!!["@alice:example.com"]!!["MNQHVEISFQ"] as Map<*, *>
        Assert.assertNull("Tracing id should not have been added", modifiedContent["org.matrix.msgid"])

        // try to force
        runBlocking {
            sendToDeviceTask.execute(
                    SendToDeviceTask.Params(
                            eventType = EventType.KEY_VERIFICATION_START,
                            contentMap = contentMap,
                            addTracingIds = true
                    )
            )
        }

        val modifiedContentForced = fakeCryptoAPi.body!!.messages!!["@alice:example.com"]!!["MNQHVEISFQ"] as Map<*, *>
        Assert.assertNotNull("Tracing id should have been added", modifiedContentForced["org.matrix.msgid"])
    }

    @Test
    fun `tracing id should not be added to all verification to_device contents`() {
        val fakeCryptoAPi = FakeCryptoApi()

        val sendToDeviceTask = DefaultSendToDeviceTask(
                cryptoApi = fakeCryptoAPi,
                globalErrorReceiver = mockk(relaxed = true)
        )
        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject("@alice:example.com", "MNQHVEISFQ", emptyMap<String, Any>())

        val verificationEvents = listOf(
                MessageType.MSGTYPE_VERIFICATION_REQUEST,
                EventType.KEY_VERIFICATION_START,
                EventType.KEY_VERIFICATION_ACCEPT,
                EventType.KEY_VERIFICATION_KEY,
                EventType.KEY_VERIFICATION_MAC,
                EventType.KEY_VERIFICATION_CANCEL,
                EventType.KEY_VERIFICATION_DONE,
                EventType.KEY_VERIFICATION_READY
        )

        for (type in verificationEvents) {
            val params = SendToDeviceTask.Params(
                    eventType = type,
                    contentMap = contentMap
            )
            runBlocking {
                sendToDeviceTask.execute(params)
            }

            val modifiedContent = fakeCryptoAPi.body!!.messages!!["@alice:example.com"]!!["MNQHVEISFQ"] as Map<*, *>
            Assert.assertNull("Tracing id should not have been added", modifiedContent["org.matrix.msgid"])
        }
    }

    internal class FakeCryptoApi : CryptoApi {
        override suspend fun getDevices(): DevicesListResponse {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun getDeviceInfo(deviceId: String): DeviceInfo {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun uploadKeys(body: JsonDict): KeysUploadResponse {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun downloadKeysForUsers(params: KeysQueryBody): KeysQueryResponse {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun uploadSigningKeys(params: UploadSigningKeysBody): KeysQueryResponse {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun uploadSignatures(params: Map<String, Any>?): SignatureUploadResponse {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun claimOneTimeKeysForUsersDevices(body: KeysClaimBody): KeysClaimResponse {
            throw java.lang.AssertionError("Should not be called")
        }

        var body: SendToDeviceBody? = null
        override suspend fun sendToDevice(eventType: String, transactionId: String, body: SendToDeviceBody) {
            this.body = body
        }

        override suspend fun deleteDevice(deviceId: String, params: DeleteDeviceParams) {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun deleteDevices(params: DeleteDevicesParams) {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun updateDeviceInfo(deviceId: String, params: UpdateDeviceInfoBody) {
            throw java.lang.AssertionError("Should not be called")
        }

        override suspend fun getKeyChanges(oldToken: String, newToken: String): KeyChangesResponse {
            throw java.lang.AssertionError("Should not be called")
        }
    }
}
