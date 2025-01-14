/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.verticalMarginItem
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formEditableAvatarItem
import im.vector.app.features.form.formSwitchItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.RoomHistoryVisibilityFormatter
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val dimensionConverter: DimensionConverter,
        private val roomHistoryVisibilityFormatter: RoomHistoryVisibilityFormatter,
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
    }

    var callback: Callback? = null

    override fun buildModels(data: RoomSettingsViewState?) {
        val roomSummary = data?.roomSummary?.invoke() ?: return
        val host = this

        formEditableAvatarItem {
            id("avatar")
            enabled(data.actionPermissions.canChangeAvatar)
            when (val avatarAction = data.avatarAction) {
                RoomSettingsViewState.AvatarAction.None -> {
                    // Use the current value
                    avatarRenderer(host.avatarRenderer)
                    // We do not want to use the fallback avatar url, which can be the other user avatar, or the current user avatar.
                    matrixItem(roomSummary.toMatrixItem().updateAvatar(data.currentRoomAvatarUrl))
                }
                RoomSettingsViewState.AvatarAction.DeleteAvatar -> imageUri(null)
                is RoomSettingsViewState.AvatarAction.UpdateAvatar -> imageUri(avatarAction.newAvatarUri)
            }
            clickListener { host.callback?.onAvatarChange() }
            deleteListener { host.callback?.onAvatarDelete() }
        }

        buildProfileSection(
                stringProvider.getString(CommonStrings.settings)
        )

        verticalMarginItem {
            id("margin")
            heightInPx(host.dimensionConverter.dpToPx(16))
        }

        formEditTextItem {
            id("name")
            enabled(data.actionPermissions.canChangeName)
            value(data.newName ?: roomSummary.displayName)
            hint(host.stringProvider.getString(CommonStrings.room_settings_name_hint))
            autoCapitalize(true)

            onTextChange { text ->
                host.callback?.onNameChanged(text)
            }
        }
        formEditTextItem {
            id("topic")
            enabled(data.actionPermissions.canChangeTopic)
            value(data.newTopic ?: roomSummary.topic)
            singleLine(false)
            hint(host.stringProvider.getString(CommonStrings.room_settings_topic_hint))

            onTextChange { text ->
                host.callback?.onTopicChanged(text)
            }
        }
        dividerItem {
            id("topicDivider")
        }
        buildProfileAction(
                id = "historyReadability",
                title = stringProvider.getString(CommonStrings.room_settings_room_read_history_rules_pref_title),
                subtitle = roomHistoryVisibilityFormatter.getSetting(data.newHistoryVisibility ?: data.currentHistoryVisibility),
                divider = true,
                editable = data.actionPermissions.canChangeHistoryVisibility,
                action = { if (data.actionPermissions.canChangeHistoryVisibility) callback?.onHistoryVisibilityClicked() }
        )

        buildProfileAction(
                id = "joinRule",
                title = stringProvider.getString(CommonStrings.room_settings_room_access_title),
                subtitle = data.getJoinRuleWording(stringProvider),
                divider = true,
                editable = data.actionPermissions.canChangeJoinRule,
                action = { if (data.actionPermissions.canChangeJoinRule) callback?.onJoinRuleClicked() }
        )

        val isPublic = (data.newRoomJoinRules.newJoinRules ?: data.currentRoomJoinRules) == RoomJoinRules.PUBLIC
        if (vectorPreferences.developerMode() && isPublic) {
            val guestAccess = data.newRoomJoinRules.newGuestAccess ?: data.currentGuestAccess
            // add guest access option?
            formSwitchItem {
                id("guest_access")
                title(host.stringProvider.getString(CommonStrings.room_settings_guest_access_title))
                switchChecked(guestAccess == GuestAccess.CanJoin)
                listener {
                    host.callback?.onToggleGuestAccess()
                }
            }
            dividerItem {
                id("guestAccessDivider")
            }
        }
    }
}
