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

import android.util.Log
import androidx.test.filters.LargeTest
import org.amshove.kluent.shouldBe
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class E2eeConfigTest : InstrumentedTest {

    @Test
    fun testBlacklistUnverifiedDefault() = runCryptoTest(context()) { cryptoTestHelper, _ ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        cryptoTestData.firstSession.cryptoService().getGlobalBlacklistUnverifiedDevices() shouldBe false
        cryptoTestData.firstSession.cryptoService().isRoomBlacklistUnverifiedDevices(cryptoTestData.roomId) shouldBe false
        cryptoTestData.secondSession!!.cryptoService().getGlobalBlacklistUnverifiedDevices() shouldBe false
        cryptoTestData.secondSession!!.cryptoService().isRoomBlacklistUnverifiedDevices(cryptoTestData.roomId) shouldBe false
    }

    @Test
    fun testCantDecryptIfGlobalUnverified() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        cryptoTestData.firstSession.cryptoService().setGlobalBlacklistUnverifiedDevices(true)

        val roomAlicePOV = cryptoTestData.firstSession.roomService().getRoom(cryptoTestData.roomId)!!

        val sentMessage = testHelper.sendMessageInRoom(roomAlicePOV, "you are blocked")

        val roomBobPOV = cryptoTestData.secondSession!!.roomService().getRoom(cryptoTestData.roomId)!!
        // ensure other received
        testHelper.ensureMessage(roomBobPOV, sentMessage) { true }

        cryptoTestHelper.ensureCannotDecrypt(listOf(sentMessage), cryptoTestData.secondSession!!, cryptoTestData.roomId)
    }

    @Test
    fun testCanDecryptIfGlobalUnverifiedAndUserTrusted() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        Log.v("#E2E TEST", "Initializing cross signing for alice and bob...")
        cryptoTestHelper.initializeCrossSigning(cryptoTestData.firstSession)
        cryptoTestHelper.initializeCrossSigning(cryptoTestData.secondSession!!)
        Log.v("#E2E TEST", "... Initialized")

        Log.v("#E2E TEST", "Start User Verification")
        cryptoTestHelper.verifySASCrossSign(cryptoTestData.firstSession, cryptoTestData.secondSession!!, cryptoTestData.roomId)

        cryptoTestData.firstSession.cryptoService().setGlobalBlacklistUnverifiedDevices(true)

        val roomAlicePOV = cryptoTestData.firstSession.roomService().getRoom(cryptoTestData.roomId)!!

        Log.v("#E2E TEST", "Send message in room")
        val sentMessage = testHelper.sendMessageInRoom(roomAlicePOV, "you can read")

        val roomBobPOV = cryptoTestData.secondSession!!.roomService().getRoom(cryptoTestData.roomId)!!
        // ensure other received

        testHelper.ensureMessage(roomBobPOV, sentMessage) { true }

        cryptoTestHelper.ensureCanDecrypt(
                listOf(sentMessage),
                cryptoTestData.secondSession!!,
                cryptoTestData.roomId,
                listOf(
                        roomBobPOV.timelineService().getTimelineEvent(sentMessage)?.getLastMessageContent()!!.body
                )
        )
    }

    @Test
    fun testCantDecryptIfPerRoomUnverified() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        val roomAlicePOV = cryptoTestData.firstSession.roomService().getRoom(cryptoTestData.roomId)!!

        val beforeMessage =  testHelper.sendMessageInRoom(roomAlicePOV, "you can read")

        val roomBobPOV = cryptoTestData.secondSession!!.roomService().getRoom(cryptoTestData.roomId)!!
        // ensure other received
        Log.v("#E2E TEST", "Wait for bob to get the message")
        testHelper.ensureMessage(roomBobPOV, beforeMessage) { true }

        Log.v("#E2E TEST", "ensure bob Can Decrypt first message")
        cryptoTestHelper.ensureCanDecrypt(
                listOf(beforeMessage),
                cryptoTestData.secondSession!!,
                cryptoTestData.roomId,
                listOf("you can read")
        )

        Log.v("#E2E TEST", "setRoomBlockUnverifiedDevices true")
        cryptoTestData.firstSession.cryptoService().setRoomBlockUnverifiedDevices(cryptoTestData.roomId, true)

        Log.v("#E2E TEST", "let alice send the message")
        val afterMessage = testHelper.sendMessageInRoom(roomAlicePOV, "you are blocked")

        // ensure received

        Log.v("#E2E TEST", "Ensure bob received second message")
        testHelper.ensureMessage(roomBobPOV, afterMessage) { true }

        cryptoTestHelper.ensureCannotDecrypt(
                listOf(afterMessage),
                cryptoTestData.secondSession!!,
                cryptoTestData.roomId,
                MXCryptoError.ErrorType.KEYS_WITHHELD
        )
    }
}
