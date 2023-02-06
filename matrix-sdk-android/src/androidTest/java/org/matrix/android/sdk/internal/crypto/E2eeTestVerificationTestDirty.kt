/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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
import junit.framework.TestCase.fail
import kotlinx.coroutines.delay
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.MessageVerificationState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class E2eeTestVerificationTestDirty : InstrumentedTest {

    @Test
    fun testVerificationStateRefreshedAfterKeyDownload() = CommonTestHelper.runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        // We are going to setup a second session for bob that will send a message while alice session
        // has stopped syncing.

        aliceSession.syncService().stopSync()
        aliceSession.syncService().stopAnyBackgroundSync()
        // wait a bit for session to be really closed
        delay(1_000)

        Log.v("#E2E TEST", "Create a new session for Bob")
        val newBobSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        Log.v("#E2E TEST", "New bob session will send a message")
        val eventId = testHelper.sendMessageInRoom(newBobSession.getRoom(e2eRoomID)!!, "I am unknown")

        aliceSession.syncService().startSync(true)

        // Check without starting a timeline so that it doesn't update itself
        testHelper.retryWithBackoff(
                onFail = {
                    fail("${aliceSession.myUserId.take(10)} should not have downloaded the device at time of decryption")
                }) {
            val timeLineEvent = aliceSession.getRoom(e2eRoomID)?.getTimelineEvent(eventId).also {
                Log.v("#E2E TEST", "Verification state is ${it?.root?.mxDecryptionResult?.verificationState}")
            }
            timeLineEvent != null &&
                    timeLineEvent.isEncrypted() &&
                    timeLineEvent.root.getClearType() == EventType.MESSAGE &&
                    timeLineEvent.root.mxDecryptionResult?.verificationState == MessageVerificationState.UNKNOWN_DEVICE
        }

        // After key download it should be dirty (that will happen after sync completed)
        testHelper.retryWithBackoff(
                onFail = {
                    fail("${aliceSession.myUserId.take(10)} should be dirty")
                }) {
            val timeLineEvent = aliceSession.getRoom(e2eRoomID)?.getTimelineEvent(eventId).also {
                Log.v("#E2E TEST", "Is verification state dirty ${it?.root?.verificationStateIsDirty}")
            }
            timeLineEvent?.root?.verificationStateIsDirty.orFalse()
        }

        Log.v("#E2E TEST", "Start timeline and check that verification state is updated")
        // eventually should be marked as dirty then have correct state when a timeline is started
        testHelper.ensureMessage(aliceSession.getRoom(e2eRoomID)!!, eventId) {
            it.isEncrypted() &&
                    it.root.getClearType() == EventType.MESSAGE &&
                    it.root.mxDecryptionResult?.verificationState == MessageVerificationState.UN_SIGNED_DEVICE
        }
    }
}
