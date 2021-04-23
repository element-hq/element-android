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

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.RoomGroupingMethod
import im.vector.app.features.home.RoomListDisplayMode
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo

data class RoomListViewState(
        val displayMode: RoomListDisplayMode,
        val roomFilter: String = "",
        val roomMembershipChanges: Map<String, ChangeMembershipState> = emptyMap(),
        val asyncSuggestedRooms: Async<List<SpaceChildInfo>> = Uninitialized,
        val currentUserName: String? = null,
        val currentRoomGrouping: Async<RoomGroupingMethod> = Uninitialized
) : MvRxState {

    constructor(args: RoomListParams) : this(displayMode = args.displayMode)
}
