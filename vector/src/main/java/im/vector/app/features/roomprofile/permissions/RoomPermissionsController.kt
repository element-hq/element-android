/*
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.roomprofile.permissions

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Success
import im.vector.app.R
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.settingsInfoItem
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import javax.inject.Inject

class RoomPermissionsController @Inject constructor(
        private val stringProvider: StringProvider,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomPermissionsViewState>() {

    interface Callback {
        fun onEditPermission(editablePermission: EditablePermission, currentRole: Role)
    }

    var callback: Callback? = null

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    // Order is the order applied in the UI
    private val allEditablePermissions = listOf(
            EditablePermission.DefaultRole(),
            EditablePermission.SendMessages(),
            EditablePermission.InviteUsers(),
            EditablePermission.ChangeSettings(),
            EditablePermission.KickUsers(),
            EditablePermission.BanUsers(),
            EditablePermission.RemoveMessagesSentByOthers(),
            EditablePermission.NotifyEveryone(),
            EditablePermission.ModifyWidgets(),
            EditablePermission.ChangeRoomAvatar(),
            EditablePermission.ChangeMainAddressForTheRoom(),
            EditablePermission.EnableRoomEncryption(),
            EditablePermission.ChangeHistoryVisibility(),
            EditablePermission.ChangeRoomName(),
            EditablePermission.ChangePermissions(),
            EditablePermission.SendRoomServerAclEvents(),
            EditablePermission.UpgradeTheRoom(),
            EditablePermission.ChangeTopic()
    )

    init {
        setData(null)
    }

    override fun buildModels(data: RoomPermissionsViewState?) {
        buildProfileSection(
                stringProvider.getString(R.string.room_permissions_title)
        )

        settingsInfoItem {
            id("notice")
            helperText(stringProvider.getString(R.string.room_permissions_notice))
        }

        when (val content = data?.currentPowerLevelsContent) {
            is Success -> buildPermissions(data, content())
            else       -> {
                loadingItem {
                    id("loading")
                    loadingText(stringProvider.getString(R.string.loading))
                }
            }
        }
    }

    private fun buildPermissions(data: RoomPermissionsViewState, content: PowerLevelsContent) {
        allEditablePermissions.forEach { editablePermission ->
            val currentRole = getCurrentRole(editablePermission, content)
            buildProfileAction(
                    id = editablePermission.labelResId.toString(),
                    title = stringProvider.getString(editablePermission.labelResId),
                    subtitle = getSubtitle(currentRole),
                    dividerColor = dividerColor,
                    divider = true,
                    editable = data.actionPermissions.canChangePowerLevels,
                    action = { callback?.onEditPermission(editablePermission, currentRole) }
            )
        }
    }

    private fun getSubtitle(currentRole: Role): String {
        return when (currentRole) {
            Role.Admin,
            Role.Moderator,
            Role.Default   -> stringProvider.getString(currentRole.res)
            is Role.Custom -> stringProvider.getString(currentRole.res, currentRole.value)
        }
    }

    private fun getCurrentRole(editablePermission: EditablePermission, content: PowerLevelsContent): Role {
        val value = when (editablePermission) {
            is EditablePermission.EventTypeEditablePermission -> content.events[editablePermission.eventType] ?: content.stateDefault
            is EditablePermission.DefaultRole                 -> content.usersDefault
            is EditablePermission.SendMessages                -> content.eventsDefault
            is EditablePermission.InviteUsers                 -> content.invite
            is EditablePermission.ChangeSettings              -> content.stateDefault
            is EditablePermission.KickUsers                   -> content.kick
            is EditablePermission.BanUsers                    -> content.ban
            is EditablePermission.RemoveMessagesSentByOthers  -> content.redact
            is EditablePermission.NotifyEveryone              -> content.notificationLevel(PowerLevelsContent.NOTIFICATIONS_ROOM_KEY)
        }

        return Role.fromValue(
                value,
                when (editablePermission) {
                    is EditablePermission.EventTypeEditablePermission -> content.stateDefault
                    is EditablePermission.DefaultRole                 -> Role.Default.value
                    is EditablePermission.SendMessages                -> Role.Default.value
                    is EditablePermission.InviteUsers                 -> Role.Moderator.value
                    is EditablePermission.ChangeSettings              -> Role.Moderator.value
                    is EditablePermission.KickUsers                   -> Role.Moderator.value
                    is EditablePermission.BanUsers                    -> Role.Moderator.value
                    is EditablePermission.RemoveMessagesSentByOthers  -> Role.Moderator.value
                    is EditablePermission.NotifyEveryone              -> Role.Moderator.value
                }
        )
    }
}
