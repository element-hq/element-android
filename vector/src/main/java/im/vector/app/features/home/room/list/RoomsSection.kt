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

package im.vector.app.features.home.room.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount

data class RoomsSection(
        val sectionName: String,
        // can be a paged list or a regular list
        val livePages: LiveData<PagedList<RoomSummary>>? = null,
        val liveList: LiveData<List<RoomSummary>>? = null,
        val liveSuggested: LiveData<SuggestedRoomInfo>? = null,
        val isExpanded: MutableLiveData<Boolean> = MutableLiveData(true),
        val itemCount: Flow<Int>,
        val notificationCount: MutableLiveData<RoomAggregateNotificationCount> = MutableLiveData(RoomAggregateNotificationCount(0, 0)),
        val notifyOfLocalEcho: Boolean = false
)
