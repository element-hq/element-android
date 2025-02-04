/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.invite

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.user.model.User

data class SpaceInviteBottomSheetState(
        val spaceId: String,
        val summary: Async<RoomSummary> = Uninitialized,
        val inviterUser: Async<User> = Uninitialized,
        val peopleYouKnow: Async<List<User>> = Uninitialized,
        val joinActionState: Async<Unit> = Uninitialized,
        val rejectActionState: Async<Unit> = Uninitialized
) : MavericksState {
    constructor(args: SpaceInviteBottomSheet.Args) : this(
            spaceId = args.spaceId
    )
}
