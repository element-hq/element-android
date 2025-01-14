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

data class SpaceAddRoomsState(
        // The current filter
        val spaceId: String = "",
        val currentFilter: String = "",
        val spaceName: String = "",
        val ignoreRooms: List<String> = emptyList(),
        val isSaving: Async<List<String>> = Uninitialized,
        val shouldShowDMs: Boolean = false,
        val onlyShowSpaces: Boolean = false
//        val selectionList: Map<String, Boolean> = emptyMap()
) : MavericksState {
    constructor(args: SpaceManageArgs) : this(
            spaceId = args.spaceId,
            onlyShowSpaces = args.manageType == ManageType.AddRoomsOnlySpaces
    )
}
