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

package org.matrix.android.sdk.session.room.timeline

import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import java.util.concurrent.CountDownLatch
import kotlin.test.fail

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class TimelineWithManyMembersTest : InstrumentedTest {

    private val NUMBER_OF_MEMBERS = 5

    private val commonTestHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(commonTestHelper)

    /**
     * Ensures when someone sends a message to a crowded room, everyone can decrypt the message.
     */
    @Test
    fun everyoneShouldDecryptMessage3Members() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobAndSamInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val samSession = cryptoTestData.thirdSession!!

        val aliceRoomId = cryptoTestData.roomId

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)
        bobSession.cryptoService().setWarnOnUnknownDevices(false)
        samSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!
        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
        val roomFromSamPOV = samSession.getRoom(aliceRoomId)!!

        val bobTimeline = roomFromBobPOV.createTimeline(null, TimelineSettings(30))
        val samTimeline = roomFromSamPOV.createTimeline(null, TimelineSettings(30))
        bobTimeline.start()
        samTimeline.start()

        val firstMessage = "First messages from Alice"
        commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                firstMessage,
                1)

        bobSession.startSync(true)
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                snapshot.firstOrNull()?.root?.getClearContent()?.toModel<MessageContent>()?.body?.startsWith(firstMessage).orFalse()
            }
            bobTimeline.addListener(eventsListener)
            commonTestHelper.await(lock)
        }
        bobSession.stopSync()

        samSession.startSync(true)
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                snapshot.firstOrNull()?.root?.getClearContent()?.toModel<MessageContent>()?.body?.startsWith(firstMessage).orFalse()
            }
            samTimeline.addListener(eventsListener)
            commonTestHelper.await(lock)
        }
        samSession.stopSync()
    }

    /**
     * Ensures when someone sends a message to a crowded room, everyone can decrypt the message.
     */
    @Test
    fun everyone_should_decrypt_message_in_a_crowded_room() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithManyMembers(NUMBER_OF_MEMBERS)

        val sessionForFirstMember = cryptoTestData.firstSession
        val roomForFirstMember = sessionForFirstMember.getRoom(cryptoTestData.roomId)!!

        val firstMessage = "First messages from Alice"
        commonTestHelper.sendTextMessage(
                roomForFirstMember,
                firstMessage,
                1,
                600_000
        )

        for (index in 1 until cryptoTestData.sessions.size) {
            val session = cryptoTestData.sessions[index]
            val roomForCurrentMember = session.getRoom(cryptoTestData.roomId)!!
            val timelineForCurrentMember = roomForCurrentMember.createTimeline(null, TimelineSettings(30))
            timelineForCurrentMember.start()

            session.startSync(true)

            run {
                val lock = CountDownLatch(1)
                val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                    snapshot
                            .find { it.isEncrypted() }
                            ?.let {
                                val body = it.root.getClearContent()?.toModel<MessageContent>()?.body
                                if (body?.startsWith(firstMessage).orFalse()) {
                                    return@createEventListener true
                                } else {
                                    fail("User " + session.myUserId + " decrypted as " + body + " CryptoError: " + it.root.mCryptoError)
                                }
                            } ?: return@createEventListener false
                }
                timelineForCurrentMember.addListener(eventsListener)
                commonTestHelper.await(lock, 600_000)
            }
            session.stopSync()
        }
    }
}
