/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.members

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.platform.GenericIdArgs
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomMemberListViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val roomMemberSummaries: Async<RoomMemberSummaries> = Uninitialized,
        val areAllMembersLoaded: Boolean = false,
        val ignoredUserIds: List<String> = emptyList(),
        val filter: String = "",
        val threePidInvites: Async<List<Event>> = Uninitialized,
        val trustLevelMap: Async<Map<String, RoomEncryptionTrustLevel?>> = Uninitialized,
        val actionsPermissions: ActionPermissions = ActionPermissions()
) : MavericksState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    constructor(args: GenericIdArgs) : this(roomId = args.id)
}

data class ActionPermissions(
        val canInvite: Boolean = false,
        val canRevokeThreePidInvite: Boolean = false
)

typealias RoomMemberSummaries = List<Pair<RoomMemberListCategories, List<RoomMemberSummary>>>

enum class RoomMemberListCategories(@StringRes val titleRes: Int) {
    ADMIN(R.string.room_member_power_level_admins),
    MODERATOR(R.string.room_member_power_level_moderators),
    CUSTOM(R.string.room_member_power_level_custom),
    INVITE(R.string.room_member_power_level_invites),
    USER(R.string.room_member_power_level_users)
}
