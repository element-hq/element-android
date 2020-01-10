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

package im.vector.riotx.features.home.room.detail.timeline.factory

import android.view.View
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.crypto.model.event.EncryptionEventContent
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotx.features.home.room.detail.timeline.item.NoticeItem
import im.vector.riotx.features.home.room.detail.timeline.item.NoticeItem_
import javax.inject.Inject

class EncryptionItemFactory @Inject constructor(private val stringProvider: StringProvider,
                                                private val avatarRenderer: AvatarRenderer,
                                                private val avatarSizeProvider: AvatarSizeProvider,
                                                private val session: Session) {

    fun create(event: TimelineEvent,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): NoticeItem? {
        val text = buildNoticeText(event.root, event.getDisambiguatedDisplayName()) ?: return null
        val informationData = MessageInformationData(
                eventId = event.root.eventId ?: "?",
                senderId = event.root.senderId ?: "",
                sendState = event.root.sendState,
                ageLocalTS = event.root.ageLocalTs,
                avatarUrl = event.senderAvatar,
                memberName = event.getDisambiguatedDisplayName(),
                showInformation = false,
                sentByMe = event.root.senderId == session.myUserId
        )
        val attributes = NoticeItem.Attributes(
                avatarRenderer = avatarRenderer,
                informationData = informationData,
                noticeText = text,
                itemLongClickListener = View.OnLongClickListener { view ->
                    callback?.onEventLongClicked(informationData, null, view) ?: false
                },
                readReceiptsCallback = callback
        )
        return NoticeItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .highlighted(highlight)
                .attributes(attributes)
    }

    private fun buildNoticeText(event: Event, senderName: String?): CharSequence? {
        return when {
            EventType.STATE_ROOM_ENCRYPTION == event.getClearType() -> {
                val content = event.content.toModel<EncryptionEventContent>() ?: return null
                stringProvider.getString(R.string.notice_end_to_end, senderName, content.algorithm)
            }
            else                                         -> null
        }
    }
}
