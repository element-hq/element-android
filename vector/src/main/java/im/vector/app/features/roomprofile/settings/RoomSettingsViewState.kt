/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings

import android.net.Uri
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomSettingsViewState(
        val roomId: String,
        // Default value: https://matrix.org/docs/spec/client_server/r0.6.1#id88
        val currentHistoryVisibility: RoomHistoryVisibility = RoomHistoryVisibility.SHARED,
        val currentRoomJoinRules: RoomJoinRules = RoomJoinRules.INVITE,
        val currentGuestAccess: GuestAccess? = null,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val isLoading: Boolean = false,
        val currentRoomAvatarUrl: String? = null,
        val avatarAction: AvatarAction = AvatarAction.None,
        val newName: String? = null,
        val newTopic: String? = null,
        val newHistoryVisibility: RoomHistoryVisibility? = null,
        val newRoomJoinRules: NewJoinRule = NewJoinRule(),
        val showSaveAction: Boolean = false,
        val actionPermissions: ActionPermissions = ActionPermissions(),
        val supportsRestricted: Boolean = false,
        val canUpgradeToRestricted: Boolean = false,
) : MavericksState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    data class ActionPermissions(
            val canChangeAvatar: Boolean = false,
            val canChangeName: Boolean = false,
            val canChangeTopic: Boolean = false,
            val canChangeHistoryVisibility: Boolean = false,
            val canChangeJoinRule: Boolean = false,
            val canAddChildren: Boolean = false
    )

    sealed class AvatarAction {
        object None : AvatarAction()
        object DeleteAvatar : AvatarAction()
        data class UpdateAvatar(
                val newAvatarUri: Uri,
                val newAvatarFileName: String
        ) : AvatarAction()
    }

    data class NewJoinRule(
            val newJoinRules: RoomJoinRules? = null,
            val newGuestAccess: GuestAccess? = null
    ) {
        fun hasChanged() = newJoinRules != null || newGuestAccess != null
    }

    fun getJoinRuleWording(stringProvider: StringProvider): String {
        return when (val joinRule = newRoomJoinRules.newJoinRules ?: currentRoomJoinRules) {
            RoomJoinRules.INVITE -> {
                stringProvider.getString(CommonStrings.room_settings_room_access_private_title)
            }
            RoomJoinRules.PUBLIC -> {
                stringProvider.getString(CommonStrings.room_settings_room_access_public_title)
            }
            RoomJoinRules.KNOCK -> {
                stringProvider.getString(CommonStrings.room_settings_room_access_entry_knock)
            }
            RoomJoinRules.RESTRICTED -> {
                stringProvider.getString(CommonStrings.room_settings_room_access_restricted_title)
            }
            else -> {
                stringProvider.getString(CommonStrings.room_settings_room_access_entry_unknown, joinRule.value)
            }
        }
    }
}
