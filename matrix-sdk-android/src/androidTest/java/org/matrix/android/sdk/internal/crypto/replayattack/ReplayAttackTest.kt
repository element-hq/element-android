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

package org.matrix.android.sdk.internal.crypto.replayattack

import androidx.test.filters.LargeTest
import org.amshove.kluent.internal.assertFailsWith
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class ReplayAttackTest : InstrumentedTest {

    @Test
    fun replayAttackAlreadyDecryptedEventTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        val e2eRoomID = cryptoTestData.roomId

        // Alice
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomPOV = aliceSession.roomService().getRoom(e2eRoomID)!!

        // Bob
        val bobSession = cryptoTestData.secondSession
        val bobRoomPOV = bobSession!!.roomService().getRoom(e2eRoomID)!!
        assertEquals(bobRoomPOV.roomSummary()?.joinedMembersCount, 2)

        // Alice will send a message
        val sentEvents = testHelper.sendTextMessageSuspending(aliceRoomPOV, "Hello I will be decrypted twice", 1)
        assertEquals(1, sentEvents.size)

        val fakeEventId = sentEvents[0].eventId + "_fake"
        val fakeEventWithTheSameIndex =
                sentEvents[0].copy(eventId = fakeEventId, root = sentEvents[0].root.copy(eventId = fakeEventId))

        // Lets assume we are from the main timelineId
        val timelineId = "timelineId"
        // Lets decrypt the original event
        aliceSession.cryptoService().decryptEvent(sentEvents[0].root, timelineId)
        // Lets decrypt the fake event that will have the same message index
        val exception = assertFailsWith<MXCryptoError.Base> {
            // An exception should be thrown while the same index would have been used for the previous decryption
            aliceSession.cryptoService().decryptEvent(fakeEventWithTheSameIndex.root, timelineId)
        }
        assertEquals(MXCryptoError.ErrorType.DUPLICATED_MESSAGE_INDEX, exception.errorType)
        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    fun replayAttackSameEventTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        val e2eRoomID = cryptoTestData.roomId

        // Alice
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomPOV = aliceSession.roomService().getRoom(e2eRoomID)!!

        // Bob
        val bobSession = cryptoTestData.secondSession
        val bobRoomPOV = bobSession!!.roomService().getRoom(e2eRoomID)!!
        assertEquals(bobRoomPOV.roomSummary()?.joinedMembersCount, 2)

        // Alice will send a message
        val sentEvents = testHelper.sendTextMessageSuspending(aliceRoomPOV, "Hello I will be decrypted twice", 1)
        Assert.assertTrue("Message should be sent", sentEvents.size == 1)
        assertEquals(sentEvents.size, 1)

        // Lets assume we are from the main timelineId
        val timelineId = "timelineId"
        // Lets decrypt the original event
        aliceSession.cryptoService().decryptEvent(sentEvents[0].root, timelineId)
        try {
            // Lets try to decrypt the same event
            aliceSession.cryptoService().decryptEvent(sentEvents[0].root, timelineId)
        } catch (ex: Throwable) {
            fail("Shouldn't throw a decryption error for same event")
        }
    }
}
