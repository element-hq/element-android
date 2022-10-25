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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.ForwardedRoomKeyContent
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.OlmEventContent
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.UnRequestedForwardManager

class UnRequestedKeysManagerTest {

    private val aliceMxId = "alice@example.com"
    private val bobMxId = "bob@example.com"
    private val bobDeviceId = "MKRJDSLYGA"

    private val device1Id = "MGDAADVDMG"

    private val aliceFirstDevice = CryptoDeviceInfo(
            deviceId = device1Id,
            userId = aliceMxId,
            algorithms = MXCryptoAlgorithms.supportedAlgorithms(),
            keys = mapOf(
                    "curve25519:$device1Id" to "yDa6cWOZ/WGBqm/JMUfTUCdEbAIzKHhuIcdDbnPEhDU",
                    "ed25519:$device1Id" to "XTge+TDwfm+WW10IGnaqEyLTSukPPzg3R1J1YvO1SBI",
            ),
            signatures = mapOf(
                    aliceMxId to mapOf(
                            "ed25519:$device1Id"
                                    to "bPOAqM40+QSMgeEzUbYbPSZZccDDMUG00lCNdSXCoaS1gKKBGkSEaHO1OcibISIabjLYzmhp9mgtivz32fbABQ",
                            "ed25519:Ru4ni66dbQ6FZgUoHyyBtmjKecOHMvMSsSBZ2SABtt0"
                                    to "owzUsQ4Pvn35uEIc5FdVnXVRPzsVYBV8uJRUSqr4y8r5tp0DvrMArtJukKETgYEAivcZMT1lwNihHIN9xh06DA"
                    )
            ),
            unsigned = UnsignedDeviceInfo(deviceDisplayName = "Element Web"),
            trustLevel = DeviceTrustLevel(crossSigningVerified = true, locallyVerified = true)
    )

    private val aBobDevice = CryptoDeviceInfo(
            deviceId = bobDeviceId,
            userId = bobMxId,
            algorithms = MXCryptoAlgorithms.supportedAlgorithms(),
            keys = mapOf(
                    "curve25519:$bobDeviceId" to "tWwg63Yfn//61Ylhir6z4QGejvo193E6MVHmURtYVn0",
                    "ed25519:$bobDeviceId" to "pS5NJ1LiVksQFX+p58NlphqMxE705laRVtUtZpYIAfs",
            ),
            signatures = mapOf(
                    bobMxId to mapOf(
                            "ed25519:$bobDeviceId" to "zAJqsmOSzkx8EWXcrynCsWtbgWZifN7A6DLyEBs+ZPPLnNuPN5Jwzc1Rg+oZWZaRPvOPcSL0cgcxRegSBU0NBA",
                    )
            ),
            unsigned = UnsignedDeviceInfo(deviceDisplayName = "Element Ios")
    )

    @Test
    fun `test process key request if invite received`() {
        val fakeDeviceListManager = mockk<DeviceListManager> {
            coEvery { downloadKeys(any(), any()) } returns MXUsersDevicesMap<CryptoDeviceInfo>().apply {
                setObject(bobMxId, bobDeviceId, aBobDevice)
            }
        }
        val unrequestedForwardManager = UnRequestedForwardManager(fakeDeviceListManager)

        val roomId = "someRoomId"

        unrequestedForwardManager.onUnRequestedKeyForward(
                roomId,
                createFakeSuccessfullyDecryptedForwardToDevice(
                        aBobDevice,
                        aliceFirstDevice,
                        aBobDevice,
                        megolmSessionId = "megolmId1"
                ),
                1_000
        )

        unrequestedForwardManager.onUnRequestedKeyForward(
                roomId,
                createFakeSuccessfullyDecryptedForwardToDevice(
                        aBobDevice,
                        aliceFirstDevice,
                        aBobDevice,
                        megolmSessionId = "megolmId2"
                ),
                1_000
        )
        // for now no reason to accept
        runBlocking {
            unrequestedForwardManager.postSyncProcessParkedKeysIfNeeded(1000) {
                fail("There should be no key to process")
            }
        }

        // ACT
        // suppose an invite is received but from another user
        val inviteTime = 1_000L
        unrequestedForwardManager.onInviteReceived(roomId, "@jhon:example.com", inviteTime)

        // we shouldn't process the requests!
//        runBlocking {
            unrequestedForwardManager.postSyncProcessParkedKeysIfNeeded(inviteTime) {
                fail("There should be no key to process")
            }
//        }

        // ACT
        // suppose an invite is received from correct user

        unrequestedForwardManager.onInviteReceived(roomId, aBobDevice.userId, inviteTime)
        runBlocking {
            unrequestedForwardManager.postSyncProcessParkedKeysIfNeeded(inviteTime) {
                it.size shouldBe 2
            }
        }
    }

    @Test
    fun `test invite before keys`() {
        val fakeDeviceListManager = mockk<DeviceListManager> {
            coEvery { downloadKeys(any(), any()) } returns MXUsersDevicesMap<CryptoDeviceInfo>().apply {
                setObject(bobMxId, bobDeviceId, aBobDevice)
            }
        }
        val unrequestedForwardManager = UnRequestedForwardManager(fakeDeviceListManager)

        val roomId = "someRoomId"

        unrequestedForwardManager.onInviteReceived(roomId, aBobDevice.userId, 1_000)

        unrequestedForwardManager.onUnRequestedKeyForward(
                roomId,
                createFakeSuccessfullyDecryptedForwardToDevice(
                        aBobDevice,
                        aliceFirstDevice,
                        aBobDevice,
                        megolmSessionId = "megolmId1"
                ),
                1_000
        )

        runBlocking {
            unrequestedForwardManager.postSyncProcessParkedKeysIfNeeded(1000) {
                it.size shouldBe 1
            }
        }
    }

    @Test
    fun `test validity window`() {
        val fakeDeviceListManager = mockk<DeviceListManager> {
            coEvery { downloadKeys(any(), any()) } returns MXUsersDevicesMap<CryptoDeviceInfo>().apply {
                setObject(bobMxId, bobDeviceId, aBobDevice)
            }
        }
        val unrequestedForwardManager = UnRequestedForwardManager(fakeDeviceListManager)

        val roomId = "someRoomId"

        val timeOfKeyReception = 1_000L

        unrequestedForwardManager.onUnRequestedKeyForward(
                roomId,
                createFakeSuccessfullyDecryptedForwardToDevice(
                        aBobDevice,
                        aliceFirstDevice,
                        aBobDevice,
                        megolmSessionId = "megolmId1"
                ),
                timeOfKeyReception
        )

        val currentTimeWindow = 10 * 60_000

        // simulate very late invite
        val inviteTime = timeOfKeyReception + currentTimeWindow + 1_000
        unrequestedForwardManager.onInviteReceived(roomId, aBobDevice.userId, inviteTime)

        runBlocking {
            unrequestedForwardManager.postSyncProcessParkedKeysIfNeeded(inviteTime) {
                fail("There should be no key to process")
            }
        }
    }

    private fun createFakeSuccessfullyDecryptedForwardToDevice(
            sentBy: CryptoDeviceInfo,
            dest: CryptoDeviceInfo,
            sessionInitiator: CryptoDeviceInfo,
            algorithm: String = MXCRYPTO_ALGORITHM_MEGOLM,
            roomId: String = "!zzgDlIhbWOevcdFBXr:example.com",
            megolmSessionId: String = "Z/FSE8wDYheouGjGP9pezC4S1i39RtAXM3q9VXrBVZw"
    ): Event {
        return Event(
                type = EventType.ENCRYPTED,
                eventId = "!fake",
                senderId = sentBy.userId,
                content = OlmEventContent(
                        ciphertext = mapOf(
                                dest.identityKey()!! to mapOf(
                                        "type" to 0,
                                        "body" to "AwogcziNF/tv60X0elsBmnKPN3+LTXr4K3vXw+1ZJ6jpTxESIJCmMMDvOA+"
                                )
                        ),
                        senderKey = sentBy.identityKey()
                ).toContent(),

                ).apply {
            mxDecryptionResult = OlmDecryptionResult(
                    payload = mapOf(
                            "type" to EventType.FORWARDED_ROOM_KEY,
                            "content" to ForwardedRoomKeyContent(
                                    algorithm = algorithm,
                                    roomId = roomId,
                                    senderKey = sessionInitiator.identityKey(),
                                    sessionId = megolmSessionId,
                                    sessionKey = "AQAAAAAc4dK+lXxXyaFbckSxwjIEoIGDLKYovONJ7viWpwevhfvoBh+Q..."
                            ).toContent()
                    ),
                    senderKey = sentBy.identityKey()
            )
        }
    }
}
