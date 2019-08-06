/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.home.room.list

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotx.R

data class RoomListViewState(
        val displayMode: RoomListFragment.DisplayMode,
        val asyncRooms: Async<List<RoomSummary>> = Uninitialized,
        val roomFilter: String = "",
        val asyncFilteredRooms: Async<RoomSummaries> = Uninitialized,
        // List of roomIds that the user wants to join
        val joiningRoomsIds: Set<String> = emptySet(),
        // List of roomIds that the user wants to join, but an error occurred
        val joiningErrorRoomsIds: Set<String> = emptySet(),
        // List of roomIds that the user wants to join
        val rejectingRoomsIds: Set<String> = emptySet(),
        // List of roomIds that the user wants to reject, but an error occurred
        val rejectingErrorRoomsIds: Set<String> = emptySet(),
        val isInviteExpanded: Boolean = true,
        val isFavouriteRoomsExpanded: Boolean = true,
        val isDirectRoomsExpanded: Boolean = true,
        val isGroupRoomsExpanded: Boolean = true,
        val isLowPriorityRoomsExpanded: Boolean = true,
        val isServerNoticeRoomsExpanded: Boolean = true
) : MvRxState {

    constructor(args: RoomListParams) : this(displayMode = args.displayMode)

    fun isCategoryExpanded(roomCategory: RoomCategory): Boolean {
        return when (roomCategory) {
            RoomCategory.INVITE        -> isInviteExpanded
            RoomCategory.FAVOURITE     -> isFavouriteRoomsExpanded
            RoomCategory.DIRECT        -> isDirectRoomsExpanded
            RoomCategory.GROUP         -> isGroupRoomsExpanded
            RoomCategory.LOW_PRIORITY  -> isLowPriorityRoomsExpanded
            RoomCategory.SERVER_NOTICE -> isServerNoticeRoomsExpanded
        }
    }

    fun toggle(roomCategory: RoomCategory): RoomListViewState {
        return when (roomCategory) {
            RoomCategory.INVITE        -> copy(isInviteExpanded = !isInviteExpanded)
            RoomCategory.FAVOURITE     -> copy(isFavouriteRoomsExpanded = !isFavouriteRoomsExpanded)
            RoomCategory.DIRECT        -> copy(isDirectRoomsExpanded = !isDirectRoomsExpanded)
            RoomCategory.GROUP         -> copy(isGroupRoomsExpanded = !isGroupRoomsExpanded)
            RoomCategory.LOW_PRIORITY  -> copy(isLowPriorityRoomsExpanded = !isLowPriorityRoomsExpanded)
            RoomCategory.SERVER_NOTICE -> copy(isServerNoticeRoomsExpanded = !isServerNoticeRoomsExpanded)
        }
    }
}

typealias RoomSummaries = LinkedHashMap<RoomCategory, List<RoomSummary>>

enum class RoomCategory(@StringRes val titleRes: Int) {
    INVITE(R.string.invitations_header),
    FAVOURITE(R.string.bottom_action_favourites),
    DIRECT(R.string.bottom_action_people_x),
    GROUP(R.string.bottom_action_rooms),
    LOW_PRIORITY(R.string.low_priority_header),
    SERVER_NOTICE(R.string.system_alerts_header)
}

fun RoomSummaries?.isNullOrEmpty(): Boolean {
    return this == null || this.values.flatten().isEmpty()
}