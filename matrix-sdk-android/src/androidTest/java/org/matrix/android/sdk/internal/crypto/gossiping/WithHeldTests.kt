/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.MockOkHttpInterceptor
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class WithHeldTests : InstrumentedTest {

    private val testHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(testHelper)

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_WithHeldUnverifiedReason() {
        // =============================
        // ARRANGE
        // =============================

        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val bobSession = testHelper.createAccount(TestConstants.USER_BOB, SessionTestParams(true))

        // Initialize cross signing on both
        cryptoTestHelper.initializeCrossSigning(aliceSession)
        cryptoTestHelper.initializeCrossSigning(bobSession)

        val roomId = cryptoTestHelper.createDM(aliceSession, bobSession)
        cryptoTestHelper.verifySASCrossSign(aliceSession, bobSession, roomId)

        val roomAlicePOV = aliceSession.getRoom(roomId)!!

        val bobUnverifiedSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        // =============================
        // ACT
        // =============================

        // Alice decide to not send to unverified sessions
        aliceSession.cryptoService().setGlobalBlacklistUnverifiedDevices(true)

        val timelineEvent = testHelper.sendTextMessage(roomAlicePOV, "Hello Bob", 1).first()

        // await for bob unverified session to get the message
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                bobUnverifiedSession.getRoom(roomId)?.getTimelineEvent(timelineEvent.eventId) != null
            }
        }

        val eventBobPOV = bobUnverifiedSession.getRoom(roomId)?.getTimelineEvent(timelineEvent.eventId)!!

        // =============================
        // ASSERT
        // =============================

        // Bob should not be able to decrypt because the keys is withheld
        try {
            // .. might need to wait a bit for stability?
            testHelper.runBlockingTest {
                bobUnverifiedSession.cryptoService().decryptEvent(eventBobPOV.root, "")
            }
            Assert.fail("This session should not be able to decrypt")
        } catch (failure: Throwable) {
            val type = (failure as MXCryptoError.Base).errorType
            val technicalMessage = failure.technicalMessage
            Assert.assertEquals("Error should be withheld", MXCryptoError.ErrorType.KEYS_WITHHELD, type)
            Assert.assertEquals("Cause should be unverified", WithHeldCode.UNVERIFIED.value, technicalMessage)
        }

        // enable back sending to unverified
        aliceSession.cryptoService().setGlobalBlacklistUnverifiedDevices(false)

        val secondEvent = testHelper.sendTextMessage(roomAlicePOV, "Verify your device!!", 1).first()

        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val ev = bobUnverifiedSession.getRoom(roomId)?.getTimelineEvent(secondEvent.eventId)
                // wait until it's decrypted
                ev?.root?.getClearType() == EventType.MESSAGE
            }
        }

        // Previous message should still be undecryptable (partially withheld session)
        try {
            // .. might need to wait a bit for stability?
            testHelper.runBlockingTest {
                bobUnverifiedSession.cryptoService().decryptEvent(eventBobPOV.root, "")
            }
            Assert.fail("This session should not be able to decrypt")
        } catch (failure: Throwable) {
            val type = (failure as MXCryptoError.Base).errorType
            val technicalMessage = failure.technicalMessage
            Assert.assertEquals("Error should be withheld", MXCryptoError.ErrorType.KEYS_WITHHELD, type)
            Assert.assertEquals("Cause should be unverified", WithHeldCode.UNVERIFIED.value, technicalMessage)
        }

        testHelper.signOutAndClose(aliceSession)
        testHelper.signOutAndClose(bobSession)
        testHelper.signOutAndClose(bobUnverifiedSession)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_WithHeldNoOlm() {
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!
        val aliceInterceptor = testHelper.getTestInterceptor(aliceSession)

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

        val eventId = testHelper.sendTextMessage(roomAlicePov, "first message", 1).first().eventId

        // await for bob session to get the message
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                bobSession.getRoom(testData.roomId)?.getTimelineEvent(eventId) != null
            }
        }

        // Previous message should still be undecryptable (partially withheld session)
        val eventBobPOV = bobSession.getRoom(testData.roomId)?.getTimelineEvent(eventId)
        try {
            // .. might need to wait a bit for stability?
            testHelper.runBlockingTest {
                bobSession.cryptoService().decryptEvent(eventBobPOV!!.root, "")
            }
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
        val bobSecondSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(withInitialSync = true))
        // send a second message
        val secondMessageId = testHelper.sendTextMessage(roomAlicePov, "second message", 1).first().eventId

        // Check that the
        // await for bob SecondSession session to get the message
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                bobSecondSession.getRoom(testData.roomId)?.getTimelineEvent(secondMessageId) != null
            }
        }

        val chainIndex2 = aliceSession.cryptoService().getSharedWithInfo(testData.roomId, sessionId).getObject(bobSecondSession.myUserId, bobSecondSession.sessionParams.credentials.deviceId)

        Assert.assertEquals("Alice should have marked bob's device for this session", 1, chainIndex2)

        aliceInterceptor.clearRules()
        testData.cleanUp(testHelper)
        testHelper.signOutAndClose(bobSecondSession)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_WithHeldKeyRequest() {
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!

        val roomAlicePov = aliceSession.getRoom(testData.roomId)!!

        val eventId = testHelper.sendTextMessage(roomAlicePov, "first message", 1).first().eventId

        testHelper.signOutAndClose(bobSession)

        // Create a new session for bob

        val bobSecondSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))
        // initialize to force request keys if missing
        cryptoTestHelper.initializeCrossSigning(bobSecondSession)

        // Trust bob second device from Alice POV
        aliceSession.cryptoService().crossSigningService().trustDevice(bobSecondSession.sessionParams.deviceId!!, NoOpMatrixCallback())
        bobSecondSession.cryptoService().crossSigningService().trustDevice(aliceSession.sessionParams.deviceId!!, NoOpMatrixCallback())

        var sessionId: String? = null
        // Check that the
        // await for bob SecondSession session to get the message
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val timeLineEvent = bobSecondSession.getRoom(testData.roomId)?.getTimelineEvent(eventId)?.also {
                    // try to decrypt and force key request
                    tryOrNull {
                        testHelper.runBlockingTest {
                            bobSecondSession.cryptoService().decryptEvent(it.root, "")
                        }
                    }
                }
                sessionId = timeLineEvent?.root?.content?.toModel<EncryptedEventContent>()?.sessionId
                timeLineEvent != null
            }
        }

        // Check that bob second session requested the key
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val wc = bobSecondSession.cryptoService().getWithHeldMegolmSession(roomAlicePov.roomId, sessionId!!)
                wc?.code == WithHeldCode.UNAUTHORISED
            }
        }
    }
}
