/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.action

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.mockk
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class CheckIfCanRedactEventUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val checkIfCanRedactEventUseCase = CheckIfCanRedactEventUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given an event which can be redacted and owned by user when use case executes then the result is true`() {
        val canRedactEventTypes = listOf(
                EventType.MESSAGE,
                EventType.STICKER,
                VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO
        ) +
                EventType.POLL_START.values +
                EventType.STATE_ROOM_BEACON_INFO.values

        canRedactEventTypes.forEach { eventType ->
            val event = givenAnEvent(
                    eventType = eventType,
                    senderId = fakeActiveSessionHolder.fakeSession.myUserId
            )

            val actionPermissions = givenActionPermissions(canRedact = false)

            val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

            result shouldBe true
        }
    }

    @Test
    fun `given redact permission and an event which can be redacted and sent by another user when use case executes then the result is true`() {
        val event = givenAnEvent(
                eventType = EventType.MESSAGE,
                senderId = "user-id"
        )

        val actionPermissions = givenActionPermissions(canRedact = true)

        val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

        result shouldBe true
    }

    @Test
    fun `given an event which cannot be redacted when use case executes then the result is false`() {
        val event = givenAnEvent(
                eventType = EventType.CALL_ANSWER,
                senderId = fakeActiveSessionHolder.fakeSession.myUserId
        )

        val actionPermissions = givenActionPermissions(canRedact = false)

        val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

        result shouldBe false
    }

    @Test
    fun `given missing redact permission and an event which can be redacted and sent by another user when use case executes then the result is false`() {
        val event = givenAnEvent(
                eventType = EventType.MESSAGE,
                senderId = "user-id"
        )

        val actionPermissions = givenActionPermissions(canRedact = false)

        val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

        result shouldBe false
    }

    private fun givenAnEvent(eventType: String, senderId: String): TimelineEvent {
        val eventId = "event-id"
        return TimelineEvent(
                root = Event(
                        eventId = eventId,
                        type = eventType,
                        senderId = senderId
                ),
                localId = 123L,
                eventId = eventId,
                displayIndex = 1,
                ownedByThreadChunk = false,
                senderInfo = mockk()
        )
    }

    private fun givenActionPermissions(canRedact: Boolean): ActionPermissions {
        return ActionPermissions(canRedact = canRedact)
    }
}
