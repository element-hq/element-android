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

package im.vector.app.features.home.room.list

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.matrix.android.api.session.room.members.ChangeMembershipState
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary

data class RoomListViewState(
        val displayMode: RoomListDisplayMode,
        val asyncRooms: Async<List<RoomSummary>> = Uninitialized,
        val roomFilter: String = "",
        val asyncFilteredRooms: Async<RoomSummaries> = Uninitialized,
        val roomMembershipChanges: Map<String, ChangeMembershipState> = emptyMap(),
        val isInviteExpanded: Boolean = true,
        val isFavouriteRoomsExpanded: Boolean = true,
        val isDirectRoomsExpanded: Boolean = true,
        val isGroupRoomsExpanded: Boolean = true,
        val isCombinedRoomsExpanded: Boolean = true,
        val isLowPriorityRoomsExpanded: Boolean = true,
        val isServerNoticeRoomsExpanded: Boolean = true
) : MvRxState {

    companion object {
        const val ROOM_LIST_ROOM_EXPANDED_LOW_PRIORITY_PREFIX = "ROOM_LIST_ROOM_EXPANDED_LOW_PRIORITY_"
    }

    constructor(args: RoomListParams) : this(displayMode = args.displayMode)

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
    private fun getRoomListExpandedLowPriorityPref(displayMode: RoomListDisplayMode): String {
        return ROOM_LIST_ROOM_EXPANDED_LOW_PRIORITY_PREFIX + displayMode.toString()
    }

    fun initWithContext(context: Context, displayMode: RoomListDisplayMode): RoomListViewState {
        val sp = getSharedPreferences(context)
        val pref = getRoomListExpandedLowPriorityPref(displayMode)
        return copy(isLowPriorityRoomsExpanded = sp.getBoolean(pref, isLowPriorityRoomsExpanded))
    }

    fun persistWithContext(context: Context, displayMode: RoomListDisplayMode) {
        val sp = getSharedPreferences(context)
        val pref = getRoomListExpandedLowPriorityPref(displayMode)
        sp.edit().putBoolean(pref, isLowPriorityRoomsExpanded).apply()
    }

    fun isCategoryExpanded(roomCategory: RoomCategory): Boolean {
        return when (roomCategory) {
            RoomCategory.INVITE        -> isInviteExpanded
            RoomCategory.FAVOURITE     -> isFavouriteRoomsExpanded
            RoomCategory.DIRECT        -> isDirectRoomsExpanded
            RoomCategory.GROUP         -> isGroupRoomsExpanded
            RoomCategory.COMBINED      -> isCombinedRoomsExpanded
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
            RoomCategory.COMBINED      -> copy(isCombinedRoomsExpanded = !isCombinedRoomsExpanded)
            RoomCategory.LOW_PRIORITY  -> copy(isLowPriorityRoomsExpanded = !isLowPriorityRoomsExpanded)
            RoomCategory.SERVER_NOTICE -> copy(isServerNoticeRoomsExpanded = !isServerNoticeRoomsExpanded)
        }
    }

    val hasUnread: Boolean
        get() = asyncFilteredRooms.invoke()
                ?.flatMap { it.value }
                ?.filter { it.membership == Membership.JOIN }
                ?.any { it.hasUnreadMessages }
                ?: false
}

typealias RoomSummaries = LinkedHashMap<RoomCategory, List<RoomSummary>>

enum class RoomCategory(@StringRes val titleRes: Int) {
    INVITE(R.string.invitations_header),
    FAVOURITE(R.string.bottom_action_favourites),
    DIRECT(R.string.bottom_action_people_x),
    GROUP(R.string.bottom_action_rooms),
    COMBINED(R.string.normal_priority_header),
    LOW_PRIORITY(R.string.low_priority_header),
    SERVER_NOTICE(R.string.system_alerts_header)
}

fun RoomSummaries?.isNullOrEmpty(): Boolean {
    return this == null || this.values.flatten().isEmpty()
}
