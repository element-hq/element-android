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

import androidx.annotation.StringRes
import com.airbnb.mvrx.MvRxState
import im.vector.app.R
import im.vector.app.features.home.RoomListDisplayMode
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomListViewState(
        val displayMode: RoomListDisplayMode,
        val roomFilter: String = "",
        val roomMembershipChanges: Map<String, ChangeMembershipState> = emptyMap()
) : MvRxState {

    constructor(args: RoomListParams) : this(displayMode = args.displayMode)

    val hasUnread: Boolean = false
//        get() = asyncFilteredRooms.invoke()
//                ?.flatMap { it.value }
//                ?.filter { it.membership == Membership.JOIN }
//                ?.any { it.hasUnreadMessages }
//                ?: false
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
