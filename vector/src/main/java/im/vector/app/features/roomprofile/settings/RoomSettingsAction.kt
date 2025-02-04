/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules

sealed class RoomSettingsAction : VectorViewModelAction {
    data class SetAvatarAction(val avatarAction: RoomSettingsViewState.AvatarAction) : RoomSettingsAction()
    data class SetRoomName(val newName: String) : RoomSettingsAction()
    data class SetRoomTopic(val newTopic: String) : RoomSettingsAction()
    data class SetRoomHistoryVisibility(val visibility: RoomHistoryVisibility) : RoomSettingsAction()
    data class SetRoomJoinRule(val roomJoinRule: RoomJoinRules) : RoomSettingsAction()
    data class SetRoomGuestAccess(val guestAccess: GuestAccess) : RoomSettingsAction()

    object Save : RoomSettingsAction()
    object Cancel : RoomSettingsAction()
}
