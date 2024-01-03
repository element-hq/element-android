/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.send

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import org.matrix.android.sdk.api.extensions.ensureNotEmpty
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.AudioInfo
import org.matrix.android.sdk.api.session.room.model.message.AudioWaveformInfo
import org.matrix.android.sdk.api.session.room.model.message.FileInfo
import org.matrix.android.sdk.api.session.room.model.message.ImageInfo
import org.matrix.android.sdk.api.session.room.model.message.LocationAsset
import org.matrix.android.sdk.api.session.room.model.message.LocationAssetType
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContentWithFormattedBody
import org.matrix.android.sdk.api.session.room.model.message.MessageEndPollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFileContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageLocationContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollResponseContent
import org.matrix.android.sdk.api.session.room.model.message.MessageStickerContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.PollAnswer
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.model.message.PollResponse
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.model.message.ThumbnailInfo
import org.matrix.android.sdk.api.session.room.model.message.VideoInfo
import org.matrix.android.sdk.api.session.room.model.relation.ReactionContent
import org.matrix.android.sdk.api.session.room.model.relation.ReactionInfo
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.model.relation.ReplyToContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.isReply
import org.matrix.android.sdk.api.util.TextContent
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.content.ThumbnailExtractor
import org.matrix.android.sdk.internal.session.permalinks.PermalinkFactory
import org.matrix.android.sdk.internal.session.room.send.model.EventRedactBody
import org.matrix.android.sdk.internal.session.room.send.pills.TextPillsUtils
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Creates local echo of events for room events.
 * A local echo is an event that is persisted even if not yet sent to the server,
 * in an optimistic way (as if the server as responded immediately). Local echo are using a local id,
 * (the transaction ID), this id is used when receiving an event from a sync to check if this event
 * is matching an existing local echo.
 *
 * The transactionId is used as loc
 */
internal class LocalEchoEventFactory @Inject constructor(
        private val context: Context,
        @UserId private val userId: String,
        private val markdownParser: MarkdownParser,
        private val textPillsUtils: TextPillsUtils,
        private val thumbnailExtractor: ThumbnailExtractor,
        private val waveformSanitizer: WaveFormSanitizer,
        private val localEchoRepository: LocalEchoRepository,
        private val permalinkFactory: PermalinkFactory,
        private val clock: Clock,
) {
    fun createTextEvent(roomId: String, msgType: String, text: CharSequence, autoMarkdown: Boolean, additionalContent: Content? = null): Event {
        if (msgType == MessageType.MSGTYPE_TEXT || msgType == MessageType.MSGTYPE_EMOTE) {
            return createFormattedTextEvent(roomId, createTextContent(text, autoMarkdown), msgType, additionalContent)
        }
        val content = MessageTextContent(msgType = msgType, body = text.toString())
        return createMessageEvent(roomId, content, additionalContent)
    }

    private fun createTextContent(text: CharSequence, autoMarkdown: Boolean): TextContent {
        if (autoMarkdown) {
            return markdownParser.parse(text)
        } else {
            // Try to detect pills
            textPillsUtils.processSpecialSpansToHtml(text)?.let {
                return TextContent(text.toString(), it)
            }
        }

        return TextContent(text.toString())
    }

    fun createFormattedTextEvent(roomId: String, textContent: TextContent, msgType: String, additionalContent: Content? = null): Event {
        return createMessageEvent(roomId, textContent.toMessageTextContent(msgType), additionalContent)
    }

    fun createReplaceTextEvent(
            roomId: String,
            targetEventId: String,
            newBodyText: CharSequence,
            newBodyFormattedText: CharSequence?,
            newBodyAutoMarkdown: Boolean,
            msgType: String,
            compatibilityText: String,
            additionalContent: Content? = null,
    ): Event {
        val content = if (newBodyFormattedText != null) {
            TextContent(newBodyText.toString(), newBodyFormattedText.toString()).toMessageTextContent(msgType)
        } else {
            createTextContent(newBodyText, newBodyAutoMarkdown).toMessageTextContent(msgType)
        }.toContent()
        return createMessageEvent(
                roomId,
                MessageTextContent(
                        msgType = msgType,
                        body = compatibilityText,
                        relatesTo = RelationDefaultContent(RelationType.REPLACE, targetEventId),
                        newContent = content,
                ),
                additionalContent,
        )
    }

    private fun createPollContent(
            question: String,
            options: List<String>,
            pollType: PollType,
    ): MessagePollContent {
        return MessagePollContent(
                unstablePollCreationInfo = PollCreationInfo(
                        question = PollQuestion(unstableQuestion = question),
                        kind = pollType,
                        answers = options.map { option ->
                            PollAnswer(id = UUID.randomUUID().toString(), unstableAnswer = option)
                        }
                )
        )
    }

    fun createPollReplaceEvent(
            roomId: String,
            pollType: PollType,
            targetEventId: String,
            question: String,
            options: List<String>,
            additionalContent: Content? = null,
    ): Event {
        val newContent = MessagePollContent(
                relatesTo = RelationDefaultContent(RelationType.REPLACE, targetEventId),
                newContent = createPollContent(question, options, pollType).toContent()
        )
        val localId = LocalEcho.createLocalEchoId()
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.POLL_START.unstable,
                content = newContent.toContent().plus(additionalContent.orEmpty())
        )
    }

    fun createPollReplyEvent(
            roomId: String,
            pollEventId: String,
            answerId: String,
            additionalContent: Content? = null,
    ): Event {
        val content = MessagePollResponseContent(
                body = answerId,
                relatesTo = RelationDefaultContent(
                        type = RelationType.REFERENCE,
                        eventId = pollEventId
                ),
                unstableResponse = PollResponse(answers = listOf(answerId))
        )
        val localId = LocalEcho.createLocalEchoId()
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.POLL_RESPONSE.unstable,
                content = content.toContent().plus(additionalContent.orEmpty()),
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
    }

    fun createPollEvent(
            roomId: String,
            pollType: PollType,
            question: String,
            options: List<String>,
            additionalContent: Content? = null,
    ): Event {
        val content = createPollContent(question, options, pollType)
        val localId = LocalEcho.createLocalEchoId()
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.POLL_START.unstable,
                content = content.toContent().plus(additionalContent.orEmpty()),
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
    }

    fun createEndPollEvent(
            roomId: String,
            eventId: String,
            additionalContent: Content? = null,
    ): Event {
        val content = MessageEndPollContent(
                relatesTo = RelationDefaultContent(
                        type = RelationType.REFERENCE,
                        eventId = eventId
                ),
                unstableText = "Ended poll",
        )
        val localId = LocalEcho.createLocalEchoId()
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.POLL_END.unstable,
                content = content.toContent().plus(additionalContent.orEmpty()),
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
    }

    fun createStaticLocationEvent(
            roomId: String,
            latitude: Double,
            longitude: Double,
            uncertainty: Double?,
            isUserLocation: Boolean,
            additionalContent: Content? = null,
    ): Event {
        val geoUri = buildGeoUri(latitude, longitude, uncertainty)
        val assetType = if (isUserLocation) LocationAssetType.SELF else LocationAssetType.PIN
        val content = MessageLocationContent(
                geoUri = geoUri,
                body = geoUri,
                unstableLocationInfo = LocationInfo(geoUri = geoUri, description = geoUri),
                unstableLocationAsset = LocationAsset(type = assetType),
                unstableTimestampMillis = clock.epochMillis(),
                unstableText = geoUri
        )
        return createMessageEvent(roomId, content, additionalContent)
    }

    fun createLiveLocationEvent(
            beaconInfoEventId: String,
            roomId: String,
            latitude: Double,
            longitude: Double,
            uncertainty: Double?,
            additionalContent: Content? = null,
    ): Event {
        val geoUri = buildGeoUri(latitude, longitude, uncertainty)
        val content = MessageBeaconLocationDataContent(
                body = geoUri,
                relatesTo = RelationDefaultContent(
                        type = RelationType.REFERENCE,
                        eventId = beaconInfoEventId
                ),
                unstableLocationInfo = LocationInfo(geoUri = geoUri, description = geoUri),
                unstableTimestampMillis = clock.epochMillis(),
        )
        val localId = LocalEcho.createLocalEchoId()
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.BEACON_LOCATION_DATA.unstable,
                content = content.toContent().plus(additionalContent.orEmpty()),
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
    }

    fun createReplaceTextOfReply(
            roomId: String,
            eventReplaced: TimelineEvent,
            originalEvent: TimelineEvent,
            replyText: CharSequence,
            replyTextFormatted: String?,
            autoMarkdown: Boolean,
            msgType: String,
            compatibilityText: String,
            additionalContent: Content? = null,
    ): Event {
        val permalink = permalinkFactory.createPermalink(roomId, originalEvent.root.eventId ?: "", false)
        val userLink = originalEvent.root.senderId?.let { permalinkFactory.createPermalink(it, false) } ?: ""

        val bodyOfRepliedEvent = bodyForReply(timelineEvent = originalEvent)
        // As we always supply formatted body for replies we should force the MarkdownParser to produce html.
        val newBodyFormatted = replyTextFormatted ?: markdownParser.parse(replyText, force = true, advanced = autoMarkdown).takeFormatted()
        // Body of the original message may not have formatted version, so may also have to convert to html.
        val formattedBodyOfRepliedEvent =
                bodyOfRepliedEvent.formattedText ?: markdownParser.parse(text = bodyOfRepliedEvent.text, force = true, advanced = autoMarkdown).takeFormatted()
        val replyFormatted = buildFormattedReply(
                permalink,
                userLink,
                originalEvent.senderInfo.disambiguatedDisplayName,
                formattedBodyOfRepliedEvent,
                newBodyFormatted
        )
        //
        // > <@alice:example.org> This is the original body
        //
        val replyFallback = buildReplyFallback(bodyOfRepliedEvent, originalEvent.root.senderId ?: "", replyText.toString())

        return createMessageEvent(
                roomId,
                MessageTextContent(
                        msgType = msgType,
                        body = compatibilityText,
                        relatesTo = RelationDefaultContent(RelationType.REPLACE, eventReplaced.root.eventId),
                        newContent = MessageTextContent(
                                msgType = msgType,
                                format = MessageFormat.FORMAT_MATRIX_HTML,
                                body = replyFallback,
                                formattedBody = replyFormatted
                        )
                                .toContent()
                ),
                additionalContent,
        )
    }

    fun createMediaEvent(
            roomId: String,
            attachment: ContentAttachmentData,
            rootThreadEventId: String?,
            relatesTo: RelationDefaultContent?,
            additionalContent: Content? = null,
    ): Event {
        return when (attachment.type) {
            ContentAttachmentData.Type.IMAGE -> createImageEvent(roomId, attachment, rootThreadEventId, relatesTo, additionalContent)
            ContentAttachmentData.Type.VIDEO -> createVideoEvent(roomId, attachment, rootThreadEventId, relatesTo, additionalContent)
            ContentAttachmentData.Type.AUDIO -> createAudioEvent(
                    roomId,
                    attachment,
                    isVoiceMessage = false,
                    rootThreadEventId = rootThreadEventId,
                    relatesTo,
                    additionalContent
            )
            ContentAttachmentData.Type.VOICE_MESSAGE -> createAudioEvent(
                    roomId,
                    attachment,
                    isVoiceMessage = true,
                    rootThreadEventId = rootThreadEventId,
                    relatesTo,
                    additionalContent,
            )
            ContentAttachmentData.Type.FILE -> createFileEvent(roomId, attachment, rootThreadEventId, relatesTo, additionalContent)
        }
    }

    fun createReactionEvent(roomId: String, targetEventId: String, reaction: String, additionalContent: Content? = null): Event {
        val content = ReactionContent(
                ReactionInfo(
                        RelationType.ANNOTATION,
                        targetEventId,
                        reaction
                )
        )
        val localId = LocalEcho.createLocalEchoId()
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.REACTION,
                content = content.toContent().plus(additionalContent.orEmpty()),
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
    }

    private fun createImageEvent(
            roomId: String,
            attachment: ContentAttachmentData,
            rootThreadEventId: String?,
            relatesTo: RelationDefaultContent?,
            additionalContent: Content?,
    ): Event {
        var width = attachment.width
        var height = attachment.height

        when (attachment.exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                val tmp = width
                width = height
                height = tmp
            }
        }

        val content = MessageImageContent(
                msgType = MessageType.MSGTYPE_IMAGE,
                body = attachment.name ?: "image",
                info = ImageInfo(
                        mimeType = attachment.getSafeMimeType(),
                        width = width?.toInt() ?: 0,
                        height = height?.toInt() ?: 0,
                        size = attachment.size
                ),
                url = attachment.queryUri.toString(),
                relatesTo = relatesTo ?: rootThreadEventId?.let { generateThreadRelationContent(it) }
        )
        return createMessageEvent(roomId, content, additionalContent)
    }

    private fun createVideoEvent(
            roomId: String,
            attachment: ContentAttachmentData,
            rootThreadEventId: String?,
            relatesTo: RelationDefaultContent?,
            additionalContent: Content?,
    ): Event {
        val mediaDataRetriever = MediaMetadataRetriever()
        mediaDataRetriever.setDataSource(context, attachment.queryUri)

        // Use frame to calculate height and width as we are sure to get the right ones
        val firstFrame: Bitmap? = mediaDataRetriever.frameAtTime
        val height = firstFrame?.height ?: 0
        val width = firstFrame?.width ?: 0
        mediaDataRetriever.release()

        val thumbnailInfo = thumbnailExtractor.extractThumbnail(attachment)?.let {
            ThumbnailInfo(
                    width = it.width,
                    height = it.height,
                    size = it.size,
                    mimeType = it.mimeType
            )
        }
        val content = MessageVideoContent(
                msgType = MessageType.MSGTYPE_VIDEO,
                body = attachment.name ?: "video",
                videoInfo = VideoInfo(
                        mimeType = attachment.getSafeMimeType(),
                        width = width,
                        height = height,
                        size = attachment.size,
                        duration = attachment.duration?.toInt() ?: 0,
                        // Glide will be able to use the local path and extract a thumbnail.
                        thumbnailUrl = attachment.queryUri.toString(),
                        thumbnailInfo = thumbnailInfo
                ),
                url = attachment.queryUri.toString(),
                relatesTo = relatesTo ?: rootThreadEventId?.let { generateThreadRelationContent(it) }
        )
        return createMessageEvent(roomId, content, additionalContent)
    }

    private fun createAudioEvent(
            roomId: String,
            attachment: ContentAttachmentData,
            isVoiceMessage: Boolean,
            rootThreadEventId: String?,
            relatesTo: RelationDefaultContent?,
            additionalContent: Content?
    ): Event {
        val content = MessageAudioContent(
                msgType = MessageType.MSGTYPE_AUDIO,
                body = attachment.name ?: "audio",
                audioInfo = AudioInfo(
                        duration = attachment.duration?.toInt(),
                        mimeType = attachment.getSafeMimeType()?.takeIf { it.isNotBlank() },
                        size = attachment.size
                ),
                url = attachment.queryUri.toString(),
                audioWaveformInfo = if (!isVoiceMessage) null else AudioWaveformInfo(
                        duration = attachment.duration?.toInt(),
                        waveform = waveformSanitizer.sanitize(attachment.waveform)
                ),
                voiceMessageIndicator = if (!isVoiceMessage) null else emptyMap(),
                relatesTo = relatesTo ?: rootThreadEventId?.let { generateThreadRelationContent(it) }
        )
        return createMessageEvent(roomId, content, additionalContent)
    }

    private fun createFileEvent(
            roomId: String,
            attachment: ContentAttachmentData,
            rootThreadEventId: String?,
            relatesTo: RelationDefaultContent?,
            additionalContent: Content?
    ): Event {
        val content = MessageFileContent(
                msgType = MessageType.MSGTYPE_FILE,
                body = attachment.name ?: "file",
                info = FileInfo(
                        mimeType = attachment.getSafeMimeType()?.takeIf { it.isNotBlank() },
                        size = attachment.size
                ),
                url = attachment.queryUri.toString(),
                relatesTo = relatesTo ?: rootThreadEventId?.let { generateThreadRelationContent(it) }
        )
        return createMessageEvent(roomId, content, additionalContent)
    }

    private fun createMessageEvent(roomId: String, content: MessageContent, additionalContent: Content?): Event {
        return createEvent(roomId, EventType.MESSAGE, content.toContent(), additionalContent)
    }

    fun createEvent(roomId: String, type: String, content: Content?, additionalContent: Content? = null): Event {
        val newContent = enhanceStickerIfNeeded(type, content) ?: content
        val updatedNewContent = newContent?.plus(additionalContent.orEmpty()) ?: additionalContent
        val localId = LocalEcho.createLocalEchoId()
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = type,
                content = updatedNewContent,
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
    }

    /**
     * Enhance sticker to support threads fallback if needed.
     */
    private fun enhanceStickerIfNeeded(type: String, content: Content?): Content? {
        var newContent: Content? = null
        if (type == EventType.STICKER) {
            val isThread = (content.toModel<MessageStickerContent>())?.relatesTo?.type == RelationType.THREAD
            val rootThreadEventId = (content.toModel<MessageStickerContent>())?.relatesTo?.eventId
            if (isThread && rootThreadEventId != null) {
                val newRelationalDefaultContent = (content.toModel<MessageStickerContent>())?.relatesTo?.copy(
                        inReplyTo = ReplyToContent(eventId = localEchoRepository.getLatestThreadEvent(rootThreadEventId))
                )
                newContent = (content.toModel<MessageStickerContent>())?.copy(
                        relatesTo = newRelationalDefaultContent
                ).toContent()
            }
        }
        return newContent
    }

    /**
     * Creates a thread event related to the already existing root event.
     */
    fun createThreadTextEvent(
            rootThreadEventId: String,
            roomId: String,
            text: CharSequence,
            msgType: String,
            autoMarkdown: Boolean,
            formattedText: String?,
            additionalContent: Content? = null,
    ): Event {
        val content = formattedText?.let { TextContent(text.toString(), it) } ?: createTextContent(text, autoMarkdown)
        return createEvent(
                roomId,
                EventType.MESSAGE,
                content.toThreadTextContent(
                        rootThreadEventId = rootThreadEventId,
                        latestThreadEventId = localEchoRepository.getLatestThreadEvent(rootThreadEventId),
                        msgType = msgType
                ).toContent().plus(additionalContent.orEmpty())
        )
    }

    private fun dummyOriginServerTs(): Long {
        return clock.epochMillis()
    }

    fun createReplyTextContent(
            eventReplied: TimelineEvent,
            replyText: CharSequence,
            replyTextFormatted: CharSequence?,
            autoMarkdown: Boolean,
            rootThreadEventId: String? = null,
            showInThread: Boolean,
            isRedactedEvent: Boolean = false
    ): MessageContent? {
        // Fallbacks and event representation
        // TODO Add error/warning logs when any of this is null
        val permalink = permalinkFactory.createPermalink(eventReplied.root, false) ?: return null
        val userId = eventReplied.root.senderId ?: return null
        val userLink = permalinkFactory.createPermalink(userId, false) ?: return null

        val body = bodyForReply(timelineEvent = eventReplied, isRedactedEvent = isRedactedEvent)

        // As we always supply formatted body for replies we should force the MarkdownParser to produce html.
        val finalReplyTextFormatted = replyTextFormatted?.toString() ?: markdownParser.parse(replyText, force = true, advanced = autoMarkdown).takeFormatted()
        // Body of the original message may not have formatted version, so may also have to convert to html.
        val bodyFormatted = body.formattedText ?: markdownParser.parse(body.text, force = true, advanced = autoMarkdown).takeFormatted()
        val replyFormatted = buildFormattedReply(
                permalink,
                userLink,
                userId,
                bodyFormatted,
                finalReplyTextFormatted
        )
        //
        // > <@alice:example.org> This is the original body
        //
        val replyFallback = buildReplyFallback(body, userId, replyText.toString())

        val eventId = eventReplied.root.eventId ?: return null
        return MessageTextContent(
                msgType = MessageType.MSGTYPE_TEXT,
                format = MessageFormat.FORMAT_MATRIX_HTML,
                body = replyFallback,
                formattedBody = replyFormatted,
                relatesTo = generateReplyRelationContent(
                        eventId = eventId,
                        rootThreadEventId = rootThreadEventId,
                        showInThread = showInThread
                )
        )
    }

    /**
     * Creates a reply to a regular timeline Event or a thread Event if needed.
     */
    fun createReplyTextEvent(
            roomId: String,
            eventReplied: TimelineEvent,
            replyText: CharSequence,
            replyTextFormatted: CharSequence?,
            autoMarkdown: Boolean,
            rootThreadEventId: String? = null,
            showInThread: Boolean,
            additionalContent: Content? = null,
    ): Event? {
        val content = createReplyTextContent(eventReplied, replyText, replyTextFormatted, autoMarkdown, rootThreadEventId, showInThread)
        return content?.let {
            createMessageEvent(roomId, it, additionalContent)
        }
    }

    private fun generateThreadRelationContent(rootThreadEventId: String) =
            RelationDefaultContent(
                    type = RelationType.THREAD,
                    eventId = rootThreadEventId,
                    isFallingBack = true,
                    inReplyTo = ReplyToContent(eventId = localEchoRepository.getLatestThreadEvent(rootThreadEventId)),
            )

    /**
     * Generates the appropriate relatesTo object for a reply event.
     * It can either be a regular reply or a reply within a thread
     * "m.relates_to": {
     *      "rel_type": "m.thread",
     *      "event_id": "$thread_root",
     *      "is_falling_back": false,
     *      "m.in_reply_to": {
     *          "event_id": "$event_target"
     *      }
     *  }
     */
    private fun generateReplyRelationContent(eventId: String, rootThreadEventId: String? = null, showInThread: Boolean): RelationDefaultContent =
            rootThreadEventId?.let {
                RelationDefaultContent(
                        type = RelationType.THREAD,
                        eventId = it,
                        isFallingBack = showInThread,
                        // False when is a rich reply from within a thread, and true when is a reply that should be visible from threads
                        inReplyTo = ReplyToContent(eventId = eventId)
                )
            } ?: RelationDefaultContent(null, null, ReplyToContent(eventId = eventId))

    private fun buildFormattedReply(permalink: String, userLink: String, userId: String, bodyFormatted: String, newBodyFormatted: String): String {
        return REPLY_PATTERN.format(
                permalink,
                userLink,
                userId,
                // Remove inner mx_reply tags if any
                bodyFormatted.replace(MX_REPLY_REGEX, ""),
                newBodyFormatted
        )
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

    private fun bodyForReply(timelineEvent: TimelineEvent, isRedactedEvent: Boolean = false): TextContent {
        val content = when (timelineEvent.root.getClearType()) {
            in EventType.POLL_END.values -> {
                // for end poll event, we use the content of the start poll event
                localEchoRepository
                        .getRelatedPollEvent(timelineEvent)
                        ?.getLastMessageContent()
                        ?: timelineEvent.getLastMessageContent()
            }
            else -> timelineEvent.getLastMessageContent()
        }
        return bodyForReply(content = content, isReply = timelineEvent.isReply(), isRedactedEvent = isRedactedEvent)
    }

    /**
     * Returns a TextContent used for the fallback event representation in a reply message.
     * In case of an edit of a reply the last content is not
     * himself a reply, but it will contain the fallbacks, so we have to trim them.
     */
    fun bodyForReply(content: MessageContent?, isReply: Boolean, isRedactedEvent: Boolean = false): TextContent {
        when (content?.msgType) {
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE -> {
                var formattedText: String? = null
                if (content is MessageContentWithFormattedBody) {
                    formattedText = content.matrixFormattedBody
                }
                return if (isRedactedEvent) {
                    TextContent("message removed.")
                } else if (isReply) {
                    TextContent(content.body, formattedText).removeInReplyFallbacks()
                } else {
                    TextContent(content.body, formattedText)
                }
            }
            MessageType.MSGTYPE_FILE -> return TextContent("sent a file.")
            MessageType.MSGTYPE_AUDIO -> return TextContent("sent an audio file.")
            MessageType.MSGTYPE_IMAGE -> return TextContent("sent an image.")
            MessageType.MSGTYPE_VIDEO -> return TextContent("sent a video.")
            MessageType.MSGTYPE_BEACON_INFO -> return TextContent(content.body.ensureNotEmpty() ?: "Live location")
            MessageType.MSGTYPE_POLL_START -> {
                return TextContent((content as? MessagePollContent)?.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "")
            }
            MessageType.MSGTYPE_POLL_END -> return TextContent("Ended poll")
            else -> {
                return if (isRedactedEvent) {
                    TextContent("message removed.")
                } else TextContent(content?.body ?: "")
            }
        }
    }

    /**
     * Returns RFC5870 formatted geo uri 'geo:latitude,longitude;u=uncertainty' like 'geo:40.05,29.24;u=30'
     * Uncertainty of the location is in meters and not required.
     */
    private fun buildGeoUri(latitude: Double, longitude: Double, uncertainty: Double?): String {
        return buildString {
            append("geo:")
            append(latitude)
            append(",")
            append(longitude)
            uncertainty?.let {
                append(";u=")
                append(it)
            }
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
    fun createRedactEvent(roomId: String, eventId: String, reason: String?, withRelTypes: List<String>? = null, additionalContent: Content? = null): Event {
        val localId = LocalEcho.createLocalEchoId()
        val content = if (reason != null || withRelTypes != null) {
            EventRedactBody(
                    reason = reason,
                    unstableWithRelTypes = withRelTypes,
            ).toContent().plus(additionalContent.orEmpty())
        } else {
            additionalContent
        }
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                senderId = userId,
                eventId = localId,
                type = EventType.REDACTION,
                redacts = eventId,
                content = content,
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
    }

    fun createLocalEcho(event: Event) {
        checkNotNull(event.roomId) { "Your event should have a roomId" }
        localEchoRepository.createLocalEcho(event)
    }

    fun createQuotedTextEvent(
            roomId: String,
            quotedEvent: TimelineEvent,
            text: String,
            formattedText: String?,
            autoMarkdown: Boolean,
            rootThreadEventId: String?,
            additionalContent: Content? = null,
    ): Event {
        val messageContent = quotedEvent.getLastMessageContent()
        val formattedQuotedText = (messageContent as? MessageContentWithFormattedBody)?.formattedBody
        val textContent = createQuoteTextContent(messageContent?.body, formattedQuotedText, text, formattedText, autoMarkdown)
        return if (rootThreadEventId != null) {
            createMessageEvent(
                    roomId,
                    textContent.toThreadTextContent(
                            rootThreadEventId = rootThreadEventId,
                            latestThreadEventId = localEchoRepository.getLatestThreadEvent(rootThreadEventId),
                            msgType = MessageType.MSGTYPE_TEXT
                    ),
                    additionalContent,
            )
        } else {
            createFormattedTextEvent(
                    roomId,
                    textContent,
                    MessageType.MSGTYPE_TEXT,
                    additionalContent,
            )
        }
    }

    private fun createQuoteTextContent(
            quotedText: String?,
            formattedQuotedText: String?,
            text: String,
            formattedText: String?,
            autoMarkdown: Boolean
    ): TextContent {
        val currentFormattedText = formattedText ?: if (autoMarkdown) {
            val parsed = markdownParser.parse(text, force = true, advanced = true)
            // If formattedText == text, formattedText is returned as null
            parsed.formattedText ?: parsed.text
        } else {
            text
        }
        val processedFormattedQuotedText = formattedQuotedText ?: quotedText

        val plainTextBody = buildString {
            val plainMessageParagraphs = quotedText?.split("\n\n".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray().orEmpty()
            plainMessageParagraphs.forEachIndexed { index, paragraph ->
                if (paragraph.isNotBlank()) {
                    append("> ")
                    append(paragraph)
                }

                if (index != plainMessageParagraphs.lastIndex) {
                    append("\n\n")
                }
            }
            append("\n\n")
            append(text)
        }
        val formattedTextBody = buildString {
            if (!processedFormattedQuotedText.isNullOrBlank()) {
                append("<blockquote>")
                append(processedFormattedQuotedText)
                append("</blockquote>")
            }
            append("<br/>")
            append(currentFormattedText)
        }
        return TextContent(plainTextBody, formattedTextBody)
    }

    companion object {
        // <mx-reply>
        //     <blockquote>
        //         <a href="https://matrix.to/#/!somewhere:domain.com/$event:domain.com">In reply to</a>
        //         <a href="https://matrix.to/#/@alice:example.org">@alice:example.org</a>
        //         <br />
        //         <!-- This is where the related event's HTML would be. -->
        //     </blockquote>
        // </mx-reply>
        // No whitespace because currently breaks temporary formatted text to Span
        const val REPLY_PATTERN = """<mx-reply><blockquote><a href="%s">In reply to</a> <a href="%s">%s</a><br />%s</blockquote></mx-reply>%s"""
        const val QUOTE_PATTERN = """<blockquote><p>%s</p></blockquote><p>%s</p>"""

        // This is used to replace inner mx-reply tags
        val MX_REPLY_REGEX = "<mx-reply>.*</mx-reply>".toRegex()
    }
}
