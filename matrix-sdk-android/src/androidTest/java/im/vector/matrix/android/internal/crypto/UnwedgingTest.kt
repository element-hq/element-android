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

package im.vector.matrix.android.internal.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.CryptoTestHelper
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch

/**
 * Ref:
 * - https://github.com/matrix-org/matrix-doc/pull/1719
 * - https://matrix.org/docs/spec/client_server/latest#recovering-from-undecryptable-messages
 * - https://github.com/matrix-org/matrix-js-sdk/pull/780
 * - https://github.com/matrix-org/matrix-ios-sdk/pull/778
 * - https://github.com/matrix-org/matrix-ios-sdk/pull/784
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class UnwedgingTest : InstrumentedTest {

    private lateinit var messagesReceivedByBob: List<TimelineEvent>
    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Before
    fun init() {
        messagesReceivedByBob = emptyList()
    }

    /**
     * - Alice & Bob in a e2e room
     * - Alice sends a 1st message with a 1st megolm session
     * - Store the olm session between A&B devices
     * - Alice sends a 2nd message with a 2nd megolm session
     * - Simulate Alice using a backup of her OS and make her crypto state like after the first message
     * - Alice sends a 3rd message with a 3rd megolm session but a wedged olm session
     *
     * What Bob must see:
     * -> No issue with the 2 first messages
     * -> The third event must fail to decrypt at first because Bob the olm session is wedged
     * -> This is automatically fixed after SDKs restarted the olm session
     */
    @Test
    fun testUnwedging() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        val bobSession = cryptoTestData.secondSession!!

        val aliceCryptoStore = (aliceSession.cryptoService() as DefaultCryptoService).cryptoStoreForTesting

        bobSession.cryptoService().setWarnOnUnknownDevices(false)

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!

        val bobTimeline = roomFromBobPOV.createTimeline(null, TimelineSettings(20))
        bobTimeline.start()

        var latch = CountDownLatch(1)
        var bobEventsListener = createEventListener(latch, 1)
        bobTimeline.addListener(bobEventsListener)
        messagesReceivedByBob = emptyList()

        // - Alice sends a 1st message with a 1st megolm session
        roomFromAlicePOV.sendTextMessage("First message")

        // Wait for the message to be received by Bob
        mTestHelper.await(latch)
        bobTimeline.removeListener(bobEventsListener)

        messagesReceivedByBob.size shouldBe 1

        //  - Store the olm session between A&B devices
        // Let us pickle our session with bob here so we can later unpickle it
        // and wedge our session.
        val sessionIdsForBob = aliceCryptoStore.getDeviceSessionIds(bobSession.cryptoService().getMyDevice().identityKey()!!)
        sessionIdsForBob!!.size shouldBe 1
        val olmSession = aliceCryptoStore.getDeviceSession(sessionIdsForBob.first(), bobSession.cryptoService().getMyDevice().identityKey()!!)!!

        // Sam join the room
        val samSession = mCryptoTestHelper.createSamAccountAndInviteToTheRoom(roomFromAlicePOV)

        latch = CountDownLatch(1)
        bobEventsListener = createEventListener(latch, 2)
        bobTimeline.addListener(bobEventsListener)
        messagesReceivedByBob = emptyList()

        // - Alice sends a 2nd message with a 2nd megolm session
        roomFromAlicePOV.sendTextMessage("Second message")

        // Wait for the message to be received by Bob
        mTestHelper.await(latch)
        bobTimeline.removeListener(bobEventsListener)

        messagesReceivedByBob.size shouldBe 2

        // Let us wedge the session now. Set crypto state like after the first message
        aliceCryptoStore.storeSession(olmSession, bobSession.cryptoService().getMyDevice().identityKey()!!)

        latch = CountDownLatch(1)
        bobEventsListener = createEventListener(latch, 3)
        bobTimeline.addListener(bobEventsListener)
        messagesReceivedByBob = emptyList()

        // - Alice sends a 3rd message with a 3rd megolm session but a wedged olm session
        roomFromAlicePOV.sendTextMessage("Third message")

        // Wait for the message to be received by Bob
        mTestHelper.await(latch)
        bobTimeline.removeListener(bobEventsListener)

        messagesReceivedByBob.size shouldBe 3

        messagesReceivedByBob[0].root.getClearType() shouldBeEqualTo EventType.ENCRYPTED
        messagesReceivedByBob[1].root.getClearType() shouldBeEqualTo EventType.MESSAGE
        messagesReceivedByBob[2].root.getClearType() shouldBeEqualTo EventType.MESSAGE

        bobTimeline.dispose()

        cryptoTestData.cleanUp(mTestHelper)
        mTestHelper.signOutAndClose(samSession)
    }

    private fun createEventListener(latch: CountDownLatch, expectedNumberOfMessages: Int): Timeline.Listener {
        return object : Timeline.Listener {
            override fun onTimelineFailure(throwable: Throwable) {
                // noop
            }

            override fun onNewTimelineEvents(eventIds: List<String>) {
                // noop
            }

            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                messagesReceivedByBob = snapshot.filter { it.root.type == EventType.ENCRYPTED }

                if (messagesReceivedByBob.size == expectedNumberOfMessages) {
                    latch.countDown()
                }
            }
        }
    }
}
