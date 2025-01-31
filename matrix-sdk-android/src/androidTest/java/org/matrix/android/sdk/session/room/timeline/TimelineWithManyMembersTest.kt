/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.session.room.timeline

import androidx.test.filters.LargeTest
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import java.util.concurrent.CountDownLatch

/** !! Not working with the new timeline
 *  Disabling it until the fix is made
 */
@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@Ignore("This test will be ignored until it is fixed")
@LargeTest
class TimelineWithManyMembersTest : InstrumentedTest {

    companion object {
        private const val NUMBER_OF_MEMBERS = 6
    }

    /**
     * Ensures when someone sends a message to a crowded room, everyone can decrypt the message.
     */

    @Test
    fun everyone_should_decrypt_message_in_a_crowded_room() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->
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
            val timelineForCurrentMember = roomForCurrentMember.timelineService().createTimeline(null, TimelineSettings(30))
            timelineForCurrentMember.start()

            session.syncService().startSync(true)

            run {
                val lock = CountDownLatch(1)
                val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                    snapshot
                            .find { it.isEncrypted() }
                            ?.let {
                                val body = it.root.getClearContent()?.toModel<MessageContent>()?.body
                                if (body?.startsWith(firstMessage).orFalse()) {
                                    println("User " + session.myUserId + " decrypted as " + body)
                                    return@createEventListener true
                                } else {
                                    fail("User " + session.myUserId + " decrypted as " + body + " CryptoError: " + it.root.mCryptoError)
                                    false
                                }
                            } ?: return@createEventListener false
                }
                timelineForCurrentMember.addListener(eventsListener)
                commonTestHelper.await(lock, 600_000)
            }
            session.syncService().stopSync()
        }
    }
}
