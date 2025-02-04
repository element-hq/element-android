/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.share

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class IncomingShareViewState(
        val sharedData: SharedData? = null,
        val roomSummaries: Async<List<RoomSummary>> = Uninitialized,
        val filteredRoomSummaries: Async<List<RoomSummary>> = Uninitialized,
        val selectedRoomIds: Set<String> = emptySet(),
        val isInMultiSelectionMode: Boolean = false
) : MavericksState
