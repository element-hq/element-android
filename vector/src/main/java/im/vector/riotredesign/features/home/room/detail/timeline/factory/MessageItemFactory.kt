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

package im.vector.riotredesign.features.home.room.detail.timeline.factory

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import androidx.annotation.ColorRes
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.matrix.android.api.permalinks.MatrixPermalinkSpan
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.RelationType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.EditAggregatedSummary
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.EmojiCompatFontProvider
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.core.linkify.VectorLinkify
import im.vector.riotredesign.core.resources.ColorProvider
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.core.utils.DebouncedClickListener
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.getColorFromUserId
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.detail.timeline.item.*
import im.vector.riotredesign.features.html.EventHtmlRenderer
import im.vector.riotredesign.features.media.ImageContentRenderer
import im.vector.riotredesign.features.media.VideoContentRenderer
import me.gujun.android.span.span

class MessageItemFactory(private val colorProvider: ColorProvider,
                         private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                         private val timelineDateFormatter: TimelineDateFormatter,
                         private val htmlRenderer: EventHtmlRenderer,
                         private val stringProvider: StringProvider,
                         private val emojiCompatFontProvider: EmojiCompatFontProvider) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               callback: TimelineEventController.Callback?
    ): VectorEpoxyModel<*>? {

        val eventId = event.root.eventId ?: return null

        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        val isNextMessageReceivedMoreThanOneHourAgo = nextDate?.isBefore(date.minusMinutes(60))
                ?: false

        val showInformation = addDaySeparator
                || event.senderAvatar != nextEvent?.senderAvatar
                || event.senderName != nextEvent?.senderName
                || nextEvent?.root?.type != EventType.MESSAGE
                || isNextMessageReceivedMoreThanOneHourAgo

        val time = timelineDateFormatter.formatMessageHour(date)
        val avatarUrl = event.senderAvatar
        val memberName = event.senderName ?: event.root.sender ?: ""
        val formattedMemberName = span(memberName) {
            textColor = colorProvider.getColor(getColorFromUserId(event.root.sender
                    ?: ""))
        }
        val hasBeenEdited = event.annotations?.editSummary != null
        val informationData = MessageInformationData(eventId = eventId,
                senderId = event.root.sender ?: "",
                sendState = event.sendState,
                time = time,
                avatarUrl = avatarUrl,
                memberName = formattedMemberName,
                showInformation = showInformation,
                orderedReactionList = event.annotations?.reactionsSummary?.map {
                    ReactionInfoData(it.key, it.count, it.addedByMe, it.localEchoEvents.isEmpty())
                },
                hasBeenEdited = hasBeenEdited
        )

        if (event.root.unsignedData?.redactedEvent != null) {
            //message is redacted
            return buildRedactedItem(informationData, callback)
        }

        val messageContent: MessageContent =
                event.annotations?.editSummary?.aggregatedContent?.toModel()
                        ?: event.root.content.toModel()
                        ?: //Malformed content, we should echo something on screen
                        return DefaultItem_().text(stringProvider.getString(R.string.malformed_message))

        if (messageContent.relatesTo?.type == RelationType.REPLACE) {
            // ignore replace event, the targeted id is already edited
            return BlankItem_()
        }
//        val all = event.root.toContent()
//        val ev = all.toModel<Event>()
        return when (messageContent) {
            is MessageEmoteContent  -> buildEmoteMessageItem(messageContent,
                    informationData,
                    hasBeenEdited,
                    event.annotations?.editSummary,
                    callback)
            is MessageTextContent   -> buildTextMessageItem(event.sendState,
                    messageContent,
                    informationData,
                    hasBeenEdited,
                    event.annotations?.editSummary,
                    callback
            )
            is MessageImageContent  -> buildImageMessageItem(messageContent, informationData, callback)
            is MessageNoticeContent -> buildNoticeMessageItem(messageContent, informationData, callback)
            is MessageVideoContent  -> buildVideoMessageItem(messageContent, informationData, callback)
            is MessageFileContent   -> buildFileMessageItem(messageContent, informationData, callback)
            is MessageAudioContent  -> buildAudioMessageItem(messageContent, informationData, callback)
            else                    -> buildNotHandledMessageItem(messageContent)
        }
    }

    private fun buildAudioMessageItem(messageContent: MessageAudioContent,
                                      informationData: MessageInformationData,
                                      callback: TimelineEventController.Callback?): MessageFileItem? {
        return MessageFileItem_()
                .informationData(informationData)
                .avatarCallback(callback)
                .filename(messageContent.body)
                .iconRes(R.drawable.filetype_audio)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view: View ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .clickListener(
                        DebouncedClickListener(View.OnClickListener {
                            callback?.onAudioMessageClicked(messageContent)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildFileMessageItem(messageContent: MessageFileContent,
                                     informationData: MessageInformationData,
                                     callback: TimelineEventController.Callback?): MessageFileItem? {
        return MessageFileItem_()
                .informationData(informationData)
                .avatarCallback(callback)
                .filename(messageContent.body)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .iconRes(R.drawable.filetype_attachment)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
                .clickListener(
                        DebouncedClickListener(View.OnClickListener { _ ->
                            callback?.onFileMessageClicked(messageContent)
                        }))
    }

    private fun buildNotHandledMessageItem(messageContent: MessageContent): DefaultItem? {
        val text = "${messageContent.type} message events are not yet handled"
        return DefaultItem_().text(text)
    }

    private fun buildImageMessageItem(messageContent: MessageImageContent,
                                      informationData: MessageInformationData,
                                      callback: TimelineEventController.Callback?): MessageImageVideoItem? {

        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val data = ImageContentRenderer.Data(
                filename = messageContent.body,
                url = messageContent.url,
                height = messageContent.info?.height,
                maxHeight = maxHeight,
                width = messageContent.info?.width,
                maxWidth = maxWidth,
                orientation = messageContent.info?.orientation,
                rotation = messageContent.info?.rotation
        )
        return MessageImageVideoItem_()
                .playable(messageContent.info?.mimeType == "image/gif")
                .informationData(informationData)
                .avatarCallback(callback)
                .mediaData(data)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .clickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onImageMessageClicked(messageContent, data, view)
                        }))
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildVideoMessageItem(messageContent: MessageVideoContent,
                                      informationData: MessageInformationData,
                                      callback: TimelineEventController.Callback?): MessageImageVideoItem? {

        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val thumbnailData = ImageContentRenderer.Data(
                filename = messageContent.body,
                url = messageContent.info?.thumbnailUrl,
                height = messageContent.info?.height,
                maxHeight = maxHeight,
                width = messageContent.info?.width,
                maxWidth = maxWidth
        )

        val videoData = VideoContentRenderer.Data(
                filename = messageContent.body,
                videoUrl = messageContent.url,
                thumbnailMediaData = thumbnailData
        )

        return MessageImageVideoItem_()
                .playable(true)
                .informationData(informationData)
                .avatarCallback(callback)
                .mediaData(thumbnailData)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .clickListener { view -> callback?.onVideoMessageClicked(messageContent, videoData, view) }
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildTextMessageItem(sendState: SendState,
                                     messageContent: MessageTextContent,
                                     informationData: MessageInformationData,
                                     hasBeenEdited: Boolean,
                                     editSummary: EditAggregatedSummary?,
                                     callback: TimelineEventController.Callback?): MessageTextItem? {

        val bodyToUse = messageContent.formattedBody?.let {
            htmlRenderer.render(it)
        } ?: messageContent.body

        val linkifiedBody = linkifyBody(bodyToUse, callback)

        return MessageTextItem_()
                .apply {
                    if (hasBeenEdited) {
                        val spannable = annotateWithEdited(linkifiedBody, callback, informationData, editSummary)
                        message(spannable)
                    } else {
                        message(linkifiedBody)
                    }
                }
                .informationData(informationData)
                .avatarCallback(callback)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                //click on the text
                .clickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun annotateWithEdited(linkifiedBody: CharSequence,
                                   callback: TimelineEventController.Callback?,
                                   informationData: MessageInformationData,
                                   editSummary: EditAggregatedSummary?): SpannableStringBuilder {
        val spannable = SpannableStringBuilder()
        spannable.append(linkifiedBody)
        // TODO i18n
        val editedSuffix = "(edited)"
        spannable.append(" ").append(editedSuffix)
        val color = colorProvider.getColorFromAttribute(R.attr.vctr_list_header_secondary_text_color)
        val editStart = spannable.indexOf(editedSuffix)
        val editEnd = editStart + editedSuffix.length
        spannable.setSpan(
                ForegroundColorSpan(color),
                editStart,
                editEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

        spannable.setSpan(RelativeSizeSpan(.9f), editStart, editEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View?) {
                callback?.onEditedDecorationClicked(informationData, editSummary)
            }

            override fun updateDrawState(ds: TextPaint?) {
                //nop
            }
        },
                editStart,
                editEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannable
    }

    private fun buildNoticeMessageItem(messageContent: MessageNoticeContent,
                                       informationData: MessageInformationData,
                                       callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = span {
                text = it
                textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                textStyle = "italic"
            }
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .message(message)
                .informationData(informationData)
                .avatarCallback(callback)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .memberClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onMemberNameClicked(informationData)
                        }))
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildEmoteMessageItem(messageContent: MessageEmoteContent,
                                      informationData: MessageInformationData,
                                      hasBeenEdited: Boolean,
                                      editSummary: EditAggregatedSummary?,
                                      callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = "* ${informationData.memberName} $it"
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .apply {
                    if (hasBeenEdited) {
                        val spannable = annotateWithEdited(message, callback, informationData, editSummary)
                        message(spannable)
                    } else {
                        message(message)
                    }
                }
                .informationData(informationData)
                .avatarCallback(callback)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildRedactedItem(informationData: MessageInformationData,
                                  callback: TimelineEventController.Callback?): RedactedMessageItem? {
        return RedactedMessageItem_()
                .informationData(informationData)
                .avatarCallback(callback)
    }

    private fun linkifyBody(body: CharSequence, callback: TimelineEventController.Callback?): CharSequence {
        val spannable = SpannableStringBuilder(body)
        MatrixLinkify.addLinks(spannable, object : MatrixPermalinkSpan.Callback {
            override fun onUrlClicked(url: String) {
                callback?.onUrlClicked(url)
            }
        })
        VectorLinkify.addLinks(spannable, true)
        return spannable
    }
}