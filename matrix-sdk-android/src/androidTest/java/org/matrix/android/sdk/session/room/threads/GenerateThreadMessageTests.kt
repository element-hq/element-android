/*
 * Copyright (c) 2022 New Vector Ltd
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
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import timber.log.Timber
import java.util.concurrent.CountDownLatch

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class GenerateThreadMessageTests : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(commonTestHelper)

    private val logPrefix = "---Test--> "

//    @Rule
//    @JvmField
//    val mRetryTestRule = RetryTestRule()

    @Test
    fun reply_in_thread_to_normal_timeline_message_should_create_a_thread() {
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
                val initMessageThreadDetails = snapshot.firstOrNull { it.root.eventId == initMessage.root.eventId }?.root?.threadDetails
                initMessageThreadDetails?.isRootThread?.shouldBeTrue() ?: assert(false)
                initMessageThreadDetails?.numberOfThreads?.shouldBe(1)
                Timber.e("$logPrefix $initMessageThreadDetails")
                true
            }
            timeline.addListener(eventsListener)
            commonTestHelper.await(lock, 600_000)
        }
        aliceSession.stopSync()
    }
}
