/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.send

import android.media.MediaMetadataRetriever
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.R
import im.vector.matrix.android.api.permalinks.PermalinkFactory
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.*
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.model.relation.ReactionContent
import im.vector.matrix.android.api.session.room.model.relation.ReactionInfo
import im.vector.matrix.android.api.session.room.model.relation.RelationDefaultContent
import im.vector.matrix.android.api.session.room.model.relation.ReplyToContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.getLastMessageContent
import im.vector.matrix.android.internal.database.helper.addSendingEvent
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.content.ThumbnailExtractor
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.util.StringProvider
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.util.*
import javax.inject.Inject

/**
 * Creates local echo of events for room events.
 * A local echo is an event that is persisted even if not yet sent to the server,
 * in an optimistic way (as if the server as responded immediately). Local echo are using a local id,
 * (the transaction ID), this id is used when receiving an event from a sync to check if this event
 * is matching an existing local echo.
 *
 * The transactionID is used as loc
 */
internal class LocalEchoEventFactory @Inject constructor(@UserId private val userId: String,
                                                         private val stringProvider: StringProvider,
                                                         private val roomSummaryUpdater: RoomSummaryUpdater) {
    // TODO Inject
    private val parser = Parser.builder().build()
    // TODO Inject
    private val renderer = HtmlRenderer.builder().build()

    fun createTextEvent(roomId: String, msgType: String, text: String, autoMarkdown: Boolean): Event {
        if (msgType == MessageType.MSGTYPE_TEXT) {
            return createFormattedTextEvent(roomId, createTextContent(text, autoMarkdown))
        }
        val content = MessageTextContent(type = msgType, body = text)
        return createEvent(roomId, content)
    }

    private fun createTextContent(text: String, autoMarkdown: Boolean): TextContent {
        if (autoMarkdown) {
            val document = parser.parse(text)
            val htmlText = renderer.render(document)

            if (isFormattedTextPertinent(text, htmlText)) {
                return TextContent(text, htmlText)
            }
        }

        return TextContent(text)
    }

    private fun isFormattedTextPertinent(text: String, htmlText: String?) =
            text != htmlText && htmlText != "<p>${text.trim()}</p>\n"

    fun createFormattedTextEvent(roomId: String, textContent: TextContent): Event {
        return createEvent(roomId, textContent.toMessageTextContent())
    }

    fun createReplaceTextEvent(roomId: String,
                               targetEventId: String,
                               newBodyText: String,
                               newBodyAutoMarkdown: Boolean,
                               msgType: String,
                               compatibilityText: String): Event {
        return createEvent(roomId,
                MessageTextContent(
                        type = msgType,
                        body = compatibilityText,
                        relatesTo = RelationDefaultContent(RelationType.REPLACE, targetEventId),
                        newContent = createTextContent(newBodyText, newBodyAutoMarkdown)
                                .toMessageTextContent(msgType)
                                .toContent()
                ))
    }

    fun createReplaceTextOfReply(roomId: String, eventReplaced: TimelineEvent,
                                 originalEvent: TimelineEvent,
                                 newBodyText: String,
                                 newBodyAutoMarkdown: Boolean,
                                 msgType: String,
                                 compatibilityText: String): Event {
        val permalink = PermalinkFactory.createPermalink(roomId, originalEvent.root.eventId ?: "")
        val userLink = originalEvent.root.senderId?.let { PermalinkFactory.createPermalink(it) }
                ?: ""

        val body = bodyForReply(originalEvent.getLastMessageContent(), originalEvent.root.getClearContent().toModel())
        val replyFormatted = REPLY_PATTERN.format(
                permalink,
                stringProvider.getString(R.string.message_reply_to_prefix),
                userLink,
                originalEvent.senderName ?: originalEvent.root.senderId,
                body.takeFormatted(),
                createTextContent(newBodyText, newBodyAutoMarkdown).takeFormatted()
        )
        //
        // > <@alice:example.org> This is the original body
        //
        val replyFallback = buildReplyFallback(body, originalEvent.root.senderId ?: "", newBodyText)

        return createEvent(roomId,
                MessageTextContent(
                        type = msgType,
                        body = compatibilityText,
                        relatesTo = RelationDefaultContent(RelationType.REPLACE, eventReplaced.root.eventId),
                        newContent = MessageTextContent(
                                type = msgType,
                                format = MessageType.FORMAT_MATRIX_HTML,
                                body = replyFallback,
                                formattedBody = replyFormatted
                        )
                                .toContent()
                ))
    }

    fun createMediaEvent(roomId: String, attachment: ContentAttachmentData): Event {
        return when (attachment.type) {
            ContentAttachmentData.Type.IMAGE -> createImageEvent(roomId, attachment)
            ContentAttachmentData.Type.VIDEO -> createVideoEvent(roomId, attachment)
            ContentAttachmentData.Type.AUDIO -> createAudioEvent(roomId, attachment)
            ContentAttachmentData.Type.FILE  -> createFileEvent(roomId, attachment)
        }
    }

    fun createReactionEvent(roomId: String, targetEventId: String, reaction: String): Event {
        val content = ReactionContent(
                ReactionInfo(
                        RelationType.ANNOTATION,
                        targetEventId,
                        reaction
                )
        )
        val localId = dummyEventId(roomId)
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.REACTION,
                content = content.toContent(),
                unsignedData = UnsignedData(age = null, transactionId = localId))

    }


    private fun createImageEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val content = MessageImageContent(
                type = MessageType.MSGTYPE_IMAGE,
                body = attachment.name ?: "image",
                info = ImageInfo(
                        mimeType = attachment.mimeType,
                        width = attachment.width?.toInt() ?: 0,
                        height = attachment.height?.toInt() ?: 0,
                        orientation = attachment.exifOrientation,
                        size = attachment.size.toInt()
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createVideoEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val mediaDataRetriever = MediaMetadataRetriever()
        mediaDataRetriever.setDataSource(attachment.path)

        // Use frame to calculate height and width as we are sure to get the right ones
        val firstFrame = mediaDataRetriever.frameAtTime
        val height = firstFrame.height
        val width = firstFrame.width
        mediaDataRetriever.release()

        val thumbnailInfo = ThumbnailExtractor.extractThumbnail(attachment)?.let {
            ThumbnailInfo(
                    width = it.width,
                    height = it.height,
                    size = it.size,
                    mimeType = it.mimeType
            )
        }
        val content = MessageVideoContent(
                type = MessageType.MSGTYPE_VIDEO,
                body = attachment.name ?: "video",
                videoInfo = VideoInfo(
                        mimeType = attachment.mimeType,
                        width = width,
                        height = height,
                        size = attachment.size,
                        duration = attachment.duration?.toInt() ?: 0,
                        // Glide will be able to use the local path and extract a thumbnail.
                        thumbnailUrl = attachment.path,
                        thumbnailInfo = thumbnailInfo
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createAudioEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val content = MessageAudioContent(
                type = MessageType.MSGTYPE_AUDIO,
                body = attachment.name ?: "audio",
                audioInfo = AudioInfo(
                        mimeType = attachment.mimeType.takeIf { it.isNotBlank() } ?: "audio/mpeg",
                        size = attachment.size
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createFileEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val content = MessageFileContent(
                type = MessageType.MSGTYPE_FILE,
                body = attachment.name ?: "file",
                info = FileInfo(
                        mimeType = attachment.mimeType.takeIf { it.isNotBlank() }
                                ?: "application/octet-stream",
                        size = attachment.size
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createEvent(roomId: String, content: Any? = null): Event {
        val localID = dummyEventId(roomId)
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localID,
                type = EventType.MESSAGE,
                content = content.toContent(),
                unsignedData = UnsignedData(age = null, transactionId = localID)
        )
    }

    private fun dummyOriginServerTs(): Long {
        return System.currentTimeMillis()
    }

    private fun dummyEventId(roomId: String): String {
        return "$LOCAL_ID_PREFIX${UUID.randomUUID()}"
    }

    fun createReplyTextEvent(roomId: String, eventReplied: TimelineEvent, replyText: String, autoMarkdown: Boolean): Event? {
        //Fallbacks and event representation
        //TODO Add error/warning logs when any of this is null
        val permalink = PermalinkFactory.createPermalink(eventReplied.root) ?: return null
        val userId = eventReplied.root.senderId ?: return null
        val userLink = PermalinkFactory.createPermalink(userId) ?: return null

        val body = bodyForReply(eventReplied.getLastMessageContent(), eventReplied.root.getClearContent().toModel())
        val replyFormatted = REPLY_PATTERN.format(
                permalink,
                stringProvider.getString(R.string.message_reply_to_prefix),
                userLink,
                userId,
                body.takeFormatted(),
                createTextContent(replyText, autoMarkdown).takeFormatted()
        )
        //
        // > <@alice:example.org> This is the original body
        //
        val replyFallback = buildReplyFallback(body, userId, replyText)

        val eventId = eventReplied.root.eventId ?: return null
        val content = MessageTextContent(
                type = MessageType.MSGTYPE_TEXT,
                format = MessageType.FORMAT_MATRIX_HTML,
                body = replyFallback,
                formattedBody = replyFormatted,
                relatesTo = RelationDefaultContent(null, null, ReplyToContent(eventId))
        )
        return createEvent(roomId, content)
    }

    private fun buildReplyFallback(body: TextContent, originalSenderId: String?, newBodyText: String): String {
        return buildString {
            append("> <")
            append(originalSenderId)
            append(">")

            val lines = body.text.split("\n")
            lines.forEachIndexed { index, s ->
                if (index == 0) {
                    append(" $s")
                } else {
                    append("\n> $s")
                }
            }
            append("\n\n")
            append(newBodyText)
        }
    }

    /**
     * Returns a TextContent used for the fallback event representation in a reply message.
     * We also pass the original content, because in case of an edit of a reply the last content is not
     * himself a reply, but it will contain the fallbacks, so we have to trim them.
     */
    private fun bodyForReply(content: MessageContent?, originalContent: MessageContent?): TextContent {
        when (content?.type) {
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE -> {
                var formattedText: String? = null
                if (content is MessageTextContent) {
                    if (content.format == MessageType.FORMAT_MATRIX_HTML) {
                        formattedText = content.formattedBody
                    }
                }
                val isReply = content.isReply() || originalContent.isReply()
                return if (isReply)
                    TextContent(content.body, formattedText).removeInReplyFallbacks()
                else
                    TextContent(content.body, formattedText)
            }
            MessageType.MSGTYPE_FILE   -> return TextContent(stringProvider.getString(R.string.reply_to_a_file))
            MessageType.MSGTYPE_AUDIO  -> return TextContent(stringProvider.getString(R.string.reply_to_an_audio_file))
            MessageType.MSGTYPE_IMAGE  -> return TextContent(stringProvider.getString(R.string.reply_to_an_image))
            MessageType.MSGTYPE_VIDEO  -> return TextContent(stringProvider.getString(R.string.reply_to_a_video))
            else                       -> return TextContent(content?.body ?: "")
        }
    }

    /*
     * {
        "content": {
            "reason": "Spamming"
            },
            "event_id": "$143273582443PhrSn:domain.com",
            "origin_server_ts": 1432735824653,
            "redacts": "$fukweghifu23:localhost",
            "room_id": "!jEsUZKDJdhlrceRyVU:domain.com",
            "sender": "@example:domain.com",
            "type": "m.room.redaction",
            "unsigned": {
            "age": 1234
        }
    }
     */
    fun createRedactEvent(roomId: String, eventId: String, reason: String?): Event {
        val localID = dummyEventId(roomId)
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localID,
                type = EventType.REDACTION,
                redacts = eventId,
                content = reason?.let { mapOf("reason" to it).toContent() },
                unsignedData = UnsignedData(age = null, transactionId = localID)
        )
    }

    fun saveLocalEcho(monarchy: Monarchy, event: Event) {
        if (event.roomId == null) throw IllegalStateException("Your event should have a roomId")
        monarchy.writeAsync { realm ->
            val roomEntity = RoomEntity.where(realm, roomId = event.roomId).findFirst()
                    ?: return@writeAsync
            roomEntity.addSendingEvent(event)
            roomSummaryUpdater.update(realm, event.roomId)
        }
    }

    companion object {
        const val LOCAL_ID_PREFIX = "local."


        // <mx-reply>
        //     <blockquote>
        //         <a href="https://matrix.to/#/!somewhere:domain.com/$event:domain.com">In reply to</a>
        //         <a href="https://matrix.to/#/@alice:example.org">@alice:example.org</a>
        //         <br />
        //         <!-- This is where the related event's HTML would be. -->
        //     </blockquote>
        // </mx-reply>
        // No whitespace because currently breaks temporary formatted text to Span
        const val REPLY_PATTERN = """<mx-reply><blockquote><a href="%s">%s</a><a href="%s">%s</a><br />%s</blockquote></mx-reply>%s"""

        fun isLocalEchoId(eventId: String): Boolean = eventId.startsWith(LOCAL_ID_PREFIX)
    }
}
