/*
 * Copyright 2020 New Vector Ltd
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
                                    else                                               -> 1
                                }
                            else                                              ->
                                when {
                                    rightRoomMemberSummary.displayName.isNullOrBlank() -> -1
                                    else                                               -> {
                                        when (leftRoomMemberSummary.displayName) {
                                            rightRoomMemberSummary.displayName ->
                                                // Same display name, compare id
                                                leftRoomMemberSummary.userId.compareTo(rightRoomMemberSummary.userId)
                                            else                               ->
                                                leftRoomMemberSummary.displayName!!.compareTo(rightRoomMemberSummary.displayName!!, true)
                                        }
                                    }
                                }
                        }
                }
        }
    }
}
