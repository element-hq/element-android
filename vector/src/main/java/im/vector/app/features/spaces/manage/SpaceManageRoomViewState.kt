/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.SpaceHierarchyData

data class SpaceManageRoomViewState(
        val spaceId: String,
        val spaceSummary: Async<RoomSummary> = Uninitialized,
        val childrenInfo: Async<SpaceHierarchyData> = Uninitialized,
        val selectedRooms: List<String> = emptyList(),
        val currentFilter: String = "",
        val actionState: Async<Unit> = Uninitialized,
        val paginationStatus: Async<Unit> = Uninitialized,
        // cached room summaries of known rooms, we use it because computed room name would be better using it
        val knownRoomSummaries: List<RoomSummary> = emptyList()
) : MavericksState {
    constructor(args: SpaceManageArgs) : this(
            spaceId = args.spaceId
    )
}
