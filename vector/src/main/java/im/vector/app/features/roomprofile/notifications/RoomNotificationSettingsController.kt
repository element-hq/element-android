/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.roomprofile.notifications

import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.profiles.notifications.notificationSettingsFooterItem
import im.vector.app.core.epoxy.profiles.notifications.radioButtonItem
import im.vector.app.core.epoxy.profiles.notifications.textHeaderItem
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
            textRes(R.string.room_settings_room_notifications_notify_me)
        }
        data.notificationOptions.forEach {  notificationState ->
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
        RoomNotificationState.ALL_MESSAGES_NOISY -> R.string.room_settings_all_messages
        RoomNotificationState.MENTIONS_ONLY      -> R.string.room_settings_mention_and_keyword_only
        RoomNotificationState.MUTE               -> R.string.room_settings_none
        else -> null
    }
}
