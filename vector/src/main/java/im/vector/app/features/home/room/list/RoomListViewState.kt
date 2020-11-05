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
import im.vector.app.features.home.room.ScSdkPreferences
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary

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
        val isServerNoticeRoomsExpanded: Boolean = true,
        val scSdkPreferences: ScSdkPreferences? = null
) : MvRxState {

    companion object {
        const val ROOM_LIST_ROOM_EXPANDED_LOW_PRIORITY_PREFIX = "ROOM_LIST_ROOM_EXPANDED_LOW_PRIORITY_"
        const val ROOM_LIST_ROOM_EXPANDED_NORMAL_PRIORITY_PREFIX = "ROOM_LIST_ROOM_EXPANDED_NORMAL_PRIORITY_"
        const val ROOM_LIST_ROOM_EXPANDED_FAVORITE_PREFIX = "ROOM_LIST_ROOM_EXPANDED_FAVORITE_"
        const val ROOM_LIST_ROOM_EXPANDED_DIRECT_PREFIX = "ROOM_LIST_ROOM_EXPANDED_DIRECT_"
        const val ROOM_LIST_ROOM_EXPANDED_GROUP_PREFIX = "ROOM_LIST_ROOM_EXPANDED_GROUP_"
        const val ROOM_LIST_ROOM_EXPANDED_SERVER_NOTICE_PREFIX = "ROOM_LIST_ROOM_EXPANDED_SERVER_NOTICE_"
    }

    constructor(args: RoomListParams) : this(displayMode = args.displayMode)

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
    private fun getRoomListExpandedPref(prefix: String, displayMode: RoomListDisplayMode): String {
        return prefix + displayMode.toString()
    }

    // SC addition
    fun initWithContext(context: Context, displayMode: RoomListDisplayMode): RoomListViewState {
        val sp = getSharedPreferences(context)
        val prefLowPrio = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_LOW_PRIORITY_PREFIX, displayMode)
        val prefNormalPrio = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_NORMAL_PRIORITY_PREFIX, displayMode)
        val prefFavourite = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_FAVORITE_PREFIX, displayMode)
        val prefDirect = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_DIRECT_PREFIX, displayMode)
        val prefGroup = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_GROUP_PREFIX, displayMode)
        val prefServerNotice = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_SERVER_NOTICE_PREFIX, displayMode)
        val scSdkPreferences = ScSdkPreferences(context)
        return copy(isLowPriorityRoomsExpanded = sp.getBoolean(prefLowPrio, isLowPriorityRoomsExpanded),
                isCombinedRoomsExpanded = sp.getBoolean(prefNormalPrio, isCombinedRoomsExpanded),
                isFavouriteRoomsExpanded = sp.getBoolean(prefFavourite, isFavouriteRoomsExpanded),
                isDirectRoomsExpanded = sp.getBoolean(prefDirect, isDirectRoomsExpanded),
                isGroupRoomsExpanded = sp.getBoolean(prefGroup, isGroupRoomsExpanded),
                isServerNoticeRoomsExpanded = sp.getBoolean(prefServerNotice, isServerNoticeRoomsExpanded),
                scSdkPreferences = scSdkPreferences)
    }

    // SC addition
    fun persistWithContext(context: Context, displayMode: RoomListDisplayMode) {
        val sp = getSharedPreferences(context)
        val prefLowPrio = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_LOW_PRIORITY_PREFIX, displayMode)
        val prefNormalPrio = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_NORMAL_PRIORITY_PREFIX, displayMode)
        val prefFavourite = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_FAVORITE_PREFIX, displayMode)
        val prefDirect = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_DIRECT_PREFIX, displayMode)
        val prefGroup = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_GROUP_PREFIX, displayMode)
        val prefServerNotice = getRoomListExpandedPref(ROOM_LIST_ROOM_EXPANDED_SERVER_NOTICE_PREFIX, displayMode)
        sp.edit()
                .putBoolean(prefLowPrio, isLowPriorityRoomsExpanded)
                .putBoolean(prefNormalPrio, isCombinedRoomsExpanded)
                .putBoolean(prefFavourite, isFavouriteRoomsExpanded)
                .putBoolean(prefDirect, isDirectRoomsExpanded)
                .putBoolean(prefGroup, isGroupRoomsExpanded)
                .putBoolean(prefServerNotice, isServerNoticeRoomsExpanded)
                .apply()
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
                ?.any { it.scHasUnreadMessages(scSdkPreferences) }
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
