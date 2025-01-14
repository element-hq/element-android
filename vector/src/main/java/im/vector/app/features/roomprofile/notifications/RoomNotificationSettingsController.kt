/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.notifications

import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.profiles.notifications.notificationSettingsFooterItem
import im.vector.app.core.epoxy.profiles.notifications.radioButtonItem
import im.vector.app.core.epoxy.profiles.notifications.textHeaderItem
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import javax.inject.Inject

class RoomNotificationSettingsController @Inject constructor() : TypedEpoxyController<RoomNotificationSettingsViewState>() {

    interface Callback {
        fun didSelectRoomNotificationState(roomNotificationState: RoomNotificationState)
        fun didSelectAccountSettingsLink()
    }

    var callback: Callback? = null

    override fun buildModels(data: RoomNotificationSettingsViewState?) {
        val host = this
        data ?: return

        textHeaderItem {
            id("roomNotificationSettingsHeader")
            textRes(CommonStrings.room_settings_room_notifications_notify_me)
        }
        data.notificationOptions.forEach { notificationState ->
            val title = titleForNotificationState(notificationState)
            radioButtonItem {
                id(notificationState.name)
                titleRes(title)
                selected(data.notificationStateMapped() == notificationState)
                listener {
                    host.callback?.didSelectRoomNotificationState(notificationState)
                }
            }
        }
        notificationSettingsFooterItem {
            id("roomNotificationSettingsFooter")
            encrypted(data.roomSummary()?.isEncrypted == true)
            clickListener {
                host.callback?.didSelectAccountSettingsLink()
            }
        }
    }

    @StringRes
    private fun titleForNotificationState(notificationState: RoomNotificationState): Int? = when (notificationState) {
        RoomNotificationState.ALL_MESSAGES_NOISY -> CommonStrings.room_settings_all_messages
        RoomNotificationState.MENTIONS_ONLY -> CommonStrings.room_settings_mention_and_keyword_only
        RoomNotificationState.MUTE -> CommonStrings.room_settings_none
        else -> null
    }
}
