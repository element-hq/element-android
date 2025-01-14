/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.action

import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class CheckIfCanReplyEventUseCaseTest {

    private val checkIfCanReplyEventUseCase = CheckIfCanReplyEventUseCase()

    @Test
    fun `given sending message is not allowed when use case is executed then result is false`() {
        val event = givenAnEvent(eventType = EventType.MESSAGE)
        val messageContent = givenAMessageContent(MessageType.MSGTYPE_AUDIO)
        val actionPermissions = givenActionPermissions(canSendMessage = false)

        val result = checkIfCanReplyEventUseCase.execute(event, messageContent, actionPermissions)

        result shouldBeEqualTo false
    }

    @Test
    fun `given reply is allowed for the event type when use case is executed then result is true`() {
        val eventTypes = EventType.STATE_ROOM_BEACON_INFO.values + EventType.POLL_START.values + EventType.POLL_END.values + EventType.MESSAGE

        eventTypes.forEach { eventType ->
            val event = givenAnEvent(eventType)
            val messageContent = givenAMessageContent(MessageType.MSGTYPE_AUDIO)
            val actionPermissions = givenActionPermissions(canSendMessage = true)

            val result = checkIfCanReplyEventUseCase.execute(event, messageContent, actionPermissions)

            result shouldBeEqualTo true
        }
    }

    @Test
    fun `given reply is not allowed for the event type when use case is executed then result is false`() {
        val event = givenAnEvent(EventType.CALL_ANSWER)
        val messageContent = givenAMessageContent(MessageType.MSGTYPE_AUDIO)
        val actionPermissions = givenActionPermissions(canSendMessage = true)

        val result = checkIfCanReplyEventUseCase.execute(event, messageContent, actionPermissions)

        result shouldBeEqualTo false
    }

    @Test
    fun `given reply is allowed for the message type when use case is executed then result is true`() {
        val messageTypes = listOf(
                MessageType.MSGTYPE_TEXT,
                MessageType.MSGTYPE_NOTICE,
                MessageType.MSGTYPE_EMOTE,
                MessageType.MSGTYPE_IMAGE,
                MessageType.MSGTYPE_VIDEO,
                MessageType.MSGTYPE_AUDIO,
                MessageType.MSGTYPE_FILE,
                MessageType.MSGTYPE_POLL_START,
                MessageType.MSGTYPE_POLL_END,
                MessageType.MSGTYPE_BEACON_INFO,
                MessageType.MSGTYPE_LOCATION
        )

        messageTypes.forEach { messageType ->
            val event = givenAnEvent(EventType.MESSAGE)
            val messageContent = givenAMessageContent(messageType)
            val actionPermissions = givenActionPermissions(canSendMessage = true)

            val result = checkIfCanReplyEventUseCase.execute(event, messageContent, actionPermissions)

            result shouldBeEqualTo true
        }
    }

    @Test
    fun `given reply is not allowed for the message type when use case is executed then result is false`() {
        val event = givenAnEvent(EventType.MESSAGE)
        val messageContent = givenAMessageContent(MessageType.MSGTYPE_BEACON_LOCATION_DATA)
        val actionPermissions = givenActionPermissions(canSendMessage = true)

        val result = checkIfCanReplyEventUseCase.execute(event, messageContent, actionPermissions)

        result shouldBeEqualTo false
    }

    private fun givenAnEvent(eventType: String): TimelineEvent {
        val eventId = "event-id"
        return TimelineEvent(
                root = Event(
                        eventId = eventId,
                        type = eventType
                ),
                localId = 123L,
                eventId = eventId,
                displayIndex = 1,
                ownedByThreadChunk = false,
                senderInfo = mockk()
        )
    }

    private fun givenAMessageContent(messageType: String): MessageContent {
        return mockk<MessageContent>().also {
            every { it.msgType } returns messageType
        }
    }

    private fun givenActionPermissions(canSendMessage: Boolean): ActionPermissions {
        return ActionPermissions(canSendMessage = canSendMessage)
    }
}
