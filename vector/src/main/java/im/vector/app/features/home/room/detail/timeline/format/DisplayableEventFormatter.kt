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

package im.vector.app.features.home.room.detail.timeline.format

import dagger.Lazy
import im.vector.app.EmojiSpanify
import im.vector.app.R
import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.core.extensions.orEmpty
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.isLive
import im.vector.app.features.voicebroadcast.isVoiceBroadcast
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.commonmark.node.Document
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import org.matrix.android.sdk.api.session.room.model.relation.ReactionContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getTextDisplayableContent
import javax.inject.Inject

class DisplayableEventFormatter @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val drawableProvider: DrawableProvider,
        private val emojiSpanify: EmojiSpanify,
        private val noticeEventFormatter: NoticeEventFormatter,
        private val htmlRenderer: Lazy<EventHtmlRenderer>
) {

    fun format(timelineEvent: TimelineEvent, isDm: Boolean, appendAuthor: Boolean): CharSequence {
        if (timelineEvent.root.isRedacted()) {
            return noticeEventFormatter.formatRedactedEvent(timelineEvent.root)
        }

        if (timelineEvent.root.isEncrypted() &&
                timelineEvent.root.mxDecryptionResult == null) {
            return stringProvider.getString(R.string.encrypted_message)
        }

        val senderName = timelineEvent.senderInfo.disambiguatedDisplayName

        return when (timelineEvent.root.getClearType()) {
            EventType.MESSAGE -> {
                timelineEvent.getVectorLastMessageContent()?.let { messageContent ->
                    when (messageContent.msgType) {
                        MessageType.MSGTYPE_TEXT -> {
                            val body = messageContent.getTextDisplayableContent()
                            if (messageContent is MessageTextContent && messageContent.matrixFormattedBody.isNullOrBlank().not()) {
                                val localFormattedBody = htmlRenderer.get().parse(body) as Document
                                val renderedBody = htmlRenderer.get().render(localFormattedBody) ?: body
                                simpleFormat(senderName, renderedBody, appendAuthor)
                            } else {
                                simpleFormat(senderName, body, appendAuthor)
                            }
                        }
                        MessageType.MSGTYPE_VERIFICATION_REQUEST -> {
                            simpleFormat(senderName, stringProvider.getString(R.string.verification_request), appendAuthor)
                        }
                        MessageType.MSGTYPE_IMAGE -> {
                            simpleFormat(senderName, stringProvider.getString(R.string.sent_an_image), appendAuthor)
                        }
                        MessageType.MSGTYPE_AUDIO -> {
                            when {
                                (messageContent as? MessageAudioContent)?.voiceMessageIndicator == null -> {
                                    simpleFormat(senderName, stringProvider.getString(R.string.sent_an_audio_file), appendAuthor)
                                }
                                timelineEvent.root.asMessageAudioEvent().isVoiceBroadcast() -> {
                                    simpleFormat(senderName, stringProvider.getString(R.string.started_a_voice_broadcast), appendAuthor)
                                }
                                else -> {
                                    simpleFormat(senderName, stringProvider.getString(R.string.sent_a_voice_message), appendAuthor)
                                }
                            }
                        }
                        MessageType.MSGTYPE_VIDEO -> {
                            simpleFormat(senderName, stringProvider.getString(R.string.sent_a_video), appendAuthor)
                        }
                        MessageType.MSGTYPE_FILE -> {
                            simpleFormat(senderName, stringProvider.getString(R.string.sent_a_file), appendAuthor)
                        }
                        MessageType.MSGTYPE_LOCATION -> {
                            simpleFormat(senderName, stringProvider.getString(R.string.sent_location), appendAuthor)
                        }
                        else -> {
                            simpleFormat(senderName, messageContent.body, appendAuthor)
                        }
                    }
                } ?: span { }
            }
            EventType.STICKER -> {
                simpleFormat(senderName, stringProvider.getString(R.string.send_a_sticker), appendAuthor)
            }
            EventType.REACTION -> {
                timelineEvent.root.getClearContent().toModel<ReactionContent>()?.relatesTo?.let {
                    val emojiSpanned = emojiSpanify.spanify(stringProvider.getString(R.string.sent_a_reaction, it.key))
                    simpleFormat(senderName, emojiSpanned, appendAuthor)
                } ?: span { }
            }
            EventType.KEY_VERIFICATION_CANCEL,
            EventType.KEY_VERIFICATION_DONE -> {
                // cancel and done can appear in timeline, so should have representation
                simpleFormat(senderName, stringProvider.getString(R.string.sent_verification_conclusion), appendAuthor)
            }
            EventType.KEY_VERIFICATION_START,
            EventType.KEY_VERIFICATION_ACCEPT,
            EventType.KEY_VERIFICATION_MAC,
            EventType.KEY_VERIFICATION_KEY,
            EventType.KEY_VERIFICATION_READY,
            EventType.CALL_CANDIDATES -> {
                span { }
            }
            in EventType.POLL_START.values -> {
                (timelineEvent.getVectorLastMessageContent() as? MessagePollContent)?.getBestPollCreationInfo()?.question?.getBestQuestion()
                        ?: stringProvider.getString(R.string.sent_a_poll)
            }
            in EventType.POLL_RESPONSE.values -> {
                stringProvider.getString(R.string.poll_response_room_list_preview)
            }
            in EventType.POLL_END.values -> {
                stringProvider.getString(R.string.poll_end_room_list_preview)
            }
            in EventType.STATE_ROOM_BEACON_INFO.values -> {
                simpleFormat(senderName, stringProvider.getString(R.string.sent_live_location), appendAuthor)
            }
            VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO -> {
                formatVoiceBroadcastEvent(timelineEvent.root, isDm, senderName)
            }
            else -> {
                span {
                    text = noticeEventFormatter.format(timelineEvent, isDm) ?: ""
                    textStyle = "italic"
                }
            }
        }
    }

    fun formatThreadSummary(
            event: Event?,
            latestEdition: String? = null
    ): CharSequence {
        event ?: return ""

        // There event have been edited
        if (latestEdition != null) {
            return run {
                val localFormattedBody = htmlRenderer.get().parse(latestEdition) as Document
                val renderedBody = htmlRenderer.get().render(localFormattedBody) ?: latestEdition
                renderedBody
            }
        }

        // The event have been redacted
        if (event.isRedacted()) {
            return noticeEventFormatter.formatRedactedEvent(event)
        }

        // The event is encrypted
        if (event.isEncrypted() &&
                event.mxDecryptionResult == null) {
            return stringProvider.getString(R.string.encrypted_message)
        }

        return when (event.getClearType()) {
            EventType.MESSAGE -> {
                (event.getClearContent().toModel() as? MessageContent)?.let { messageContent ->
                    when (messageContent.msgType) {
                        MessageType.MSGTYPE_TEXT -> {
                            val body = messageContent.getTextDisplayableContent()
                            if (messageContent is MessageTextContent && messageContent.matrixFormattedBody.isNullOrBlank().not()) {
                                val localFormattedBody = htmlRenderer.get().parse(body) as Document
                                val renderedBody = htmlRenderer.get().render(localFormattedBody) ?: body
                                renderedBody
                            } else {
                                body
                            }
                        }
                        MessageType.MSGTYPE_VERIFICATION_REQUEST -> {
                            stringProvider.getString(R.string.verification_request)
                        }
                        MessageType.MSGTYPE_IMAGE -> {
                            stringProvider.getString(R.string.sent_an_image)
                        }
                        MessageType.MSGTYPE_AUDIO -> {
                            if ((messageContent as? MessageAudioContent)?.voiceMessageIndicator != null) {
                                stringProvider.getString(R.string.sent_a_voice_message)
                            } else {
                                stringProvider.getString(R.string.sent_an_audio_file)
                            }
                        }
                        MessageType.MSGTYPE_VIDEO -> {
                            stringProvider.getString(R.string.sent_a_video)
                        }
                        MessageType.MSGTYPE_FILE -> {
                            stringProvider.getString(R.string.sent_a_file)
                        }
                        MessageType.MSGTYPE_LOCATION -> {
                            stringProvider.getString(R.string.sent_location)
                        }
                        else -> {
                            messageContent.body
                        }
                    }
                } ?: span { }
            }
            EventType.STICKER -> {
                stringProvider.getString(R.string.send_a_sticker)
            }
            EventType.REACTION -> {
                event.getClearContent().toModel<ReactionContent>()?.relatesTo?.let {
                    emojiSpanify.spanify(stringProvider.getString(R.string.sent_a_reaction, it.key))
                } ?: span { }
            }
            in EventType.POLL_START.values -> {
                event.getClearContent().toModel<MessagePollContent>(catchError = true)?.pollCreationInfo?.question?.question
                        ?: stringProvider.getString(R.string.sent_a_poll)
            }
            in EventType.POLL_RESPONSE.values -> {
                stringProvider.getString(R.string.poll_response_room_list_preview)
            }
            in EventType.POLL_END.values -> {
                stringProvider.getString(R.string.poll_end_room_list_preview)
            }
            in EventType.STATE_ROOM_BEACON_INFO.values -> {
                stringProvider.getString(R.string.sent_live_location)
            }
            else -> {
                span {
                }
            }
        }
    }

    private fun simpleFormat(senderName: String, body: CharSequence, appendAuthor: Boolean): CharSequence {
        return if (appendAuthor) {
            span {
                text = senderName
                textColor = colorProvider.getColorFromAttribute(R.attr.vctr_content_primary)
            }
                    .append(": ")
                    .append(body)
        } else {
            body
        }
    }

    private fun formatVoiceBroadcastEvent(event: Event, isDm: Boolean, senderName: String): CharSequence {
        return if (event.asVoiceBroadcastEvent()?.isLive == true) {
            span {
                drawableProvider.getDrawable(R.drawable.ic_voice_broadcast, colorProvider.getColor(R.color.palette_vermilion))?.let {
                    image(it)
                    +" "
                }
                span(stringProvider.getString(R.string.voice_broadcast_live_broadcast)) {
                    textColor = colorProvider.getColor(R.color.palette_vermilion)
                }
            }
        } else {
            noticeEventFormatter.format(event, senderName, isDm).orEmpty()
        }
    }
}
