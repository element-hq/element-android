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
import androidx.lifecycle.Observer
import androidx.test.filters.LargeTest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class RoomShieldTest : InstrumentedTest {

    @Test
    fun testShieldNoVerification() = CommonTestHelper.runCryptoTest(context()) { cryptoTestHelper, _ ->
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val roomId = testData.roomId

        cryptoTestHelper.initializeCrossSigning(testData.firstSession)
        cryptoTestHelper.initializeCrossSigning(testData.secondSession!!)

        // Test are flaky unless I use liveData observer on main thread
        // Just calling getRoomSummary() with retryWithBackOff keeps an outdated version of the value
        testData.firstSession.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Default)
        testData.secondSession!!.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Default)
    }

    @Test
    fun testShieldInOneOne() = CommonTestHelper.runLongCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val roomId = testData.roomId

        Log.v("#E2E TEST", "Initialize cross signing...")
        cryptoTestHelper.initializeCrossSigning(testData.firstSession)
        cryptoTestHelper.initializeCrossSigning(testData.secondSession!!)
        Log.v("#E2E TEST", "... Initialized.")

        // let alive and bob verify
        Log.v("#E2E TEST", "Alice and Bob verify each others...")
        cryptoTestHelper.verifySASCrossSign(testData.firstSession, testData.secondSession!!, testData.roomId)

        // Add a new session for bob
        // This session will be unverified for now

        Log.v("#E2E TEST", "Log in a new bob device...")
        val bobSecondSession = testHelper.logIntoAccount(testData.secondSession!!.myUserId, SessionTestParams(true))

        Log.v("#E2E TEST", "Bob session logged in ${bobSecondSession.myUserId.take(6)}")

        Log.v("#E2E TEST", "Assert room shields...")
        testData.firstSession.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Warning)
        // in 1:1 we ignore our own status
        testData.secondSession!!.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Trusted)

        // Adding another user should make bob consider his devices now and see same shield as alice
        Log.v("#E2E TEST", "Create Sam account")
        val samSession = testHelper.createAccount(TestConstants.USER_SAM, SessionTestParams(withInitialSync = true))

        // Let alice invite sam
        Log.v("#E2E TEST", "Let alice invite sam")
        testData.firstSession.getRoom(roomId)!!.membershipService().invite(samSession.myUserId)
        testHelper.waitForAndAcceptInviteInRoom(samSession, roomId)

        Log.v("#E2E TEST", "Assert room shields...")
        testData.firstSession.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Warning)
        testData.secondSession!!.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Warning)

        // Now let's bob verify his session

        Log.v("#E2E TEST", "Bob verifies his new session")
        cryptoTestHelper.verifyNewSession(testData.secondSession!!, bobSecondSession)

        testData.firstSession.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Trusted)
        testData.secondSession!!.assertRoomShieldIs(roomId, RoomEncryptionTrustLevel.Trusted)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun Session.assertRoomShieldIs(roomId: String, state: RoomEncryptionTrustLevel?) {
        val lock = CountDownLatch(1)
        val roomLiveData = withContext(Dispatchers.Main) {
            roomService().getRoomSummaryLive(roomId)
        }
        val observer = object : Observer<Optional<RoomSummary>> {
            override fun onChanged(value: Optional<RoomSummary>) {
                Log.v("#E2E TEST ${this@assertRoomShieldIs.myUserId.take(6)}", "Shield Update ${value.getOrNull()?.roomEncryptionTrustLevel}")
                if (value.getOrNull()?.roomEncryptionTrustLevel == state) {
                    lock.countDown()
                    roomLiveData.removeObserver(this)
                }
            }
        }
        GlobalScope.launch(Dispatchers.Main) { roomLiveData.observeForever(observer) }

        lock.await(40_000, TimeUnit.MILLISECONDS)
    }
}
