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
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.core.date.VectorDateFormatter
import im.vector.riotx.core.epoxy.bottomsheet.BottomSheetItemAction_
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetItemRoomPreview
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetItemSeparator
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

/**
 * Epoxy controller for room list actions
 */
class RoomListQuickActionsEpoxyController @Inject constructor(private val stringProvider: StringProvider,
                                                              private val avatarRenderer: AvatarRenderer,
                                                              private val dateFormatter: VectorDateFormatter,
                                                              private val fontProvider: EmojiCompatFontProvider) : TypedEpoxyController<RoomListQuickActionsState>() {

    var listener: Listener? = null

    override fun buildModels(state: RoomListQuickActionsState) {
        val roomSummary = state.roomSummary() ?: return

        // Preview
        bottomSheetItemRoomPreview {
            id("preview")
            avatarRenderer(avatarRenderer)
            roomName(roomSummary.displayName)
            avatarUrl(roomSummary.avatarUrl)
            roomId(roomSummary.roomId)
            settingsClickListener(View.OnClickListener { listener?.didSelectMenuAction(RoomListQuickActions.Settings(roomSummary.roomId)) })
        }

        // Notifications
        bottomSheetItemSeparator {
            id("notifications_separator")
        }
        RoomListQuickActions.NotificationsAllNoisy(roomSummary.roomId).toBottomSheetItem(0)
        RoomListQuickActions.NotificationsAll(roomSummary.roomId).toBottomSheetItem(1)
        RoomListQuickActions.NotificationsMentionsOnly(roomSummary.roomId).toBottomSheetItem(2)
        RoomListQuickActions.NotificationsMute(roomSummary.roomId).toBottomSheetItem(3)

        // Leave
        bottomSheetItemSeparator {
            id("leave_separator")
        }
        RoomListQuickActions.Leave(roomSummary.roomId).toBottomSheetItem(5)
    }

    private fun RoomListQuickActions.toBottomSheetItem(index: Int) {
        return BottomSheetItemAction_()
                .id("action_$index")
                .iconRes(iconResId)
                .textRes(titleRes)
                .listener(View.OnClickListener { listener?.didSelectMenuAction(this) })
                .addTo(this@RoomListQuickActionsEpoxyController)
    }

    interface Listener {
        fun didSelectMenuAction(quickActions: RoomListQuickActions)
    }
}
