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

package im.vector.app.features.spaces.leave

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.spaces.SpaceBottomSheetSettingsArgs
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class SpaceLeaveAdvanceViewState(
        val spaceId: String,
        val spaceSummary: RoomSummary? = null,
        val allChildren: Async<List<RoomSummary>> = Uninitialized,
        val selectedRooms: List<String> = emptyList(),
        val currentFilter: String = "",
        val leaveState: Async<Unit> = Uninitialized
) : MavericksState {
    constructor(args: SpaceBottomSheetSettingsArgs) : this(
            spaceId = args.spaceId
    )
}
