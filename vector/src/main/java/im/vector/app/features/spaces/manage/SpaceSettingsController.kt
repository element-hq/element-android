/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formEditableSquareAvatarItem
import im.vector.app.features.form.formMultiLineEditTextItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.settings.RoomSettingsViewState
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val vectorPreferences: VectorPreferences
) : TypedEpoxyController<RoomSettingsViewState>() {

    interface Callback {
        // Delete the avatar, or cancel an avatar change
        fun onAvatarDelete()
        fun onAvatarChange()
        fun onNameChanged(name: String)
        fun onTopicChanged(topic: String)
        fun onHistoryVisibilityClicked()
        fun onJoinRuleClicked()
        fun onToggleGuestAccess()
        fun onDevTools()
        fun onDevRoomSettings()
        fun onManageRooms()
        fun setIsPublic(public: Boolean)
        fun onRoomAliasesClicked()
        fun onRoomPermissionsClicked()
    }

    var callback: Callback? = null

    override fun buildModels(data: RoomSettingsViewState?) {
        val roomSummary = data?.roomSummary?.invoke() ?: return
        val host = this

        formEditableSquareAvatarItem {
            id("avatar")
            enabled(data.actionPermissions.canChangeAvatar)
            when (val avatarAction = data.avatarAction) {
                RoomSettingsViewState.AvatarAction.None -> {
                    // Use the current value
                    avatarRenderer(host.avatarRenderer)
                    // We do not want to use the fallback avatar url, which can be the other user avatar, or the current user avatar.
                    matrixItem(roomSummary.toMatrixItem().updateAvatar(data.currentRoomAvatarUrl))
                }
                RoomSettingsViewState.AvatarAction.DeleteAvatar ->
                    imageUri(null)
                is RoomSettingsViewState.AvatarAction.UpdateAvatar ->
                    imageUri(avatarAction.newAvatarUri)
            }
            clickListener { host.callback?.onAvatarChange() }
            deleteListener { host.callback?.onAvatarDelete() }
        }

        buildProfileSection(
                stringProvider.getString(CommonStrings.settings)
        )

        formEditTextItem {
            id("name")
            enabled(data.actionPermissions.canChangeName)
            value(data.newName ?: roomSummary.displayName)
            hint(host.stringProvider.getString(CommonStrings.create_room_name_hint))
            onTextChange { text ->
                host.callback?.onNameChanged(text)
            }
        }

        formMultiLineEditTextItem {
            id("topic")
            enabled(data.actionPermissions.canChangeTopic)
            value(data.newTopic ?: roomSummary.topic)
            hint(host.stringProvider.getString(CommonStrings.create_space_topic_hint))
            onTextChange { text ->
                host.callback?.onTopicChanged(text)
            }
        }

        val isPublic = (data.newRoomJoinRules.newJoinRules ?: data.currentRoomJoinRules) == RoomJoinRules.PUBLIC
        buildProfileAction(
                id = "joinRule",
                title = stringProvider.getString(CommonStrings.room_settings_space_access_title),
                subtitle = data.getJoinRuleWording(stringProvider),
                divider = true,
                editable = data.actionPermissions.canChangeJoinRule,
                action = { if (data.actionPermissions.canChangeJoinRule) callback?.onJoinRuleClicked() }
        )
//        if (vectorPreferences.labsUseExperimentalRestricted()) {
//            buildProfileAction(
//                    id = "joinRule",
//                    title = stringProvider.getString(CommonStrings.room_settings_room_access_title),
//                    subtitle = data.getJoinRuleWording(stringProvider),
//                    divider = false,
//                    editable = data.actionPermissions.canChangeJoinRule,
//                    action = { if (data.actionPermissions.canChangeJoinRule) callback?.onJoinRuleClicked() }
//            )
//        } else {
//            formSwitchItem {
//                id("isPublic")
//                enabled(data.actionPermissions.canChangeJoinRule)
//                title(host.stringProvider.getString(CommonStrings.make_this_space_public))
//                switchChecked(isPublic)
//
//                listener { value ->
//                    host.callback?.setIsPublic(value)
//                }
//            }
//        }
        dividerItem {
            id("divider")
        }

        buildProfileAction(
                id = "manage_rooms",
                title = stringProvider.getString(CommonStrings.space_settings_manage_rooms),
                // subtitle = data.getJoinRuleWording(stringProvider),
                divider = true,
                editable = data.actionPermissions.canAddChildren,
                action = {
                    if (data.actionPermissions.canAddChildren) callback?.onManageRooms()
                }
        )

        if (isPublic) {
            buildProfileAction(
                    id = "alias",
                    title = stringProvider.getString(CommonStrings.space_settings_alias_title),
                    subtitle = stringProvider.getString(CommonStrings.space_settings_alias_subtitle),
                    divider = true,
                    editable = true,
                    action = { callback?.onRoomAliasesClicked() }
            )
        }

        buildProfileAction(
                id = "permissions",
                title = stringProvider.getString(CommonStrings.space_settings_permissions_title),
                subtitle = stringProvider.getString(CommonStrings.space_settings_permissions_subtitle),
                divider = vectorPreferences.developerMode(),
                editable = true,
                action = { callback?.onRoomPermissionsClicked() }
        )

        if (vectorPreferences.developerMode()) {
            buildProfileAction(
                    id = "dev_tools",
                    title = stringProvider.getString(CommonStrings.settings_dev_tools),
                    icon = org.matrix.android.sdk.R.drawable.ic_verification_glasses,
                    tintIcon = false,
                    divider = true,
                    action = {
                        callback?.onDevTools()
                    }
            )

            buildProfileAction(
                    id = "room_tools",
                    title = stringProvider.getString(CommonStrings.room_list_quick_actions_room_settings),
                    icon = org.matrix.android.sdk.R.drawable.ic_verification_glasses,
                    tintIcon = false,
                    divider = false,
                    action = {
                        callback?.onDevRoomSettings()
                    }
            )
        }
    }
}
