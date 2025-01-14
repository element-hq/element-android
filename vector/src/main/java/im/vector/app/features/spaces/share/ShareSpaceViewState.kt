/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.share

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class ShareSpaceViewState(
        val spaceId: String,
        val spaceSummary: Async<RoomSummary> = Uninitialized,
        val canInviteByMxId: Boolean = false,
        val canShareLink: Boolean = false,
        val postCreation: Boolean = false
) : MavericksState {
    constructor(args: ShareSpaceBottomSheet.Args) : this(
            spaceId = args.spaceId,
            postCreation = args.postCreation
    )
}
