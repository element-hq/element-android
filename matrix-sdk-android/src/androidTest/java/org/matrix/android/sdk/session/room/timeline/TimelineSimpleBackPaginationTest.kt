/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.session.room.timeline

import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.internal.assertEquals
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
@Ignore
class TimelineSimpleBackPaginationTest : InstrumentedTest {

    @Test
    fun timeline_backPaginate_shouldReachEndOfTimeline() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->
        val numberOfMessagesToSent = 200

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val roomId = cryptoTestData.roomId

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)
        bobSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromAlicePOV = aliceSession.getRoom(roomId)!!
        val roomFromBobPOV = bobSession.getRoom(roomId)!!

        // Alice sends X messages
        val message = "Message from Alice"
        commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                message,
                numberOfMessagesToSent
        )

        val bobTimeline = roomFromBobPOV.timelineService().createTimeline(null, TimelineSettings(30))
        bobTimeline.start()

        commonTestHelper.waitWithLatch(timeout = TestConstants.timeOutMillis * 10) {
            val listener = object : Timeline.Listener {

                override fun onStateUpdated(direction: Timeline.Direction, state: Timeline.PaginationState) {
                    if (direction == Timeline.Direction.FORWARDS) {
                        return
                    }
                    if (state.hasMoreToLoad && !state.loading) {
                        bobTimeline.paginate(Timeline.Direction.BACKWARDS, 30)
                    } else if (!state.hasMoreToLoad) {
                        bobTimeline.removeListener(this)
                        it.countDown()
                    }
                }
            }
            bobTimeline.addListener(listener)
            bobTimeline.paginate(Timeline.Direction.BACKWARDS, 30)
        }
        assertEquals(false, bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS))
        assertEquals(false, bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS))

        val onlySentEvents = runBlocking {
            bobTimeline.getSnapshot()
        }
                .filter {
                    it.root.isTextMessage()
                }.filter {
                    (it.root.content.toModel<MessageTextContent>())?.body?.startsWith(message).orFalse()
                }
        assertEquals(numberOfMessagesToSent, onlySentEvents.size)

        bobTimeline.dispose()
    }
}
