/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.render

import androidx.annotation.StringRes
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.lib.strings.CommonStrings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getPollQuestion
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.isAudioMessage
import org.matrix.android.sdk.api.session.events.model.isFileMessage
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isLiveLocation
import org.matrix.android.sdk.api.session.events.model.isPollEnd
import org.matrix.android.sdk.api.session.events.model.isPollStart
import org.matrix.android.sdk.api.session.events.model.isSticker
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.events.model.isVoiceMessage
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.model.relation.ReplyToContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent

private const val A_ROOM_ID = "room-id"
private const val AN_EVENT_ID = "event-id"
private const val A_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY =
        "<mx-reply>" +
                "<blockquote>" +
                "<a href=\"matrixToLink\">In reply to</a> " +
                "<a href=\"matrixToLink\">@user:matrix.org</a>" +
                "<br />" +
                "Message content" +
                "</blockquote>" +
                "</mx-reply>" +
                "Reply text"
private const val A_NEW_PREFIX = "new-prefix"
private const val A_NEW_CONTENT = "new-content"
private const val PREFIX_PROCESSED_ONLY_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY =
        "<mx-reply>" +
                "<blockquote>" +
                "<a href=\"matrixToLink\">$A_NEW_PREFIX</a> " +
                "<a href=\"matrixToLink\">@user:matrix.org</a>" +
                "<br />" +
                "Message content" +
                "</blockquote>" +
                "</mx-reply>" +
                "Reply text"
private const val FULLY_PROCESSED_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY =
        "<mx-reply>" +
                "<blockquote>" +
                "<a href=\"matrixToLink\">$A_NEW_PREFIX</a> " +
                "<a href=\"matrixToLink\">@user:matrix.org</a>" +
                "<br />" +
                A_NEW_CONTENT +
                "</blockquote>" +
                "</mx-reply>" +
                "Reply text"

class ProcessBodyOfReplyToEventUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeStringProvider = FakeStringProvider()
    private val fakeReplyToContent = ReplyToContent(eventId = AN_EVENT_ID)
    private val fakeRepliedEvent = givenARepliedEvent()

    private val processBodyOfReplyToEventUseCase = ProcessBodyOfReplyToEventUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            stringProvider = fakeStringProvider.instance,
    )

    @Before
    fun setup() {
        givenNewPrefix()
        mockkStatic("org.matrix.android.sdk.api.session.events.model.EventKt")
        mockkStatic("org.matrix.android.sdk.api.session.room.timeline.TimelineEventKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a replied event of type file message when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isFileMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_sent_file, content = A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type voice message when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isVoiceMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_sent_voice_message, content = A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type audio message when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isAudioMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_sent_audio_file, content = A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type image message when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isImageMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_sent_image, content = A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type video message when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isVideoMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_sent_video, content = A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type sticker message when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isStickerMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_sent_sticker, content = A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type poll start message with null question when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isPollStartMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_created_poll, content = A_NEW_CONTENT)
        every { fakeRepliedEvent.getPollQuestion() } returns null

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type poll start message with existing question when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isPollStartMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_created_poll, content = "")
        every { fakeRepliedEvent.getPollQuestion() } returns A_NEW_CONTENT

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type poll end message with null question when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isPollEndMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_ended_poll, content = A_NEW_CONTENT)
        givenPollQuestionReturns(fakeRepliedEvent, null)
        every { fakeRepliedEvent.getPollQuestion() } returns null

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type poll end message with existing question when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isPollEndMessage = true)
        givenContentForId(CommonStrings.message_reply_to_sender_ended_poll, content = "")
        every { fakeRepliedEvent.getClearType() } returns EventType.POLL_END.unstable
        givenPollQuestionReturns(fakeRepliedEvent, A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type live location message when process the formatted body then content is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent(isLiveLocationMessage = true)
        givenContentForId(CommonStrings.live_location_description, content = A_NEW_CONTENT)

        executeAndAssertResult()
    }

    @Test
    fun `given a replied event of type not handled when process the formatted body only prefix is replaced by correct string`() {
        // Given
        givenTypeOfRepliedEvent()

        // When
        val result = processBodyOfReplyToEventUseCase.execute(
                roomId = A_ROOM_ID,
                matrixFormattedBody = A_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY,
                replyToContent = fakeReplyToContent,
        )

        // Then
        result shouldBeEqualTo PREFIX_PROCESSED_ONLY_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY
    }

    @Test
    fun `given no replied event found when process the formatted body then only prefix is replaced by correct string`() {
        // Given
        givenARepliedEvent(timelineEvent = null)

        // When
        val result = processBodyOfReplyToEventUseCase.execute(
                roomId = A_ROOM_ID,
                matrixFormattedBody = A_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY,
                replyToContent = fakeReplyToContent,
        )

        // Then
        result shouldBeEqualTo PREFIX_PROCESSED_ONLY_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY
    }

    private fun executeAndAssertResult() {
        // When
        val result = processBodyOfReplyToEventUseCase.execute(
                roomId = A_ROOM_ID,
                matrixFormattedBody = A_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY,
                replyToContent = fakeReplyToContent,
        )

        // Then
        result shouldBeEqualTo FULLY_PROCESSED_REPLY_TO_EVENT_MATRIX_FORMATTED_BODY
    }

    private fun givenARepliedEvent(timelineEvent: TimelineEvent? = mockk()): Event {
        val event = mockk<Event>()
        val eventId = "event-id"
        every { event.eventId } returns eventId
        every { event.roomId } returns A_ROOM_ID
        timelineEvent?.let { every { it.root } returns event }
        fakeActiveSessionHolder
                .fakeSession
                .roomService()
                .getRoom(A_ROOM_ID)
                .timelineService()
                .givenTimelineEventReturns(eventId, timelineEvent)
        return event
    }

    private fun givenTypeOfRepliedEvent(
            isFileMessage: Boolean = false,
            isVoiceMessage: Boolean = false,
            isAudioMessage: Boolean = false,
            isImageMessage: Boolean = false,
            isVideoMessage: Boolean = false,
            isStickerMessage: Boolean = false,
            isPollEndMessage: Boolean = false,
            isPollStartMessage: Boolean = false,
            isLiveLocationMessage: Boolean = false,
    ) {
        every { fakeRepliedEvent.isFileMessage() } returns isFileMessage
        every { fakeRepliedEvent.isVoiceMessage() } returns isVoiceMessage
        every { fakeRepliedEvent.isAudioMessage() } returns isAudioMessage
        every { fakeRepliedEvent.isImageMessage() } returns isImageMessage
        every { fakeRepliedEvent.isVideoMessage() } returns isVideoMessage
        every { fakeRepliedEvent.isSticker() } returns isStickerMessage
        every { fakeRepliedEvent.isPollEnd() } returns isPollEndMessage
        every { fakeRepliedEvent.isPollStart() } returns isPollStartMessage
        every { fakeRepliedEvent.isLiveLocation() } returns isLiveLocationMessage
    }

    private fun givenNewPrefix() {
        fakeStringProvider.given(CommonStrings.message_reply_to_prefix, A_NEW_PREFIX)
    }

    private fun givenContentForId(@StringRes resId: Int, content: String) {
        fakeStringProvider.given(resId, content)
    }

    private fun givenPollQuestionReturns(pollEndEvent: Event, question: String?) {
        val eventId = "start-event-id"
        val relationContent = mockk<RelationDefaultContent>()
        every { relationContent.eventId } returns eventId
        every { pollEndEvent.getRelationContent() } returns relationContent
        val timelineEvent = mockk<TimelineEvent>()
        val messagePollContent = MessagePollContent(
                pollCreationInfo = PollCreationInfo(
                        question = PollQuestion(unstableQuestion = question)
                )
        )
        every { timelineEvent.getLastMessageContent() } returns messagePollContent
        fakeActiveSessionHolder
                .fakeSession
                .roomService()
                .getRoom(A_ROOM_ID)
                .timelineService()
                .givenTimelineEventReturns(eventId, timelineEvent)
    }
}
