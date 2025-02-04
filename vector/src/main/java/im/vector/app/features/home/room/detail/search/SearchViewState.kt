/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
