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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.amshove.kluent.fail
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class DecryptRedactedEventTest : InstrumentedTest {

    @Test
    fun doNotFailToDecryptRedactedEvent() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)

        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val e2eRoomID = testData.roomId
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!

        val roomALicePOV = aliceSession.getRoom(e2eRoomID)!!
        val timelineEvent = testHelper.sendTextMessage(roomALicePOV, "Hello", 1).first()
        roomALicePOV.sendService().redactEvent(timelineEvent.root, "Wrong Room")

        // get the event from bob
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                bobSession.getRoom(e2eRoomID)?.getTimelineEvent(timelineEvent.eventId)?.root?.isRedacted() == true
            }
        }

        val eventBobPov = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(timelineEvent.eventId)!!

        testHelper.runBlockingTest {
            try {
                bobSession.cryptoService().decryptEvent(eventBobPov.root, "")
            } catch (failure: Throwable) {
                fail("Should not throw when decrypting a redacted event")
            }
        }
    }
}
