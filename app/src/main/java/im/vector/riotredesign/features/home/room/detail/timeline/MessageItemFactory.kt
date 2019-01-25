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

package im.vector.riotredesign.features.home.room.detail.timeline

import android.text.SpannableStringBuilder
import android.text.util.Linkify
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.matrix.android.api.permalinks.MatrixPermalinkSpan
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.media.MediaContentRenderer

class MessageItemFactory(private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                         private val timelineDateFormatter: TimelineDateFormatter) {

    private val messagesDisplayedWithInformation = HashSet<String?>()

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               callback: TimelineEventController.Callback?
    ): AbsMessageItem? {

        val roomMember = event.roomMember
        val nextRoomMember = nextEvent?.roomMember

        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        val isNextMessageReceivedMoreThanOneHourAgo = nextDate?.isBefore(date.minusMinutes(60))
                                                      ?: false

        if (addDaySeparator
            || nextRoomMember != roomMember
            || nextEvent?.root?.type != EventType.MESSAGE
            || isNextMessageReceivedMoreThanOneHourAgo) {
            messagesDisplayedWithInformation.add(event.root.eventId)
        }

        val messageContent: MessageContent = event.root.content.toModel() ?: return null
        val showInformation = messagesDisplayedWithInformation.contains(event.root.eventId)
        val time = timelineDateFormatter.formatMessageHour(date)
        val avatarUrl = roomMember?.avatarUrl
        val memberName = roomMember?.displayName ?: event.root.sender
        val informationData = MessageInformationData(time, avatarUrl, memberName, showInformation)

        return when (messageContent) {
            is MessageTextContent  -> buildTextMessageItem(messageContent, informationData, callback)
            is MessageImageContent -> buildImageMessageItem(messageContent, informationData)
            else                   -> null
        }
    }

    private fun buildImageMessageItem(messageContent: MessageImageContent,
                                      informationData: MessageInformationData): MessageImageItem? {

        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val data = MediaContentRenderer.Data(
                url = messageContent.url,
                height = messageContent.info.height,
                maxHeight = maxHeight,
                width = messageContent.info.width,
                maxWidth = maxWidth,
                rotation = messageContent.info.rotation,
                orientation = messageContent.info.orientation
        )
        return MessageImageItem(data, informationData)
    }

    private fun buildTextMessageItem(messageContent: MessageTextContent,
                                     informationData: MessageInformationData,
                                     callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body.let {
            val spannable = SpannableStringBuilder(it)
            MatrixLinkify.addLinks(spannable, object : MatrixPermalinkSpan.Callback {
                override fun onUrlClicked(url: String) {
                    callback?.onUrlClicked(url)
                }
            })
            Linkify.addLinks(spannable, Linkify.ALL)
            spannable
        }
        return MessageTextItem(
                message = message,
                informationData = informationData
        )
    }


}