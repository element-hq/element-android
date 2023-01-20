/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.list.data

import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class RoomPollRepository @Inject constructor(
        private val roomPollDataSource: RoomPollDataSource,
) {

    fun dispose(roomId: String) {
        roomPollDataSource.dispose(roomId)
    }

    fun getPolls(roomId: String): Flow<List<TimelineEvent>> {
        return roomPollDataSource.getPolls(roomId)
    }

    suspend fun getLoadedPollsStatus(roomId: String): LoadedPollsStatus {
        return roomPollDataSource.getLoadedPollsStatus(roomId)
    }

    suspend fun loadMorePolls(roomId: String): LoadedPollsStatus {
        return roomPollDataSource.loadMorePolls(roomId)
    }

    suspend fun syncPolls(roomId: String) {
        return roomPollDataSource.syncPolls(roomId)
    }
}
