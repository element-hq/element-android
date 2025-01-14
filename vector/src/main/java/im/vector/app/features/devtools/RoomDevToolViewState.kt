/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.devtools

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.events.model.Event

data class RoomDevToolViewState(
        val roomId: String = "",
        val displayMode: Mode = Mode.Root,
        val stateEvents: Async<List<Event>> = Uninitialized,
        val currentStateType: String? = null,
        val selectedEvent: Event? = null,
        val selectedEventJson: String? = null,
        val editedContent: String? = null,
        val modalLoading: Async<Unit> = Uninitialized,
        val sendEventDraft: SendEventDraft? = null
) : MavericksState {

    constructor(args: RoomDevToolActivity.Args) : this(roomId = args.roomId, displayMode = Mode.Root)

    sealed class Mode {
        object Root : Mode()
        object StateEventList : Mode()
        object StateEventListByType : Mode()
        object StateEventDetail : Mode()
        object EditEventContent : Mode()
        data class SendEventForm(val isState: Boolean) : Mode()
    }

    data class SendEventDraft(
            val type: String?,
            val stateKey: String?,
            val content: String?
    )
}
