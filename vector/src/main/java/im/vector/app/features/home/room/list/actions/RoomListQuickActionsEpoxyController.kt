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
package im.vector.app.features.home.room.list.actions

import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetRoomPreviewItem
import im.vector.app.core.epoxy.profiles.notifications.radioButtonItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.notifications.notificationOptions
import im.vector.app.features.roomprofile.notifications.notificationStateMapped
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

/**
 * Epoxy controller for room list actions.
 */
class RoomListQuickActionsEpoxyController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val colorProvider: ColorProvider,
        private val stringProvider: StringProvider,
) : TypedEpoxyController<RoomListQuickActionViewState>() {

    var listener: Listener? = null

    override fun buildModels(state: RoomListQuickActionViewState) {
        val notificationViewState = state.notificationSettingsViewState
        val roomSummary = notificationViewState.roomSummary() ?: return
        val host = this
        // Preview, favorite, settings
        bottomSheetRoomPreviewItem {
            id("room_preview")
            avatarRenderer(host.avatarRenderer)
            matrixItem(roomSummary.toMatrixItem())
            stringProvider(host.stringProvider)
            colorProvider(host.colorProvider)
            izLowPriority(roomSummary.isLowPriority)
            izFavorite(roomSummary.isFavorite)
            settingsClickListener { host.listener?.didSelectMenuAction(RoomListQuickActionsSharedAction.Settings(roomSummary.roomId)) }
            favoriteClickListener { host.listener?.didSelectMenuAction(RoomListQuickActionsSharedAction.Favorite(roomSummary.roomId)) }
            lowPriorityClickListener { host.listener?.didSelectMenuAction(RoomListQuickActionsSharedAction.LowPriority(roomSummary.roomId)) }
        }

        // Notifications
        bottomSheetDividerItem {
            id("notifications_separator")
        }

        notificationViewState.notificationOptions.forEach { notificationState ->
            val title = titleForNotificationState(notificationState)
            radioButtonItem {
                id(notificationState.name)
                titleRes(title)
                selected(notificationViewState.notificationStateMapped() == notificationState)
                listener {
                    host.listener?.didSelectRoomNotificationState(notificationState)
                }
            }
        }

        RoomListQuickActionsSharedAction.Leave(roomSummary.roomId, showIcon = !true).toBottomSheetItem()
    }

    @StringRes
    private fun titleForNotificationState(notificationState: RoomNotificationState): Int? = when (notificationState) {
        RoomNotificationState.ALL_MESSAGES_NOISY -> R.string.room_settings_all_messages
        RoomNotificationState.MENTIONS_ONLY -> R.string.room_settings_mention_and_keyword_only
        RoomNotificationState.MUTE -> R.string.room_settings_none
        else -> null
    }

    private fun RoomListQuickActionsSharedAction.Leave.toBottomSheetItem() {
        val host = this@RoomListQuickActionsEpoxyController
        return bottomSheetActionItem {
            id("action_leave")
            selected(false)
            if (iconResId != null) {
                iconRes(iconResId)
            } else {
                showIcon(false)
            }
            textRes(titleRes)
            destructive(this@toBottomSheetItem.destructive)
            listener { host.listener?.didSelectMenuAction(this@toBottomSheetItem) }
        }
    }

    interface Listener {
        fun didSelectMenuAction(quickAction: RoomListQuickActionsSharedAction)
        fun didSelectRoomNotificationState(roomNotificationState: RoomNotificationState)
    }
}
