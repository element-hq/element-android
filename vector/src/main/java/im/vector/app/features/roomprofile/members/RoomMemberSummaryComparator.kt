/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.members

import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import javax.inject.Inject

class RoomMemberSummaryComparator @Inject constructor() : Comparator<RoomMemberSummary> {

    override fun compare(leftRoomMemberSummary: RoomMemberSummary?, rightRoomMemberSummary: RoomMemberSummary?): Int {
        return when (leftRoomMemberSummary) {
            null ->
                when (rightRoomMemberSummary) {
                    null -> 0
                    else -> 1
                }
            else ->
                when (rightRoomMemberSummary) {
                    null -> -1
                    else ->
                        when {
                            leftRoomMemberSummary.displayName.isNullOrBlank() ->
                                when {
                                    rightRoomMemberSummary.displayName.isNullOrBlank() -> {
                                        // No display names, compare ids
                                        leftRoomMemberSummary.userId.compareTo(rightRoomMemberSummary.userId)
                                    }
                                    else -> 1
                                }
                            else ->
                                when {
                                    rightRoomMemberSummary.displayName.isNullOrBlank() -> -1
                                    else -> {
                                        when (leftRoomMemberSummary.displayName) {
                                            rightRoomMemberSummary.displayName ->
                                                // Same display name, compare id
                                                leftRoomMemberSummary.userId.compareTo(rightRoomMemberSummary.userId)
                                            else ->
                                                leftRoomMemberSummary.displayName!!.compareTo(rightRoomMemberSummary.displayName!!, true)
                                        }
                                    }
                                }
                        }
                }
        }
    }
}
