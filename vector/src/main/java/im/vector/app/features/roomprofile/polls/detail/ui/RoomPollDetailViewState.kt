/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import com.airbnb.mvrx.MavericksState

data class RoomPollDetailViewState(
        val pollId: String,
        val roomId: String,
        val pollDetail: RoomPollDetail? = null,
) : MavericksState {

    constructor(roomPollDetailArgs: RoomPollDetailArgs) : this(
            pollId = roomPollDetailArgs.pollId,
            roomId = roomPollDetailArgs.roomId,
    )
}
