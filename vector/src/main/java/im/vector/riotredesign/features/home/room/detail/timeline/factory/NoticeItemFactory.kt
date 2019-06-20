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

import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.senderAvatar
import im.vector.riotredesign.features.home.room.detail.timeline.helper.senderName
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem_

class NoticeItemFactory(private val eventFormatter: NoticeEventFormatter) {

    fun create(event: TimelineEvent,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): NoticeItem? {
        val formattedText = eventFormatter.format(event) ?: return null
        val informationData = MessageInformationData(
                eventId = event.root.eventId ?: "?",
                senderId = event.root.senderId ?: "",
                sendState = event.sendState,
                avatarUrl = event.senderAvatar(),
                memberName = event.senderName(),
                showInformation = false
        )

        return NoticeItem_()
                .noticeText(formattedText)
                .highlighted(highlight)
                .informationData(informationData)
                .baseCallback(callback)
    }


}