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

package im.vector.app.features.home.room.list.home

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import im.vector.app.features.home.room.list.home.filter.HomeRoomFilter
import kotlinx.coroutines.flow.SharedFlow
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.Optional

sealed class HomeRoomSection {
    data class RoomSummaryData(
            val list: LiveData<PagedList<RoomSummary>>,
            val filtersData: SharedFlow<Optional<List<HomeRoomFilter>>>,
    ) : HomeRoomSection()

    data class RecentRoomsData(
            val list: LiveData<List<RoomSummary>>
    ) : HomeRoomSection()

    data class InvitesCountData(
            val count: LiveData<Int>
    ) : HomeRoomSection()
}
