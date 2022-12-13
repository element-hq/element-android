/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list.home

import androidx.recyclerview.widget.DiffUtil
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class RoomSummaryRoomListDiffCallback @Inject constructor(
//        vectorPreferences: VectorPreferences
) : DiffUtil.ItemCallback<RoomSummary>() {

    override fun areItemsTheSame(oldItem: RoomSummary, newItem: RoomSummary): Boolean {
        return oldItem.roomId == newItem.roomId
    }

    override fun areContentsTheSame(oldItem: RoomSummary, newItem: RoomSummary): Boolean {
        // for this use case we can test less things
        if (oldItem.roomId != newItem.roomId) return false
        if (oldItem.displayName != newItem.displayName) return false
        if (oldItem.name != newItem.name) return false
        if (oldItem.topic != newItem.topic) return false
        if (oldItem.avatarUrl != newItem.avatarUrl) return false
        if (oldItem.canonicalAlias != newItem.canonicalAlias) return false
        if (oldItem.aliases != newItem.aliases) return false
        if (oldItem.isDirect != newItem.isDirect) return false
        if (oldItem.directUserPresence != newItem.directUserPresence) return false
        if (oldItem.latestPreviewableEvent != newItem.latestPreviewableEvent) return false
        if (oldItem.notificationCount != newItem.notificationCount) return false
        if (oldItem.highlightCount != newItem.highlightCount) return false
        if (oldItem.threadNotificationCount != newItem.threadNotificationCount) return false
        if (oldItem.threadHighlightCount != newItem.threadHighlightCount) return false
        if (oldItem.hasUnreadMessages != newItem.hasUnreadMessages) return false
        if (oldItem.userDrafts != newItem.userDrafts) return false
        if (oldItem.isEncrypted != newItem.isEncrypted) return false
        if (oldItem.typingUsers != newItem.typingUsers) return false
        if (oldItem.hasFailedSending != newItem.hasFailedSending) return false
        return true
    }
}
