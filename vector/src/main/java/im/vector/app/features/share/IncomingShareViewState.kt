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

package im.vector.app.features.share

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class IncomingShareViewState(
        val sharedData: SharedData? = null,
        val roomSummaries: Async<List<RoomSummary>> = Uninitialized,
        val filteredRoomSummaries: Async<List<RoomSummary>> = Uninitialized,
        val selectedRoomIds: Set<String> = emptySet(),
        val isInMultiSelectionMode: Boolean = false
) : MavericksState
