/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
