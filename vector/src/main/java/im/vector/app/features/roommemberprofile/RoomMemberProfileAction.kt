/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roommemberprofile

import im.vector.app.core.platform.VectorViewModelAction

sealed class RoomMemberProfileAction : VectorViewModelAction {
    object RetryFetchingInfo : RoomMemberProfileAction()
    object IgnoreUser : RoomMemberProfileAction()
    object ReportUser : RoomMemberProfileAction()
    data class BanOrUnbanUser(val reason: String?) : RoomMemberProfileAction()
    data class KickUser(val reason: String?) : RoomMemberProfileAction()
    object InviteUser : RoomMemberProfileAction()
    object VerifyUser : RoomMemberProfileAction()
    object ShareRoomMemberProfile : RoomMemberProfileAction()
    data class SetPowerLevel(val previousValue: Int, val newValue: Int, val askForValidation: Boolean) : RoomMemberProfileAction()
    data class SetUserColorOverride(val newColorSpec: String) : RoomMemberProfileAction()
    data class OpenOrCreateDm(val userId: String) : RoomMemberProfileAction()
}
