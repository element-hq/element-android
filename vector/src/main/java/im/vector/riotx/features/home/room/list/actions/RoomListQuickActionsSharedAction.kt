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

package im.vector.riotx.features.home.room.list.actions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorSharedAction

sealed class RoomListQuickActionsSharedAction(
        @StringRes val titleRes: Int,
        @DrawableRes val iconResId: Int,
        val destructive: Boolean = false)
    : VectorSharedAction {

    data class NotificationsAll(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_all,
            R.drawable.ic_room_actions_notifications_all_messages_24dp
    )

    data class NotificationsAllDefault(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_all_default,
            R.drawable.ic_room_actions_notifications_all_messages_24dp
    )

    data class NotificationsMentionsKeywords(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_mentions_and_keywords,
            R.drawable.ic_room_actions_notifications_mentions_keywords_24dp
    )

    data class NotificationsMentionsKeywordsDefault(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_mentions_and_keywords_default,
            R.drawable.ic_room_actions_notifications_mentions_keywords_24dp
    )

    data class NotificationsNone(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_none,
            R.drawable.ic_room_actions_notifications_none_24dp
    )

    data class Settings(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_settings,
            R.drawable.ic_room_actions_settings
    )

    data class Favorite(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_favorite_add,
            R.drawable.ic_star_24dp)

    data class Leave(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_leave,
            R.drawable.ic_room_actions_leave,
            true
    )
}
