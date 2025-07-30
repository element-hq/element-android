/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.members

import javax.inject.Inject

class RoomMemberListComparator @Inject constructor() : Comparator<RoomMemberWithPowerLevel> {

    override fun compare(leftRoomMember: RoomMemberWithPowerLevel?, rightRoomMember: RoomMemberWithPowerLevel?): Int {
        return when (leftRoomMember) {
            null ->
                when (rightRoomMember) {
                    null -> 0
                    else -> 1
                }
            else ->
                when (rightRoomMember) {
                    null -> -1
                    else ->
                        when {
                            leftRoomMember.powerLevel > rightRoomMember.powerLevel -> -1
                            leftRoomMember.powerLevel < rightRoomMember.powerLevel -> 1
                            leftRoomMember.summary.displayName.isNullOrBlank() ->
                                when {
                                    rightRoomMember.summary.displayName.isNullOrBlank() -> {
                                        // No display names, compare ids
                                        leftRoomMember.summary.userId.compareTo(rightRoomMember.summary.userId)
                                    }
                                    else -> 1
                                }
                            else ->
                                when {
                                    rightRoomMember.summary.displayName.isNullOrBlank() -> -1
                                    else -> {
                                        when (leftRoomMember.summary.displayName) {
                                            rightRoomMember.summary.displayName ->
                                                // Same display name, compare id
                                                leftRoomMember.summary.userId.compareTo(rightRoomMember.summary.userId)
                                            else ->
                                                leftRoomMember.summary.displayName!!.compareTo(rightRoomMember.summary.displayName!!, true)
                                        }
                                    }
                                }
                        }
                }
        }
    }
}
