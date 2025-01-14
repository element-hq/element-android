/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import com.airbnb.mvrx.MavericksState

enum class ManageType {
    AddRooms,
    AddRoomsOnlySpaces,
    Settings,
    ManageRooms
}

data class SpaceManageViewState(
        val spaceId: String = "",
        val manageType: ManageType
) : MavericksState {
    constructor(args: SpaceManageArgs) : this(
            spaceId = args.spaceId,
            manageType = args.manageType
    )
}
