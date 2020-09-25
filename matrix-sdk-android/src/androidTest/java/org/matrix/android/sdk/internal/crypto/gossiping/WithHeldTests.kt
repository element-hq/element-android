/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto.gossiping

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.MockOkHttpInterceptor
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.crypto.model.event.WithHeldCode
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class WithHeldTests : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun test_WithHeldUnverifiedReason() {
        // =============================
        // ARRANGE
        // =============================

        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val bobSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        // Initialize cross signing on both
        mCryptoTestHelper.initializeCrossSigning(aliceSession)
        mCryptoTestHelper.initializeCrossSigning(bobSession)

        val roomId = mCryptoTestHelper.createDM(aliceSession, bobSession)
        mCryptoTestHelper.verifySASCrossSign(aliceSession, bobSession, roomId)

        val roomAlicePOV = aliceSession.getRoom(roomId)!!

        val bobUnverifiedSession = mTestHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        // =============================
        // ACT
        // =============================

        // Alice decide to not send to unverified sessions
        aliceSession.cryptoService().setGlobalBlacklistUnverifiedDevices(true)

        val timelineEvent = mTestHelper.sendTextMessage(roomAlicePOV, "Hello Bob", 1).first()

        // await for bob unverified session to get the message
        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                bobUnverifiedSession.getRoom(roomId)?.getTimeLineEvent(timelineEvent.eventId) != null
            }
        }

        val eventBobPOV = bobUnverifiedSession.getRoom(roomId)?.getTimeLineEvent(timelineEvent.eventId)!!

        // =============================
        // ASSERT
        // =============================

        // Bob should not be able to decrypt because the keys is withheld
        try {
            // .. might need to wait a bit for stability?
            bobUnverifiedSession.cryptoService().decryptEvent(eventBobPOV.root, "")
            Assert.fail("This session should not be able to decrypt")
        } catch (failure: Throwable) {
            val type = (failure as MXCryptoError.Base).errorType
            val technicalMessage = failure.technicalMessage
            Assert.assertEquals("Error should be withheld", MXCryptoError.ErrorType.KEYS_WITHHELD, type)
            Assert.assertEquals("Cause should be unverified", WithHeldCode.UNVERIFIED.value, technicalMessage)
        }

        // enable back sending to unverified
        aliceSession.cryptoService().setGlobalBlacklistUnverifiedDevices(false)

        val secondEvent = mTestHelper.sendTextMessage(roomAlicePOV, "Verify your device!!", 1).first()

        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                val ev = bobUnverifiedSession.getRoom(roomId)?.getTimeLineEvent(secondEvent.eventId)
                // wait until it's decrypted
                ev?.root?.getClearType() == EventType.MESSAGE
            }
        }

        // Previous message should still be undecryptable (partially withheld session)
        try {
            // .. might need to wait a bit for stability?
            bobUnverifiedSession.cryptoService().decryptEvent(eventBobPOV.root, "")
            Assert.fail("This session should not be able to decrypt")
        } catch (failure: Throwable) {
            val type = (failure as MXCryptoError.Base).errorType
            val technicalMessage = failure.technicalMessage
            Assert.assertEquals("Error should be withheld", MXCryptoError.ErrorType.KEYS_WITHHELD, type)
            Assert.assertEquals("Cause should be unverified", WithHeldCode.UNVERIFIED.value, technicalMessage)
        }

        mTestHelper.signOutAndClose(aliceSession)
        mTestHelper.signOutAndClose(bobSession)
        mTestHelper.signOutAndClose(bobUnverifiedSession)
    }

    @Test
    fun  test_WithHeldNoOlm() {
        val testData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!
        val aliceInterceptor = mTestHelper.getTestInterceptor(aliceSession)

        // Simulate no OTK
        aliceInterceptor!!.addRule(MockOkHttpInterceptor.SimpleRule(
                "/keys/claim",
                200,
                """
                   { "one_time_keys" : {} } 
                """
        ))
        Log.d("#TEST", "Recovery :${aliceSession.sessionParams.credentials.accessToken}")

        val roomAlicePov = aliceSession.getRoom(testData.roomId)!!

        val eventId = mTestHelper.sendTextMessage(roomAlicePov, "first message", 1).first().eventId

        // await for bob session to get the message
        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                bobSession.getRoom(testData.roomId)?.getTimeLineEvent(eventId) != null
            }
        }

        // Previous message should still be undecryptable (partially withheld session)
        val eventBobPOV = bobSession.getRoom(testData.roomId)?.getTimeLineEvent(eventId)
        try {
            // .. might need to wait a bit for stability?
            bobSession.cryptoService().decryptEvent(eventBobPOV!!.root, "")
            Assert.fail("This session should not be able to decrypt")
        } catch (failure: Throwable) {
            val type = (failure as MXCryptoError.Base).errorType
            val technicalMessage = failure.technicalMessage
            Assert.assertEquals("Error should be withheld", MXCryptoError.ErrorType.KEYS_WITHHELD, type)
            Assert.assertEquals("Cause should be unverified", WithHeldCode.NO_OLM.value, technicalMessage)
        }

        // Ensure that alice has marked the session to be shared with bob
        val sessionId = eventBobPOV!!.root.content.toModel<EncryptedEventContent>()!!.sessionId!!
        val chainIndex = aliceSession.cryptoService().getSharedWithInfo(testData.roomId, sessionId).getObject(bobSession.myUserId, bobSession.sessionParams.credentials.deviceId)

        Assert.assertEquals("Alice should have marked bob's device for this session", 0, chainIndex)
        // Add a new device for bob

        aliceInterceptor.clearRules()
        val bobSecondSession = mTestHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(withInitialSync = true))
        // send a second message
        val secondMessageId = mTestHelper.sendTextMessage(roomAlicePov, "second message", 1).first().eventId

        // Check that the
        // await for bob SecondSession session to get the message
        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                bobSecondSession.getRoom(testData.roomId)?.getTimeLineEvent(secondMessageId) != null
            }
        }

        val chainIndex2 = aliceSession.cryptoService().getSharedWithInfo(testData.roomId, sessionId).getObject(bobSecondSession.myUserId, bobSecondSession.sessionParams.credentials.deviceId)

        Assert.assertEquals("Alice should have marked bob's device for this session", 1, chainIndex2)

        aliceInterceptor.clearRules()
        testData.cleanUp(mTestHelper)
        mTestHelper.signOutAndClose(bobSecondSession)
    }

    @Test
    fun test_WithHeldKeyRequest() {
        val testData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!

        val roomAlicePov = aliceSession.getRoom(testData.roomId)!!

        val eventId = mTestHelper.sendTextMessage(roomAlicePov, "first message", 1).first().eventId

        mTestHelper.signOutAndClose(bobSession)

        // Create a new session for bob

        val bobSecondSession = mTestHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))
        // initialize to force request keys if missing
        mCryptoTestHelper.initializeCrossSigning(bobSecondSession)

        // Trust bob second device from Alice POV
        aliceSession.cryptoService().crossSigningService().trustDevice(bobSecondSession.sessionParams.deviceId!!, NoOpMatrixCallback())
        bobSecondSession.cryptoService().crossSigningService().trustDevice(aliceSession.sessionParams.deviceId!!, NoOpMatrixCallback())

        var sessionId: String? = null
        // Check that the
        // await for bob SecondSession session to get the message
        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                val timeLineEvent = bobSecondSession.getRoom(testData.roomId)?.getTimeLineEvent(eventId)?.also {
                    // try to decrypt and force key request
                    tryOrNull { bobSecondSession.cryptoService().decryptEvent(it.root, "") }
                }
                sessionId = timeLineEvent?.root?.content?.toModel<EncryptedEventContent>()?.sessionId
                timeLineEvent != null
            }
        }

        // Check that bob second session requested the key
        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                val wc = bobSecondSession.cryptoService().getWithHeldMegolmSession(roomAlicePov.roomId, sessionId!!)
                wc?.code == WithHeldCode.UNAUTHORISED
            }
        }
    }
}
