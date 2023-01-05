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

import androidx.test.filters.LargeTest
import org.amshove.kluent.shouldBe
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.getRoom
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

        val sentMessage = testHelper.sendTextMessage(roomAlicePOV, "you are blocked", 1).first()

        val roomBobPOV = cryptoTestData.secondSession!!.roomService().getRoom(cryptoTestData.roomId)!!
        // ensure other received
        testHelper.retryPeriodically {
            roomBobPOV.timelineService().getTimelineEvent(sentMessage.eventId) != null
        }

        cryptoTestHelper.ensureCannotDecrypt(listOf(sentMessage.eventId), cryptoTestData.secondSession!!, cryptoTestData.roomId)
    }

    @Test
    fun testCanDecryptIfGlobalUnverifiedAndUserTrusted() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        cryptoTestHelper.initializeCrossSigning(cryptoTestData.firstSession)
        cryptoTestHelper.initializeCrossSigning(cryptoTestData.secondSession!!)

        cryptoTestHelper.verifySASCrossSign(cryptoTestData.firstSession, cryptoTestData.secondSession!!, cryptoTestData.roomId)

        cryptoTestData.firstSession.cryptoService().setGlobalBlacklistUnverifiedDevices(true)

        val roomAlicePOV = cryptoTestData.firstSession.roomService().getRoom(cryptoTestData.roomId)!!

        val sentMessage = testHelper.sendTextMessage(roomAlicePOV, "you can read", 1).first()

        val roomBobPOV = cryptoTestData.secondSession!!.roomService().getRoom(cryptoTestData.roomId)!!
        // ensure other received
        testHelper.retryPeriodically {
            roomBobPOV.timelineService().getTimelineEvent(sentMessage.eventId) != null
        }

        cryptoTestHelper.ensureCanDecrypt(
                listOf(sentMessage.eventId),
                cryptoTestData.secondSession!!,
                cryptoTestData.roomId,
                listOf(sentMessage.getLastMessageContent()!!.body)
        )
    }

    @Test
    fun testCantDecryptIfPerRoomUnverified() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        val roomAlicePOV = cryptoTestData.firstSession.roomService().getRoom(cryptoTestData.roomId)!!

        val beforeMessage = testHelper.sendTextMessage(roomAlicePOV, "you can read", 1).first()

        val roomBobPOV = cryptoTestData.secondSession!!.roomService().getRoom(cryptoTestData.roomId)!!
        // ensure other received
        testHelper.retryPeriodically {
            roomBobPOV.timelineService().getTimelineEvent(beforeMessage.eventId) != null
        }

        cryptoTestHelper.ensureCanDecrypt(
                listOf(beforeMessage.eventId),
                cryptoTestData.secondSession!!,
                cryptoTestData.roomId,
                listOf(beforeMessage.getLastMessageContent()!!.body)
        )

        cryptoTestData.firstSession.cryptoService().setRoomBlockUnverifiedDevices(cryptoTestData.roomId, true)

        val afterMessage = testHelper.sendTextMessage(roomAlicePOV, "you are blocked", 1).first()

        // ensure received
        testHelper.retryPeriodically {
            cryptoTestData.secondSession?.getRoom(cryptoTestData.roomId)?.timelineService()?.getTimelineEvent(afterMessage.eventId)?.root != null
        }

        cryptoTestHelper.ensureCannotDecrypt(
                listOf(afterMessage.eventId),
                cryptoTestData.secondSession!!,
                cryptoTestData.roomId,
                MXCryptoError.ErrorType.KEYS_WITHHELD
        )
    }
}
