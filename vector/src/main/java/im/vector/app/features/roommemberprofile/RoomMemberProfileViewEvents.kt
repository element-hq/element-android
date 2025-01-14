/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roommemberprofile

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for RoomMemberProfile.
 */
sealed class RoomMemberProfileViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : RoomMemberProfileViewEvents()
    data class Failure(val throwable: Throwable) : RoomMemberProfileViewEvents()

    object OnIgnoreActionSuccess : RoomMemberProfileViewEvents()
    object OnReportActionSuccess : RoomMemberProfileViewEvents()
    object OnSetPowerLevelSuccess : RoomMemberProfileViewEvents()
    object OnInviteActionSuccess : RoomMemberProfileViewEvents()
    object OnKickActionSuccess : RoomMemberProfileViewEvents()
    object OnBanActionSuccess : RoomMemberProfileViewEvents()
    data class ShowPowerLevelValidation(val currentValue: Int, val newValue: Int) : RoomMemberProfileViewEvents()
    data class ShowPowerLevelDemoteWarning(val currentValue: Int, val newValue: Int) : RoomMemberProfileViewEvents()

    data class StartVerification(
            val userId: String,
            val canCrossSign: Boolean
    ) : RoomMemberProfileViewEvents()

    data class ShareRoomMemberProfile(val permalink: String) : RoomMemberProfileViewEvents()
    data class OpenRoom(val roomId: String) : RoomMemberProfileViewEvents()
    object GoBack : RoomMemberProfileViewEvents()
}
