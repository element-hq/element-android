/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.invite

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized

data class InviteUsersToRoomViewState(
        val roomId: String,
        val inviteState: Async<Unit> = Uninitialized
) : MavericksState {

    constructor(args: InviteUsersToRoomArgs) : this(roomId = args.roomId)
}
