/*
 * Copyright (c) 2020 New Vector Ltd
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
