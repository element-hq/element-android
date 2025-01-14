/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.joinrule.advanced

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.settings.joinrule.JoinRulesOptionSupport
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesAllowEntry
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem

data class RoomJoinRuleChooseRestrictedState(
        // the currentRoomId
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val initialRoomJoinRules: RoomJoinRules? = null,
        val currentRoomJoinRules: RoomJoinRules? = null,
        val updatedAllowList: List<MatrixItem> = emptyList(),
        val choices: List<JoinRulesOptionSupport>? = null,
        val initialAllowList: List<RoomJoinRulesAllowEntry> = emptyList(),
        val possibleSpaceCandidate: List<MatrixItem> = emptyList(),
        val unknownRestricted: List<MatrixItem> = emptyList(),
        val filter: String = "",
        val filteredResults: Async<List<MatrixItem>> = Uninitialized,
        val hasUnsavedChanges: Boolean = false,
        val updatingStatus: Async<Unit> = Uninitialized,
        val upgradeNeededForRestricted: Boolean = false,
        val restrictedSupportedByThisVersion: Boolean = false,
        val restrictedVersionNeeded: String? = null,
        val didSwitchToReplacementRoom: Boolean = false
) : MavericksState {
    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)
}
