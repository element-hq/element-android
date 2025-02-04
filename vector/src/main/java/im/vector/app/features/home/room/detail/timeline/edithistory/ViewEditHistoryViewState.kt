/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.edithistory

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.home.room.detail.timeline.action.TimelineEventFragmentArgs
import org.matrix.android.sdk.api.session.events.model.Event

data class ViewEditHistoryViewState(
        val eventId: String,
        val roomId: String,
        val isOriginalAReply: Boolean = false,
        val editList: Async<List<Event>> = Uninitialized
) :
        MavericksState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId)
}
