/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.powerlevel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.flow.flow

fun Role.isOwner() = this == Role.Creator || this == Role.SuperAdmin

fun Room.membersByRoleFlow(): Flow<Map<Role, List<RoomMemberSummary>>> {
    val roomMembersFlow = flow().liveRoomMembers(roomMemberQueryParams())
    val roomPowerLevelsFlow = flow().liveRoomPowerLevels()
    return combine(roomMembersFlow, roomPowerLevelsFlow) { roomMembers, roomPowerLevels ->
        roomMembers.groupBy { roomPowerLevels.getSuggestedRole(it.userId) }
    }.distinctUntilChanged()
}

fun Room.isLastAdminFlow(userId: String): Flow<Boolean> {
    return membersByRoleFlow().map { membersByRole ->
        val creatorMembers = membersByRole[Role.Creator].orEmpty()
        val superAdminMembers = membersByRole[Role.SuperAdmin].orEmpty()
        val adminMembers = membersByRole[Role.Admin].orEmpty()
        val joinedAdmins = (adminMembers + creatorMembers + superAdminMembers).filter { it.membership == Membership.JOIN }
        if (joinedAdmins.size == 1) {
            joinedAdmins.first().userId == userId
        } else {
            false
        }
    }
}
