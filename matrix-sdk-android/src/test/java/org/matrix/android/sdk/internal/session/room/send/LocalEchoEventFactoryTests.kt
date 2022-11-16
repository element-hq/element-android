/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.send

import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContentWithFormattedBody
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.TextContent
import org.matrix.android.sdk.test.fakes.FakeClock
import org.matrix.android.sdk.test.fakes.FakeContext
import org.matrix.android.sdk.test.fakes.internal.session.content.FakeThumbnailExtractor
import org.matrix.android.sdk.test.fakes.internal.session.permalinks.FakePermalinkFactory
import org.matrix.android.sdk.test.fakes.internal.session.room.send.FakeLocalEchoRepository
import org.matrix.android.sdk.test.fakes.internal.session.room.send.FakeMarkdownParser
import org.matrix.android.sdk.test.fakes.internal.session.room.send.FakeWaveFormSanitizer
import org.matrix.android.sdk.test.fakes.internal.session.room.send.pills.FakeTextPillsUtils

@Suppress("MaxLineLength")
class LocalEchoEventFactoryTests {

    companion object {
        internal const val A_USER_ID_1 = "@user_1:matrix.org"
        internal const val A_ROOM_ID = "!sUeOGZKsBValPTUMax:matrix.org"
        internal const val AN_EVENT_ID = "\$vApgexcL8Vfh-WxYKsFKCDooo67ttbjm3TiVKXaWijU"
        internal const val AN_EPOCH = 1655210176L

        val A_START_EVENT = Event(
                type = EventType.STATE_ROOM_CREATE,
                eventId = AN_EVENT_ID,
                originServerTs = 1652435922563,
                senderId = A_USER_ID_1,
                roomId = A_ROOM_ID
        )
    }

    private val fakeContext = FakeContext()
    private val fakeMarkdownParser = FakeMarkdownParser()
    private val fakeTextPillsUtils = FakeTextPillsUtils()
    private val fakeThumbnailExtractor = FakeThumbnailExtractor()
    private val fakeWaveFormSanitizer = FakeWaveFormSanitizer()
    private val fakeLocalEchoRepository = FakeLocalEchoRepository()
    private val fakePermalinkFactory = FakePermalinkFactory()
    private val fakeClock = FakeClock()

    private val localEchoEventFactory = LocalEchoEventFactory(
            context = fakeContext.instance,
            userId = A_USER_ID_1,
            markdownParser = fakeMarkdownParser.instance,
            textPillsUtils = fakeTextPillsUtils.instance,
            thumbnailExtractor = fakeThumbnailExtractor.instance,
            waveformSanitizer = fakeWaveFormSanitizer.instance,
            localEchoRepository = fakeLocalEchoRepository.instance,
            permalinkFactory = fakePermalinkFactory.instance,
            clock = fakeClock
    )

    @Before
    fun setup() {
        fakeClock.givenEpoch(AN_EPOCH)
        fakeMarkdownParser.givenBoldMarkdown()
    }

    @Test
    fun `given a null quotedText, when a quote event is created, then the result message should only contain the new text after new lines`() {
        val event = createTimelineEvent(null, null)
        val quotedContent = localEchoEventFactory.createQuotedTextEvent(
                roomId = A_ROOM_ID,
                quotedEvent = event,
                text = "Text",
                formattedText = null,
                autoMarkdown = false,
                rootThreadEventId = null,
                additionalContent = null,
        ).content.toModel<MessageContent>()
        assertEquals("\n\nText", quotedContent?.body)
        assertEquals("<br/>Text", (quotedContent as? MessageContentWithFormattedBody)?.formattedBody)
    }

    @Test
    fun `given a plain text quoted message, when a quote event is created, then the result message should contain both the quoted and new text`() {
        val event = createTimelineEvent("Quoted", null)
        val quotedContent = localEchoEventFactory.createQuotedTextEvent(
                roomId = A_ROOM_ID,
                quotedEvent = event,
                text = "Text",
                formattedText = null,
                autoMarkdown = false,
                rootThreadEventId = null,
                additionalContent = null,
        ).content.toModel<MessageContent>()
        assertEquals("> Quoted\n\nText", quotedContent?.body)
        assertEquals("<blockquote>Quoted</blockquote><br/>Text", (quotedContent as? MessageContentWithFormattedBody)?.formattedBody)
    }

    @Test
    fun `given a formatted text quoted message, when a quote event is created, then the result message should contain both the formatted quote and new text`() {
        val event = createTimelineEvent("Quoted", "<b>Quoted</b>")
        val quotedContent = localEchoEventFactory.createQuotedTextEvent(
                roomId = A_ROOM_ID,
                quotedEvent = event,
                text = "Text",
                formattedText = null,
                autoMarkdown = false,
                rootThreadEventId = null,
                additionalContent = null,
        ).content.toModel<MessageContent>()
        // This still uses the plain text version
        assertEquals("> Quoted\n\nText", quotedContent?.body)
        // This one has the formatted one
        assertEquals("<blockquote><b>Quoted</b></blockquote><br/>Text", (quotedContent as? MessageContentWithFormattedBody)?.formattedBody)
    }

    @Test
    fun `given formatted text quoted message and new message, when a quote event is created, then the result message should contain both the formatted quote and new formatted text`() {
        val event = createTimelineEvent("Quoted", "<b>Quoted</b>")
        val quotedContent = localEchoEventFactory.createQuotedTextEvent(
                roomId = A_ROOM_ID,
                quotedEvent = event,
                text = "Text",
                formattedText = "<b>Formatted text</b>",
                autoMarkdown = false,
                rootThreadEventId = null,
                additionalContent = null,
        ).content.toModel<MessageContent>()
        // This still uses the plain text version
        assertEquals("> Quoted\n\nText", quotedContent?.body)
        // This one has the formatted one
        assertEquals(
                "<blockquote><b>Quoted</b></blockquote><br/><b>Formatted text</b>",
                (quotedContent as? MessageContentWithFormattedBody)?.formattedBody
        )
    }

    @Test
    fun `given formatted text quoted message and new message with autoMarkdown, when a quote event is created, then the result message should contain both the formatted quote and new formatted text, not the markdown processed text`() {
        val event = createTimelineEvent("Quoted", "<b>Quoted</b>")
        val quotedContent = localEchoEventFactory.createQuotedTextEvent(
                roomId = A_ROOM_ID,
                quotedEvent = event,
                text = "Text",
                formattedText = "<b>Formatted text</b>",
                autoMarkdown = true,
                rootThreadEventId = null,
                additionalContent = null,
        ).content.toModel<MessageContent>()
        // This still uses the plain text version
        assertEquals("> Quoted\n\nText", quotedContent?.body)
        // This one has the formatted one
        assertEquals(
                "<blockquote><b>Quoted</b></blockquote><br/><b>Formatted text</b>",
                (quotedContent as? MessageContentWithFormattedBody)?.formattedBody
        )
    }

    @Test
    fun `given a formatted text quoted message and a new message with autoMarkdown, when a quote event is created, then the result message should contain both the formatted quote and new processed formatted text`() {
        val event = createTimelineEvent("Quoted", "<b>Quoted</b>")
        val quotedContent = localEchoEventFactory.createQuotedTextEvent(
                roomId = A_ROOM_ID,
                quotedEvent = event,
                text = "**Text**",
                formattedText = null,
                autoMarkdown = true,
                rootThreadEventId = null,
                additionalContent = null,
        ).content.toModel<MessageContent>()
        // This still uses the markdown text version
        assertEquals("> Quoted\n\n**Text**", quotedContent?.body)
        // This one has the formatted one
        assertEquals(
                "<blockquote><b>Quoted</b></blockquote><br/><b>Text</b>",
                (quotedContent as? MessageContentWithFormattedBody)?.formattedBody
        )
    }

    @Test
    fun `given a plain text quoted message and a new message with autoMarkdown, when a quote event is created, then the result message should the plain text quote and new processed formatted text`() {
        val event = createTimelineEvent("Quoted", null)
        val quotedContent = localEchoEventFactory.createQuotedTextEvent(
                roomId = A_ROOM_ID,
                quotedEvent = event,
                text = "**Text**",
                formattedText = null,
                autoMarkdown = true,
                rootThreadEventId = null,
                additionalContent = null,
        ).content.toModel<MessageContent>()
        // This still uses the markdown text version
        assertEquals("> Quoted\n\n**Text**", quotedContent?.body)
        // This one has the formatted one
        assertEquals(
                "<blockquote>Quoted</blockquote><br/><b>Text</b>",
                (quotedContent as? MessageContentWithFormattedBody)?.formattedBody
        )
    }

    private fun createTimelineEvent(quotedText: String?, formattedQuotedText: String?): TimelineEvent {
        val textContent = quotedText?.let {
            TextContent(
                    quotedText,
                    formattedQuotedText
            ).toMessageTextContent().toContent()
        }
        return TimelineEvent(
                root = A_START_EVENT.copy(
                        type = EventType.MESSAGE,
                        content = textContent
                ),
                localId = 1234,
                eventId = AN_EVENT_ID,
                displayIndex = 0,
                senderInfo = SenderInfo(A_USER_ID_1, A_USER_ID_1, true, null),
        )
    }
}
