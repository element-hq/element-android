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

package org.matrix.android.sdk.session.room.threads

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import java.util.concurrent.CountDownLatch

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@Ignore("Remaining Integration tests are unstable if run with this test. Issue #5439")
class ThreadMessagingTest : InstrumentedTest {

    @Test
    fun reply_in_thread_should_create_a_thread() {
        val commonTestHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(commonTestHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val aliceRoom = aliceSession.getRoom(aliceRoomId)!!

        // Let's send a message in the normal timeline
        val textMessage = "This is a normal timeline message"
        val sentMessages = commonTestHelper.sendTextMessage(
                room = aliceRoom,
                message = textMessage,
                nbOfMessages = 1)

        val initMessage = sentMessages.first()

        initMessage.root.isThread().shouldBeFalse()
        initMessage.root.isTextMessage().shouldBeTrue()
        initMessage.root.getRootThreadEventId().shouldBeNull()
        initMessage.root.threadDetails?.isRootThread?.shouldBeFalse()

        // Let's reply in timeline to that message
        val repliesInThread = commonTestHelper.replyInThreadMessage(
                room = aliceRoom,
                message = "Reply In the above thread",
                numberOfMessages = 1,
                rootThreadEventId = initMessage.root.eventId.orEmpty())

        val replyInThread = repliesInThread.first()
        replyInThread.root.isThread().shouldBeTrue()
        replyInThread.root.isTextMessage().shouldBeTrue()
        replyInThread.root.getRootThreadEventId().shouldBeEqualTo(initMessage.root.eventId)

        // The init normal message should now be a root thread event
        val timeline = aliceRoom.createTimeline(null, TimelineSettings(30))
        timeline.start()

        aliceSession.startSync(true)
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                val initMessageThreadDetails = snapshot.firstOrNull {
                    it.root.eventId == initMessage.root.eventId
                }?.root?.threadDetails
                initMessageThreadDetails?.isRootThread?.shouldBeTrue()
                initMessageThreadDetails?.numberOfThreads?.shouldBe(1)
                true
            }
            timeline.addListener(eventsListener)
            commonTestHelper.await(lock, 600_000)
        }
        aliceSession.stopSync()
    }

    @Test
    fun reply_in_thread_should_create_a_thread_from_other_user() {
        val commonTestHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(commonTestHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        val aliceRoom = aliceSession.getRoom(aliceRoomId)!!

        // Let's send a message in the normal timeline
        val textMessage = "This is a normal timeline message"
        val sentMessages = commonTestHelper.sendTextMessage(
                room = aliceRoom,
                message = textMessage,
                nbOfMessages = 1)

        val initMessage = sentMessages.first()

        initMessage.root.isThread().shouldBeFalse()
        initMessage.root.isTextMessage().shouldBeTrue()
        initMessage.root.getRootThreadEventId().shouldBeNull()
        initMessage.root.threadDetails?.isRootThread?.shouldBeFalse()

        // Let's reply in timeline to that message from another user
        val bobSession = cryptoTestData.secondSession!!
        val bobRoomId = cryptoTestData.roomId
        val bobRoom = bobSession.getRoom(bobRoomId)!!

        val repliesInThread = commonTestHelper.replyInThreadMessage(
                room = bobRoom,
                message = "Reply In the above thread",
                numberOfMessages = 1,
                rootThreadEventId = initMessage.root.eventId.orEmpty())

        val replyInThread = repliesInThread.first()
        replyInThread.root.isThread().shouldBeTrue()
        replyInThread.root.isTextMessage().shouldBeTrue()
        replyInThread.root.getRootThreadEventId().shouldBeEqualTo(initMessage.root.eventId)

        // The init normal message should now be a root thread event
        val timeline = aliceRoom.createTimeline(null, TimelineSettings(30))
        timeline.start()

        aliceSession.startSync(true)
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                val initMessageThreadDetails = snapshot.firstOrNull { it.root.eventId == initMessage.root.eventId }?.root?.threadDetails
                initMessageThreadDetails?.isRootThread?.shouldBeTrue()
                initMessageThreadDetails?.numberOfThreads?.shouldBe(1)
                true
            }
            timeline.addListener(eventsListener)
            commonTestHelper.await(lock, 600_000)
        }
        aliceSession.stopSync()

        bobSession.startSync(true)
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                val initMessageThreadDetails = snapshot.firstOrNull { it.root.eventId == initMessage.root.eventId }?.root?.threadDetails
                initMessageThreadDetails?.isRootThread?.shouldBeTrue()
                initMessageThreadDetails?.numberOfThreads?.shouldBe(1)
                true
            }
            timeline.addListener(eventsListener)
            commonTestHelper.await(lock, 600_000)
        }
        bobSession.stopSync()
    }

    @Test
    fun reply_in_thread_to_timeline_message_multiple_times() {
        val commonTestHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(commonTestHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val aliceRoom = aliceSession.getRoom(aliceRoomId)!!

        // Let's send 5 messages in the normal timeline
        val textMessage = "This is a normal timeline message"
        val sentMessages = commonTestHelper.sendTextMessage(
                room = aliceRoom,
                message = textMessage,
                nbOfMessages = 5)

        sentMessages.forEach {
            it.root.isThread().shouldBeFalse()
            it.root.isTextMessage().shouldBeTrue()
            it.root.getRootThreadEventId().shouldBeNull()
            it.root.threadDetails?.isRootThread?.shouldBeFalse()
        }
        // let's start the thread from the second message
        val selectedInitMessage = sentMessages[1]

        // Let's reply 40 times in the timeline to the second message
        val repliesInThread = commonTestHelper.replyInThreadMessage(
                room = aliceRoom,
                message = "Reply In the above thread",
                numberOfMessages = 40,
                rootThreadEventId = selectedInitMessage.root.eventId.orEmpty())

        repliesInThread.forEach {
            it.root.isThread().shouldBeTrue()
            it.root.isTextMessage().shouldBeTrue()
            it.root.getRootThreadEventId()?.shouldBeEqualTo(selectedInitMessage.root.eventId.orEmpty()) ?: assert(false)
        }

        // The init normal message should now be a root thread event
        val timeline = aliceRoom.createTimeline(null, TimelineSettings(30))
        timeline.start()

        aliceSession.startSync(true)
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                val initMessageThreadDetails = snapshot.firstOrNull { it.root.eventId == selectedInitMessage.root.eventId }?.root?.threadDetails
                // Selected init message should be the thread root
                initMessageThreadDetails?.isRootThread?.shouldBeTrue()
                // All threads should be 40
                initMessageThreadDetails?.numberOfThreads?.shouldBeEqualTo(40)
                true
            }
            // Because we sent more than 30 messages we should paginate a bit more
            timeline.paginate(Timeline.Direction.BACKWARDS, 50)
            timeline.addListener(eventsListener)
            commonTestHelper.await(lock, 600_000)
        }
        aliceSession.stopSync()
    }

    @Test
    fun thread_summary_advanced_validation_after_multiple_messages_in_multiple_threads() {
        val commonTestHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(commonTestHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val aliceRoom = aliceSession.getRoom(aliceRoomId)!!

        // Let's send 5 messages in the normal timeline
        val textMessage = "This is a normal timeline message"
        val sentMessages = commonTestHelper.sendTextMessage(
                room = aliceRoom,
                message = textMessage,
                nbOfMessages = 5)

        sentMessages.forEach {
            it.root.isThread().shouldBeFalse()
            it.root.isTextMessage().shouldBeTrue()
            it.root.getRootThreadEventId().shouldBeNull()
            it.root.threadDetails?.isRootThread?.shouldBeFalse()
        }
        // let's start the thread from the second message
        val firstMessage = sentMessages[0]
        val secondMessage = sentMessages[1]

        // Alice will reply in thread to the second message 35 times
        val aliceThreadRepliesInSecondMessage = commonTestHelper.replyInThreadMessage(
                room = aliceRoom,
                message = "Alice reply In the above second thread message",
                numberOfMessages = 35,
                rootThreadEventId = secondMessage.root.eventId.orEmpty())

        // Let's reply in timeline to that message from another user
        val bobSession = cryptoTestData.secondSession!!
        val bobRoomId = cryptoTestData.roomId
        val bobRoom = bobSession.getRoom(bobRoomId)!!

        // Bob will reply in thread to the first message 35 times
        val bobThreadRepliesInFirstMessage = commonTestHelper.replyInThreadMessage(
                room = bobRoom,
                message = "Bob reply In the above first thread message",
                numberOfMessages = 42,
                rootThreadEventId = firstMessage.root.eventId.orEmpty())

        // Bob will also reply in second thread 5 times
        val bobThreadRepliesInSecondMessage = commonTestHelper.replyInThreadMessage(
                room = bobRoom,
                message = "Another Bob reply In the above second thread message",
                numberOfMessages = 20,
                rootThreadEventId = secondMessage.root.eventId.orEmpty())

        aliceThreadRepliesInSecondMessage.forEach {
            it.root.isThread().shouldBeTrue()
            it.root.isTextMessage().shouldBeTrue()
            it.root.getRootThreadEventId()?.shouldBeEqualTo(secondMessage.root.eventId.orEmpty()) ?: assert(false)
        }

        bobThreadRepliesInFirstMessage.forEach {
            it.root.isThread().shouldBeTrue()
            it.root.isTextMessage().shouldBeTrue()
            it.root.getRootThreadEventId()?.shouldBeEqualTo(firstMessage.root.eventId.orEmpty()) ?: assert(false)
        }

        bobThreadRepliesInSecondMessage.forEach {
            it.root.isThread().shouldBeTrue()
            it.root.isTextMessage().shouldBeTrue()
            it.root.getRootThreadEventId()?.shouldBeEqualTo(secondMessage.root.eventId.orEmpty()) ?: assert(false)
        }

        // The init normal message should now be a root thread event
        val timeline = aliceRoom.createTimeline(null, TimelineSettings(30))
        timeline.start()

        aliceSession.startSync(true)
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                val firstMessageThreadDetails = snapshot.firstOrNull { it.root.eventId == firstMessage.root.eventId }?.root?.threadDetails
                val secondMessageThreadDetails = snapshot.firstOrNull { it.root.eventId == secondMessage.root.eventId }?.root?.threadDetails

                // first & second message should be the thread root
                firstMessageThreadDetails?.isRootThread?.shouldBeTrue()
                secondMessageThreadDetails?.isRootThread?.shouldBeTrue()

                // First thread message should contain 42
                firstMessageThreadDetails?.numberOfThreads shouldBeEqualTo 42
                // Second thread message should contain 35+20
                secondMessageThreadDetails?.numberOfThreads shouldBeEqualTo 55

                true
            }
            // Because we sent more than 30 messages we should paginate a bit more
            timeline.paginate(Timeline.Direction.BACKWARDS, 50)
            timeline.paginate(Timeline.Direction.BACKWARDS, 50)
            timeline.addListener(eventsListener)
            commonTestHelper.await(lock, 600_000)
        }
        aliceSession.stopSync()
    }
}
