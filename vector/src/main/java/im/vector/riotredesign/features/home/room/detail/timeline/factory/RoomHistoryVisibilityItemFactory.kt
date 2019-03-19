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

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibility
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibilityContent
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem_


class RoomHistoryVisibilityItemFactory(private val stringProvider: StringProvider) {

    fun create(event: TimelineEvent): NoticeItem? {
        val roomMember = event.roomMember ?: return null
        val noticeText = buildNoticeText(event.root, roomMember) ?: return null
        return NoticeItem_()
                .noticeText(noticeText)
                .avatarUrl(roomMember.avatarUrl)
                .memberName(roomMember.displayName)
    }

    private fun buildNoticeText(event: Event, roomMember: RoomMember): CharSequence? {
        val content = event.content.toModel<RoomHistoryVisibilityContent>() ?: return null
        val formattedVisibility = when (content.historyVisibility) {
            RoomHistoryVisibility.SHARED         -> stringProvider.getString(R.string.notice_room_visibility_shared)
            RoomHistoryVisibility.INVITED        -> stringProvider.getString(R.string.notice_room_visibility_invited)
            RoomHistoryVisibility.JOINED         -> stringProvider.getString(R.string.notice_room_visibility_joined)
            RoomHistoryVisibility.WORLD_READABLE -> stringProvider.getString(R.string.notice_room_visibility_world_readable)
        }
        return stringProvider.getString(R.string.notice_made_future_room_visibility, roomMember.displayName, formattedVisibility)
    }


}


