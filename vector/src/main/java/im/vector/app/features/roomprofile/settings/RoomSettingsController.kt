/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formEditableAvatarItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.RoomHistoryVisibilityFormatter
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val roomHistoryVisibilityFormatter: RoomHistoryVisibilityFormatter,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomSettingsViewState>() {

    interface Callback {
        // Delete the avatar, or cancel an avatar change
        fun onAvatarDelete()
        fun onAvatarChange()
        fun onNameChanged(name: String)
        fun onTopicChanged(topic: String)
        fun onHistoryVisibilityClicked()
        fun onJoinRuleClicked()
    }

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var callback: Callback? = null

    init {
        setData(null)
    }

    override fun buildModels(data: RoomSettingsViewState?) {
        val roomSummary = data?.roomSummary?.invoke() ?: return

        formEditableAvatarItem {
            id("avatar")
            enabled(data.actionPermissions.canChangeAvatar)
            when (val avatarAction = data.avatarAction) {
                RoomSettingsViewState.AvatarAction.None -> {
                    // Use the current value
                    avatarRenderer(avatarRenderer)
                    // We do not want to use the fallback avatar url, which can be the other user avatar, or the current user avatar.
                    matrixItem(roomSummary.toMatrixItem().copy(avatarUrl = data.currentRoomAvatarUrl))
                }
                RoomSettingsViewState.AvatarAction.DeleteAvatar ->
                    imageUri(null)
                is RoomSettingsViewState.AvatarAction.UpdateAvatar ->
                    imageUri(avatarAction.newAvatarUri)
            }
            clickListener { callback?.onAvatarChange() }
            deleteListener { callback?.onAvatarDelete() }
        }

        buildProfileSection(
                stringProvider.getString(R.string.settings)
        )

        formEditTextItem {
            id("name")
            enabled(data.actionPermissions.canChangeName)
            value(data.newName ?: roomSummary.displayName)
            hint(stringProvider.getString(R.string.room_settings_name_hint))

            onTextChange { text ->
                callback?.onNameChanged(text)
            }
        }

        formEditTextItem {
            id("topic")
            enabled(data.actionPermissions.canChangeTopic)
            value(data.newTopic ?: roomSummary.topic)
            hint(stringProvider.getString(R.string.room_settings_topic_hint))

            onTextChange { text ->
                callback?.onTopicChanged(text)
            }
        }

        buildProfileAction(
                id = "historyReadability",
                title = stringProvider.getString(R.string.room_settings_room_read_history_rules_pref_title),
                subtitle = roomHistoryVisibilityFormatter.getSetting(data.newHistoryVisibility ?: data.currentHistoryVisibility),
                dividerColor = dividerColor,
                divider = true,
                editable = data.actionPermissions.canChangeHistoryVisibility,
                action = { if (data.actionPermissions.canChangeHistoryVisibility) callback?.onHistoryVisibilityClicked() }
        )

        buildProfileAction(
                id = "joinRule",
                title = stringProvider.getString(R.string.room_settings_room_access_title),
                subtitle = data.getJoinRuleWording(),
                dividerColor = dividerColor,
                divider = false,
                editable = data.actionPermissions.canChangeJoinRule,
                action = { if (data.actionPermissions.canChangeJoinRule) callback?.onJoinRuleClicked() }
        )
    }

    private fun RoomSettingsViewState.getJoinRuleWording(): String {
        val joinRule = newRoomJoinRules.newJoinRules ?: currentRoomJoinRules
        val guestAccess = newRoomJoinRules.newGuestAccess ?: currentGuestAccess
        val resId = when (joinRule) {
            RoomJoinRules.INVITE     -> {
                R.string.room_settings_room_access_entry_only_invited to null
            }
            RoomJoinRules.PRIVATE    -> {
                R.string.room_settings_room_access_entry_unknown to joinRule.value
            }
            RoomJoinRules.PUBLIC     -> {
                if (guestAccess == GuestAccess.CanJoin) {
                    R.string.room_settings_room_access_entry_anyone_with_link_including_guest to null
                } else {
                    R.string.room_settings_room_access_entry_anyone_with_link_apart_guest to null
                }
            }
            RoomJoinRules.KNOCK      -> {
                R.string.room_settings_room_access_entry_knock to null
            }
            RoomJoinRules.RESTRICTED -> {
                R.string.room_settings_room_access_entry_restricted to null
            }
        }
        return if (resId.second == null) stringProvider.getString(resId.first) else stringProvider.getString(resId.first, resId.second)
//        return stringProvider.getString(if (joinRule == RoomJoinRules.INVITE) {
//            R.string.room_settings_room_access_entry_only_invited
//        } else {
//            if (guestAccess == GuestAccess.CanJoin) {
//                R.string.room_settings_room_access_entry_anyone_with_link_including_guest
//            } else {
//                R.string.room_settings_room_access_entry_anyone_with_link_apart_guest
//            }
//        })
    }
}
