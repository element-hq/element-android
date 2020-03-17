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

package im.vector.matrix.android.internal.crypto.gossiping

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomDirectoryVisibility
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.SessionTestParams
import im.vector.matrix.android.common.TestConstants
import im.vector.matrix.android.internal.crypto.GossipingRequestState
import im.vector.matrix.android.internal.crypto.OutgoingGossipingRequestState
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class KeyShareTests : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())

    @Test
    fun test_DoNotSelfShareIfNotTrusted() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        // Create an encrypted room and add a message
        val roomId = mTestHelper.doSync<String> {
            aliceSession.createRoom(
                    CreateRoomParams(RoomDirectoryVisibility.PRIVATE).enableEncryptionWithAlgorithm(true),
                    it
            )
        }
        val room = aliceSession.getRoom(roomId)
        assertNotNull(room)
        Thread.sleep(4_000)
        assertTrue(room?.isEncrypted() == true)
        val sentEventId = mTestHelper.sendTextMessage(room!!, "My Message", 1).first().eventId

        // Open a new sessionx

        val aliceSession2 = mTestHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(true))

        val roomSecondSessionPOV = aliceSession2.getRoom(roomId)

        val receivedEvent = roomSecondSessionPOV?.getTimeLineEvent(sentEventId)
        assertNotNull(receivedEvent)
        assert(receivedEvent!!.isEncrypted())

        try {
            aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            fail("should fail")
        } catch (failure: Throwable) {
        }

        val outgoingRequestBefore = aliceSession2.cryptoService().getOutgoingRoomKeyRequest()
        // Try to request
        aliceSession2.cryptoService().requestRoomKeyForEvent(receivedEvent.root)

        val waitLatch = CountDownLatch(1)
        val eventMegolmSessionId = receivedEvent.root.content.toModel<EncryptedEventContent>()?.sessionId

        var outGoingRequestId: String? = null

        retryPeriodicallyWithLatch(waitLatch) {
            aliceSession2.cryptoService().getOutgoingRoomKeyRequest()
                    .filter { req ->
                        // filter out request that was known before
                        !outgoingRequestBefore.any { req.requestId == it.requestId }
                    }
                    .let {
                        val outgoing = it.firstOrNull { it.sessionId == eventMegolmSessionId }
                        outGoingRequestId = outgoing?.requestId
                        outgoing != null
                    }
        }
        mTestHelper.await(waitLatch)

        Log.v("TEST", "=======> Outgoing requet Id is $outGoingRequestId")

        val outgoingRequestAfter = aliceSession2.cryptoService().getOutgoingRoomKeyRequest()

        // We should have a new request
        Assert.assertTrue(outgoingRequestAfter.size > outgoingRequestBefore.size)
        Assert.assertNotNull(outgoingRequestAfter.first { it.sessionId == eventMegolmSessionId })

        // The first session should see an incoming request
        // the request should be refused, because the device is not trusted
        waitWithLatch { latch ->
            retryPeriodicallyWithLatch(latch) {
                // DEBUG LOGS
                aliceSession.cryptoService().getIncomingRoomKeyRequest().let {
                    Log.v("TEST", "Incoming request Session 1 (looking for $outGoingRequestId)")
                    Log.v("TEST", "=========================")
                    it.forEach { keyRequest ->
                        Log.v("TEST", "[ts${keyRequest.localCreationTimestamp}] requestId ${keyRequest.requestId}, for sessionId ${keyRequest.requestBody?.sessionId} is ${keyRequest.state}")
                    }
                    Log.v("TEST", "=========================")
                }

                val incoming = aliceSession.cryptoService().getIncomingRoomKeyRequest().firstOrNull { it.requestId == outGoingRequestId }
                incoming?.state == GossipingRequestState.REJECTED
            }
        }

        try {
            aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            fail("should fail")
        } catch (failure: Throwable) {
        }

        // Mark the device as trusted
        aliceSession.cryptoService().setDeviceVerification(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true), aliceSession.myUserId,
                aliceSession2.sessionParams.credentials.deviceId ?: "")

        // Re request
        aliceSession2.cryptoService().reRequestRoomKeyForEvent(receivedEvent.root)

        waitWithLatch { latch ->
            retryPeriodicallyWithLatch(latch) {
                aliceSession.cryptoService().getIncomingRoomKeyRequest().let {
                    Log.v("TEST", "Incoming request Session 1")
                    Log.v("TEST", "=========================")
                    it.forEach {
                        Log.v("TEST", "requestId ${it.requestId}, for sessionId ${it.requestBody?.sessionId} is ${it.state}")
                    }
                    Log.v("TEST", "=========================")

                    it.any { it.requestBody?.sessionId == eventMegolmSessionId && it.state == GossipingRequestState.ACCEPTED }
                }
            }
        }

        Thread.sleep(6_000)
        waitWithLatch { latch ->
            retryPeriodicallyWithLatch(latch) {
                aliceSession2.cryptoService().getOutgoingRoomKeyRequest().let {
                    it.any { it.requestBody?.sessionId == eventMegolmSessionId && it.state == OutgoingGossipingRequestState.CANCELLED }
                }
            }
        }

        try {
            aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
        } catch (failure: Throwable) {
            fail("should have been able to decrypt")
        }

        mTestHelper.signOutAndClose(aliceSession)
        mTestHelper.signOutAndClose(aliceSession2)
    }

    fun retryPeriodicallyWithLatch(latch: CountDownLatch, condition: (() -> Boolean)) {
        GlobalScope.launch {
            while (true) {
                delay(1000)
                if (condition()) {
                    latch.countDown()
                    return@launch
                }
            }
        }
    }

    fun waitWithLatch(block: (CountDownLatch) -> Unit) {
        val latch = CountDownLatch(1)
        block(latch)
        mTestHelper.await(latch)
    }
}
