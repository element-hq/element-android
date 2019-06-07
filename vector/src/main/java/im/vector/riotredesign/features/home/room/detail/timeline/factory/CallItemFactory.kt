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
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem_

class CallItemFactory(private val stringProvider: StringProvider) {

    fun create(event: TimelineEvent): NoticeItem? {
        val text = buildNoticeText(event.root, event.senderName) ?: return null
        return NoticeItem_()
                .noticeText(text)
                .avatarUrl(event.senderAvatar)
                .memberName(event.senderName)
    }

    private fun buildNoticeText(event: Event, senderName: String?): CharSequence? {
        return when {
            EventType.CALL_INVITE == event.getClearType() -> {
                val content = event.content.toModel<CallInviteContent>() ?: return null
                val isVideoCall = content.offer.sdp == CallInviteContent.Offer.SDP_VIDEO
                return if (isVideoCall) {
                    stringProvider.getString(R.string.notice_placed_video_call, senderName)
                } else {
                    stringProvider.getString(R.string.notice_placed_voice_call, senderName)
                }
            }
            EventType.CALL_ANSWER == event.getClearType() -> stringProvider.getString(R.string.notice_answered_call, senderName)
            EventType.CALL_HANGUP == event.getClearType() -> stringProvider.getString(R.string.notice_ended_call, senderName)
            else                                          -> null
        }

    }


}