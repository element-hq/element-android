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
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.permalinks.PermalinkFactory
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.*
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.model.relation.ReactionContent
import im.vector.matrix.android.api.session.room.model.relation.ReactionInfo
import im.vector.matrix.android.api.session.room.model.relation.RelationDefaultContent
import im.vector.matrix.android.api.session.room.model.relation.ReplyToContent
import im.vector.matrix.android.internal.database.helper.addSendingEvent
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.content.ThumbnailExtractor
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.util.StringProvider
import im.vector.matrix.android.internal.util.tryTransactionAsync
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
internal class LocalEchoEventFactory @Inject constructor(private val credentials: Credentials,
                                                         private val stringProvider: StringProvider,
                                                         private val roomSummaryUpdater: RoomSummaryUpdater) {

    fun createTextEvent(roomId: String, msgType: String, text: String, autoMarkdown: Boolean): Event {
        if (autoMarkdown && msgType == MessageType.MSGTYPE_TEXT) {
            val parser = Parser.builder().build()
            val document = parser.parse(text)
            val renderer = HtmlRenderer.builder().build()
            val htmlText = renderer.render(document)
            if (isFormattedTextPertinent(text, htmlText)) { //FIXME
                return createFormattedTextEvent(roomId, text, htmlText)
            }
        }
        val content = MessageTextContent(type = msgType, body = text)
        return createEvent(roomId, content)
    }

    private fun isFormattedTextPertinent(text: String, htmlText: String?) =
            text != htmlText && htmlText != "<p>$text</p>\n"

    fun createFormattedTextEvent(roomId: String, text: String, formattedText: String): Event {
        val content = MessageTextContent(
                type = MessageType.MSGTYPE_TEXT,
                format = MessageType.FORMAT_MATRIX_HTML,
                body = text,
                formattedBody = formattedText
        )
        return createEvent(roomId, content)
    }


    fun createReplaceTextEvent(roomId: String, targetEventId: String, newBodyText: String, newBodyAutoMarkdown: Boolean, msgType: String, compatibilityText: String): Event {

        var newContent = MessageTextContent(
                type = MessageType.MSGTYPE_TEXT,
                body = newBodyText
        )
        if (newBodyAutoMarkdown) {
            val parser = Parser.builder().build()
            val document = parser.parse(newBodyText)
            val renderer = HtmlRenderer.builder().build()
            val htmlText = renderer.render(document)
            if (isFormattedTextPertinent(newBodyText, htmlText)) {
                newContent = MessageTextContent(
                        type = MessageType.MSGTYPE_TEXT,
                        format = MessageType.FORMAT_MATRIX_HTML,
                        body = newBodyText,
                        formattedBody = htmlText
                )
            }
        }

        val content = MessageTextContent(
                type = msgType,
                body = compatibilityText,
                relatesTo = RelationDefaultContent(RelationType.REPLACE, targetEventId),
                newContent = newContent.toContent()
        )
        return createEvent(roomId, content)
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
                senderId = credentials.userId,
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
                        mimeType = attachment.mimeType ?: "audio/mpeg",
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
                        mimeType = attachment.mimeType ?: "application/octet-stream",
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
                senderId = credentials.userId,
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
        return "m.${UUID.randomUUID()}"
    }

    fun createReplyTextEvent(roomId: String, eventReplied: Event, replyText: String): Event? {
        //Fallbacks and event representation
        //TODO Add error/warning logs when any of this is null
        val permalink = PermalinkFactory.createPermalink(eventReplied) ?: return null
        val userId = eventReplied.senderId ?: return null
        val userLink = PermalinkFactory.createPermalink(userId) ?: return null
//        <mx-reply>
//            <blockquote>
//                <a href="https://matrix.to/#/!somewhere:domain.com/$event:domain.com">In reply to</a>
//                <a href="https://matrix.to/#/@alice:example.org">@alice:example.org</a>
//                <br />
//                <!-- This is where the related event's HTML would be. -->
//            </blockquote>
//        </mx-reply>
//        This is where the reply goes.
        val body = bodyForReply(eventReplied.content.toModel<MessageContent>())
        val replyFallbackTemplateFormatted = """
           <mx-reply><blockquote><a href="%s">${stringProvider.getString(R.string.message_reply_to_prefix)}</a><a href="%s">%s</a><br />%s</blockquote></mx-reply>%s
        """.trimIndent().format(permalink, userLink, userId, body.second ?: body.first, replyText)
//
//        > <@alice:example.org> This is the original body
//
//        This is where the reply goes
        val lines = body.first.split("\n")
        val plainTextBody = StringBuffer("><${userId}>")
        lines.firstOrNull()?.also { plainTextBody.append(" $it") }
        lines.forEachIndexed { index, s ->
            if (index > 0) {
                plainTextBody.append("\n>$s")
            }
        }
        plainTextBody.append("\n\n").append(replyText)

        val eventId = eventReplied.eventId ?: return null
        val content = MessageTextContent(
                type = MessageType.MSGTYPE_TEXT,
                format = MessageType.FORMAT_MATRIX_HTML,
                body = plainTextBody.toString(),
                formattedBody = replyFallbackTemplateFormatted,
                relatesTo = RelationDefaultContent(null, null, ReplyToContent(eventId))
        )
        return createEvent(roomId, content)
    }

    /**
     * Returns a pair of <Plain Text, Formatted Text?> used for the fallback event representation
     * in a reply message.
     */
    private fun bodyForReply(content: MessageContent?): Pair<String, String?> {
        when (content?.type) {
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE -> {
                //If we already have formatted body, return it?
                var formattedText: String? = null
                if (content is MessageTextContent) {
                    if (content.format == MessageType.FORMAT_MATRIX_HTML) {
                        formattedText = content.formattedBody
                    }
                }
                return content.body to formattedText
            }
            MessageType.MSGTYPE_FILE   -> return stringProvider.getString(R.string.reply_to_a_file) to null
            MessageType.MSGTYPE_AUDIO  -> return stringProvider.getString(R.string.reply_to_an_audio_file) to null
            MessageType.MSGTYPE_IMAGE  -> return stringProvider.getString(R.string.reply_to_an_image) to null
            MessageType.MSGTYPE_VIDEO  -> return stringProvider.getString(R.string.reply_to_a_video) to null
            else                       -> return (content?.body ?: "") to null

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
                senderId = credentials.userId,
                eventId = localID,
                type = EventType.REDACTION,
                redacts = eventId,
                content = reason?.let { mapOf("reason" to it).toContent() },
                unsignedData = UnsignedData(age = null, transactionId = localID)
        )
    }

    fun saveLocalEcho(monarchy: Monarchy, event: Event) {
        if (event.roomId == null) throw IllegalStateException("Your event should have a roomId")
        monarchy.tryTransactionAsync { realm ->
            val roomEntity = RoomEntity.where(realm, roomId = event.roomId).findFirst()
                    ?: return@tryTransactionAsync
            roomEntity.addSendingEvent(event)
            roomSummaryUpdater.update(realm, event.roomId)
        }
    }

}
