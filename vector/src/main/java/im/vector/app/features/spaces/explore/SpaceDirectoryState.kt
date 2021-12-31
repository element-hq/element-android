/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.spaces.explore

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.space.SpaceHierarchyData

data class SpaceDirectoryState(
        // The current filter
        val spaceId: String,
        val currentFilter: String = "",
        val apiResults: Map<String, Async<SpaceHierarchyData>> = emptyMap(),
        val currentRootSummary: RoomSummary? = null,
        val childList: List<SpaceChildInfo> = emptyList(),
        val hierarchyStack: List<String> = emptyList(),
        // Set of joined roomId / spaces,
        val joinedRoomsIds: Set<String> = emptySet(),
        // keys are room alias or roomId
        val changeMembershipStates: Map<String, ChangeMembershipState> = emptyMap(),
        val canAddRooms: Boolean = false,
        // cached room summaries of known rooms, we use it because computed room name would be better using it
        val knownRoomSummaries: List<RoomSummary> = emptyList(),
        val paginationStatus: Map<String, Async<Unit>> = emptyMap()
) : MavericksState {
    constructor(args: SpaceDirectoryArgs) : this(
            spaceId = args.spaceId
    )
}
