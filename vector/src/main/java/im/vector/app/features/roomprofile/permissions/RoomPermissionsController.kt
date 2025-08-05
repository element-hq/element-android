/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Success
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.settingsInfoItem
import im.vector.app.features.form.formAdvancedToggleItem
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.banOrDefault
import org.matrix.android.sdk.api.session.room.model.eventsDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.inviteOrDefault
import org.matrix.android.sdk.api.session.room.model.kickOrDefault
import org.matrix.android.sdk.api.session.room.model.redactOrDefault
import org.matrix.android.sdk.api.session.room.model.stateDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.usersDefaultOrDefault
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.room.powerlevels.UserPowerLevel
import javax.inject.Inject

class RoomPermissionsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val roleFormatter: RoleFormatter
) : TypedEpoxyController<RoomPermissionsViewState>() {

    interface Callback {
        fun onEditPermission(editablePermission: EditablePermission, currentPowerLevel: UserPowerLevel.Value)
        fun toggleShowAllPermissions()
    }

    var callback: Callback? = null

    // Order is the order applied in the UI
    // Element Web order is not really nice, try to put the settings which are more likely to be updated first
    // And a second section, hidden by default
    private val usefulEditablePermissions = listOf(
            EditablePermission.ChangeRoomAvatar(),
            EditablePermission.ChangeRoomName(),
            EditablePermission.ChangeTopic()
    )

    private val usefulEditablePermissionsForSpace = listOf(
            EditablePermission.ChangeRoomAvatar(),
            EditablePermission.ChangeRoomName(),
            EditablePermission.ChangeTopic(),
            EditablePermission.InviteUsers()
    )

    private val advancedEditablePermissions = listOf(
            EditablePermission.ChangeMainAddressForTheRoom(),

            EditablePermission.DefaultRole(),
            EditablePermission.InviteUsers(),
            EditablePermission.KickUsers(),
            EditablePermission.BanUsers(),

            EditablePermission.SendMessages(),

            EditablePermission.RemoveMessagesSentByOthers(),
            EditablePermission.NotifyEveryone(),

            EditablePermission.ChangeSettings(),
            EditablePermission.ModifyWidgets(),
            EditablePermission.ChangeHistoryVisibility(),
            EditablePermission.ChangePermissions(),
            EditablePermission.SendRoomServerAclEvents(),
            EditablePermission.EnableRoomEncryption(),
            EditablePermission.UpgradeTheRoom()
    )

    private val advancedEditablePermissionsForSpace = listOf(
            EditablePermission.ChangeMainAddressForTheRoom(),

            EditablePermission.DefaultRole(),
            EditablePermission.KickUsers(),
            EditablePermission.BanUsers(),

            EditablePermission.SendMessages(),

            EditablePermission.RemoveMessagesSentByOthers(),
            EditablePermission.NotifyEveryone(),

            EditablePermission.ChangeSettings(),
//            EditablePermission.ModifyWidgets(),
            EditablePermission.ChangeHistoryVisibility(),
            EditablePermission.ChangePermissions(),
            EditablePermission.SendRoomServerAclEvents(),
//            EditablePermission.EnableRoomEncryption(),
            EditablePermission.UpgradeTheRoom()
    )

    override fun buildModels(data: RoomPermissionsViewState?) {
        val host = this
        buildProfileSection(
                stringProvider.getString(CommonStrings.room_permissions_title)
        )

        when (val content = data?.currentPowerLevelsContent) {
            is Success -> buildPermissions(data, content())
            else -> {
                loadingItem {
                    id("loading")
                    loadingText(host.stringProvider.getString(CommonStrings.loading))
                }
            }
        }
    }

    private fun buildPermissions(data: RoomPermissionsViewState, content: PowerLevelsContent) {
        val host = this
        val editable = data.actionPermissions.canChangePowerLevels
        val isSpace = data.roomSummary.invoke()?.roomType == RoomType.SPACE

        settingsInfoItem {
            id("notice")
            helperText(
                    host.stringProvider.getString(
                            if (editable) {
                                if (isSpace) CommonStrings.space_permissions_notice else CommonStrings.room_permissions_notice
                            } else {
                                if (isSpace) CommonStrings.space_permissions_notice_read_only else CommonStrings.room_permissions_notice_read_only
                            }
                    )
            )
        }

        // Useful permissions
        if (isSpace) {
            usefulEditablePermissionsForSpace.forEach { buildPermission(it, content, editable, true) }
        } else {
            usefulEditablePermissions.forEach { buildPermission(it, content, editable, false) }
        }

        // Toggle
        formAdvancedToggleItem {
            id("showAdvanced")
            title(host.stringProvider.getString(if (data.showAdvancedPermissions) CommonStrings.hide_advanced else CommonStrings.show_advanced))
            expanded(!data.showAdvancedPermissions)
            listener { host.callback?.toggleShowAllPermissions() }
        }

        // Advanced permissions
        if (data.showAdvancedPermissions) {
            if (isSpace) {
                advancedEditablePermissionsForSpace.forEach { buildPermission(it, content, editable, true) }
            } else {
                advancedEditablePermissions.forEach { buildPermission(it, content, editable, false) }
            }
        }
    }

    private fun buildPermission(
            editablePermission: EditablePermission,
            content: PowerLevelsContent,
            editable: Boolean,
            isSpace: Boolean
    ) {
        val currentPowerLevel = getPowerLevel(editablePermission, content)
        val currentRole = Role.getSuggestedRole(currentPowerLevel)
        buildProfileAction(
                id = editablePermission.labelResId.toString(),
                title = stringProvider.getString(
                        if (isSpace) editablePermission.spaceLabelResId else editablePermission.labelResId
                ),
                subtitle = roleFormatter.format(currentRole),
                divider = true,
                editable = editable,
                action = {
                    callback
                            ?.takeIf { editable }
                            ?.onEditPermission(editablePermission, currentPowerLevel)
                }
        )
    }

    private fun getPowerLevel(editablePermission: EditablePermission, content: PowerLevelsContent): UserPowerLevel.Value {
        val value = when (editablePermission) {
            is EditablePermission.EventTypeEditablePermission -> content.events?.get(editablePermission.eventType) ?: content.stateDefaultOrDefault()
            is EditablePermission.DefaultRole -> content.usersDefaultOrDefault()
            is EditablePermission.SendMessages -> content.eventsDefaultOrDefault()
            is EditablePermission.InviteUsers -> content.inviteOrDefault()
            is EditablePermission.ChangeSettings -> content.stateDefaultOrDefault()
            is EditablePermission.KickUsers -> content.kickOrDefault()
            is EditablePermission.BanUsers -> content.banOrDefault()
            is EditablePermission.RemoveMessagesSentByOthers -> content.redactOrDefault()
            is EditablePermission.NotifyEveryone -> content.notificationLevel(PowerLevelsContent.NOTIFICATIONS_ROOM_KEY)
        }
        return UserPowerLevel.Value(value)
    }
}
