/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.banned

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomBannedMemberListViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val bannedMemberSummaries: Async<List<RoomMemberSummary>> = Uninitialized,
        val filter: String = "",
        val onGoingModerationAction: List<String> = emptyList(),
        val canUserBan: Boolean = false
) : MavericksState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)
}
