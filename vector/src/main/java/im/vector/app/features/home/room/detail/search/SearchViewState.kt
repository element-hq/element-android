/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.search

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.search.EventAndSender

data class SearchViewState(
        // Accumulated search result
        val searchResult: List<EventAndSender> = emptyList(),
        val highlights: List<String> = emptyList(),
        val hasMoreResult: Boolean = false,
        // Last batch size, will help RecyclerView to position itself
        val lastBatchSize: Int = 0,
        val searchTerm: String? = null,
        val roomId: String = "",
        // Current pagination request
        val asyncSearchRequest: Async<Unit> = Uninitialized
) : MavericksState {

    constructor(args: SearchArgs) : this(roomId = args.roomId)
}
