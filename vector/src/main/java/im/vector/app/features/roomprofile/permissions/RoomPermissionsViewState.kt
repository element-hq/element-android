/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomPermissionsViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val actionPermissions: ActionPermissions = ActionPermissions(),
        val showAdvancedPermissions: Boolean = false,
        val currentPowerLevelsContent: Async<PowerLevelsContent> = Uninitialized,
        val isLoading: Boolean = false
) : MavericksState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    data class ActionPermissions(
            val canChangePowerLevels: Boolean = false
    )
}
