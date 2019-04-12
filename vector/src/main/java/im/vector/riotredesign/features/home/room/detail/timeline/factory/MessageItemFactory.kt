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

import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.annotation.ColorRes
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.matrix.android.api.permalinks.MatrixPermalinkSpan
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageAudioContent
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageEmoteContent
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageNoticeContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.core.linkify.VectorLinkify
import im.vector.riotredesign.core.resources.ColorProvider
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.detail.timeline.item.DefaultItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.DefaultItem_
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageFileItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageFileItem_
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageImageVideoItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageImageVideoItem_
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageTextItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageTextItem_
import im.vector.riotredesign.features.html.EventHtmlRenderer
import im.vector.riotredesign.features.media.ImageContentRenderer
import im.vector.riotredesign.features.media.VideoContentRenderer
import me.gujun.android.span.span

class MessageItemFactory(private val colorProvider: ColorProvider,
                         private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                         private val timelineDateFormatter: TimelineDateFormatter,
                         private val htmlRenderer: EventHtmlRenderer) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               callback: TimelineEventController.Callback?
    ): VectorEpoxyModel<*>? {

        val eventId = event.root.eventId ?: return null
        val roomMember = event.roomMember
        val nextRoomMember = nextEvent?.roomMember

        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        val isNextMessageReceivedMoreThanOneHourAgo = nextDate?.isBefore(date.minusMinutes(60))
                                                      ?: false

        val showInformation = addDaySeparator
                              || nextRoomMember != roomMember
                              || nextEvent?.root?.type != EventType.MESSAGE
                              || isNextMessageReceivedMoreThanOneHourAgo

        val messageContent: MessageContent = event.root.content.toModel() ?: return null
        val time = timelineDateFormatter.formatMessageHour(date)
        val avatarUrl = roomMember?.avatarUrl
        val memberName = roomMember?.displayName ?: event.root.sender ?: ""
        val formattedMemberName = span(memberName) {
            textColor = colorProvider.getColor(getColorFor(event.root.sender ?: ""))
        }
        val informationData = MessageInformationData(eventId = eventId,
                                                     senderId = event.root.sender ?: "",
                                                     sendState = event.sendState,
                                                     time = time,
                                                     avatarUrl = avatarUrl,
                                                     memberName = formattedMemberName,
                                                     showInformation = showInformation)

        return when (messageContent) {
            is MessageEmoteContent  -> buildEmoteMessageItem(messageContent, informationData, callback)
            is MessageTextContent   -> buildTextMessageItem(messageContent, informationData, callback)
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
                .filename(messageContent.body)
                .iconRes(R.drawable.filetype_audio)
                .clickListener { _ -> callback?.onAudioMessageClicked(messageContent) }
    }

    private fun buildFileMessageItem(messageContent: MessageFileContent,
                                     informationData: MessageInformationData,
                                     callback: TimelineEventController.Callback?): MessageFileItem? {
        return MessageFileItem_()
                .informationData(informationData)
                .filename(messageContent.body)
                .iconRes(R.drawable.filetype_attachment)
                .clickListener { _ -> callback?.onFileMessageClicked(messageContent) }
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
                .mediaData(data)
                .clickListener { view -> callback?.onImageMessageClicked(messageContent, data, view) }
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
                .mediaData(thumbnailData)
                .clickListener { view -> callback?.onVideoMessageClicked(messageContent, videoData, view) }
    }

    private fun buildTextMessageItem(messageContent: MessageTextContent,
                                     informationData: MessageInformationData,
                                     callback: TimelineEventController.Callback?): MessageTextItem? {

        val bodyToUse = messageContent.formattedBody?.let {
            htmlRenderer.render(it)
        } ?: messageContent.body

        val linkifiedBody = linkifyBody(bodyToUse, callback)
        return MessageTextItem_()
                .message(linkifiedBody)
                .informationData(informationData)
    }

    private fun buildNoticeMessageItem(messageContent: MessageNoticeContent,
                                       informationData: MessageInformationData,
                                       callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = span {
                text = it
                textColor = colorProvider.getColor(R.color.slate_grey)
                textStyle = "italic"
            }
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .message(message)
                .informationData(informationData)
    }

    private fun buildEmoteMessageItem(messageContent: MessageEmoteContent,
                                      informationData: MessageInformationData,
                                      callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = "* ${informationData.memberName} $it"
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .message(message)
                .informationData(informationData)
    }

    private fun linkifyBody(body: CharSequence, callback: TimelineEventController.Callback?): Spannable {
        val spannable = SpannableStringBuilder(body)
        MatrixLinkify.addLinks(spannable, object : MatrixPermalinkSpan.Callback {
            override fun onUrlClicked(url: String) {
                callback?.onUrlClicked(url)
            }
        })
        VectorLinkify.addLinks(spannable, true)
        return spannable
    }

    //Based on riot-web implementation
    @ColorRes
    private fun getColorFor(sender: String): Int {
        var hash = 0
        var i = 0
        var chr: Char
        if (sender.isEmpty()) {
            return R.color.username_1
        }
        while (i < sender.length) {
            chr = sender[i]
            hash = (hash shl 5) - hash + chr.toInt()
            hash = hash or 0
            i++
        }
        val cI = Math.abs(hash) % 8 + 1
        return when (cI) {
            1    -> R.color.username_1
            2    -> R.color.username_2
            3    -> R.color.username_3
            4    -> R.color.username_4
            5    -> R.color.username_5
            6    -> R.color.username_6
            7    -> R.color.username_7
            else -> R.color.username_8
        }
    }
}