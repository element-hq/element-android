/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.poll.create

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.poll.PollMode
import org.matrix.android.sdk.api.session.room.model.message.PollType

data class CreatePollViewState(
        val roomId: String,
        val editedEventId: String?,
        val mode: PollMode,
        val question: String = "",
        val options: List<String> = List(CreatePollViewModel.MIN_OPTIONS_COUNT) { "" },
        val canCreatePoll: Boolean = false,
        val canAddMoreOptions: Boolean = true,
        val pollType: PollType = PollType.DISCLOSED_UNSTABLE
) : MavericksState {

    constructor(args: CreatePollArgs) : this(
            roomId = args.roomId,
            editedEventId = args.editedEventId,
            mode = args.mode
    )
}
