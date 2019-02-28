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

package im.vector.riotredesign.features.home.room.list

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R

data class RoomListViewState(
        val asyncRooms: Async<RoomSummaries> = Uninitialized,
        val visibleRoomId: String? = null,
        val isFavouriteRoomsExpanded: Boolean = true,
        val isDirectRoomsExpanded: Boolean = false,
        val isGroupRoomsExpanded: Boolean = false,
        val isLowPriorityRoomsExpanded: Boolean = false,
        val isServerNoticeRoomsExpanded: Boolean = false
) : MvRxState {

    fun isCategoryExpanded(roomCategory: RoomCategory): Boolean {
        return when (roomCategory) {
            RoomCategory.FAVOURITE     -> isFavouriteRoomsExpanded
            RoomCategory.DIRECT        -> isDirectRoomsExpanded
            RoomCategory.GROUP         -> isGroupRoomsExpanded
            RoomCategory.LOW_PRIORITY  -> isLowPriorityRoomsExpanded
            RoomCategory.SERVER_NOTICE -> isServerNoticeRoomsExpanded
        }
    }

    fun toggle(roomCategory: RoomCategory): RoomListViewState {
        return when (roomCategory) {
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
    FAVOURITE(R.string.room_list_favourites),
    DIRECT(R.string.room_list_direct),
    GROUP(R.string.room_list_group),
    LOW_PRIORITY(R.string.room_list_low_priority),
    SERVER_NOTICE(R.string.room_list_system_alert)
}

fun RoomSummaries?.isNullOrEmpty(): Boolean {
    return this == null || isEmpty()
}