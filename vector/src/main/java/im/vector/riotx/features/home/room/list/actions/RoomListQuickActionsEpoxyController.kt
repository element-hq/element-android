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

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetRoomPreviewItem
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

/**
 * Epoxy controller for room list actions
 */
class RoomListQuickActionsEpoxyController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider
) : TypedEpoxyController<RoomListQuickActionsState>() {

    var listener: Listener? = null

    override fun buildModels(state: RoomListQuickActionsState) {
        val roomSummary = state.roomSummary() ?: return
        val showAll = state.mode == RoomListActionsArgs.Mode.FULL

        if (showAll) {
            // Preview, favorite, settings
            bottomSheetRoomPreviewItem {
                id("room_preview")
                avatarRenderer(avatarRenderer)
                matrixItem(roomSummary.toMatrixItem())
                stringProvider(stringProvider)
                izFavorite(roomSummary.isFavorite)
                settingsClickListener { listener?.didSelectMenuAction(RoomListQuickActionsSharedAction.Settings(roomSummary.roomId)) }
                favoriteClickListener { listener?.didSelectMenuAction(RoomListQuickActionsSharedAction.Favorite(roomSummary.roomId)) }
            }

            // Notifications
            dividerItem {
                id("notifications_separator")
            }
        }

        val selectedRoomState = state.roomNotificationState()
        if (state.roomSummary()?.isDirect == true) {
            // In this case, default is All
            RoomListQuickActionsSharedAction.NotificationsAllDefault(roomSummary.roomId).toBottomSheetItem(0, selectedRoomState)
            RoomListQuickActionsSharedAction.NotificationsMentionsKeywords(roomSummary.roomId).toBottomSheetItem(1, selectedRoomState)
        } else {
            // In this case, default is MentionsKeywords
            RoomListQuickActionsSharedAction.NotificationsAll(roomSummary.roomId).toBottomSheetItem(0, selectedRoomState)
            RoomListQuickActionsSharedAction.NotificationsMentionsKeywordsDefault(roomSummary.roomId).toBottomSheetItem(1, selectedRoomState)
        }
        RoomListQuickActionsSharedAction.NotificationsNone(roomSummary.roomId).toBottomSheetItem(2, selectedRoomState)

        if (showAll) {
            // Leave
            dividerItem {
                id("leave_separator")
            }
            RoomListQuickActionsSharedAction.Leave(roomSummary.roomId).toBottomSheetItem(5)
        }
    }

    private fun RoomListQuickActionsSharedAction.toBottomSheetItem(index: Int, roomNotificationState: RoomNotificationState? = null) {
        val showSelected = roomNotificationState != null

        val selected = when (this) {
            is RoomListQuickActionsSharedAction.NotificationsAll,
            is RoomListQuickActionsSharedAction.NotificationsAllDefault              -> roomNotificationState == RoomNotificationState.ALL_MESSAGES
            is RoomListQuickActionsSharedAction.NotificationsMentionsKeywords,
            is RoomListQuickActionsSharedAction.NotificationsMentionsKeywordsDefault -> roomNotificationState == RoomNotificationState.MENTIONS_AND_KEYWORDS
            is RoomListQuickActionsSharedAction.NotificationsNone                    -> roomNotificationState == RoomNotificationState.NONE
            is RoomListQuickActionsSharedAction.Settings,
            is RoomListQuickActionsSharedAction.Favorite,
            is RoomListQuickActionsSharedAction.Leave                                -> false
        }
        return bottomSheetActionItem {
            id("action_$index")
            showSelected(showSelected)
            selected(selected)
            iconRes(iconResId)
            textRes(titleRes)
            destructive(this@toBottomSheetItem.destructive)
            listener(View.OnClickListener { listener?.didSelectMenuAction(this@toBottomSheetItem) })
        }
    }

    interface Listener {
        fun didSelectMenuAction(quickAction: RoomListQuickActionsSharedAction)
    }
}
