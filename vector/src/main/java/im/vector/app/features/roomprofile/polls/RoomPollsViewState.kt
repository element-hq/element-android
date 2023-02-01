/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.polls.list.ui.PollSummary

data class RoomPollsViewState(
        val roomId: String,
        val polls: List<PollSummary> = emptyList(),
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = true,
        val nbSyncedDays: Int = 0,
        val isSyncing: Boolean = false,
) : MavericksState {

    constructor(roomProfileArgs: RoomProfileArgs) : this(roomId = roomProfileArgs.roomId)

    fun hasNoPolls() = polls.isEmpty()
    fun hasNoPollsAndCanLoadMore() = !isSyncing && hasNoPolls() && canLoadMore
}
