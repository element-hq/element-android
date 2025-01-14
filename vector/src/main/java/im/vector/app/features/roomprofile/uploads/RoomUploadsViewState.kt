/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.uploads

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.uploads.UploadEvent

data class RoomUploadsViewState(
        val roomId: String = "",
        val roomSummary: Async<RoomSummary> = Uninitialized,
        // Store cumul of pagination result, grouped by type
        val mediaEvents: List<UploadEvent> = emptyList(),
        val fileEvents: List<UploadEvent> = emptyList(),
        // Current pagination request
        val asyncEventsRequest: Async<Unit> = Uninitialized,
        // True if more result are available server side
        val hasMore: Boolean = true
) : MavericksState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)
}
