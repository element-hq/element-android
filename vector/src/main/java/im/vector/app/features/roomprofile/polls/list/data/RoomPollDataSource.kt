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

import androidx.lifecycle.asFlow
import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.poll.PollHistoryService
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class RoomPollDataSource @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    private fun getPollHistoryService(roomId: String): PollHistoryService {
        return activeSessionHolder
                .getSafeActiveSession()
                ?.getRoom(roomId)
                ?.pollHistoryService()
                ?: throw PollHistoryError.UnknownRoomError
    }

    fun dispose(roomId: String) {
        getPollHistoryService(roomId).dispose()
    }

    fun getPolls(roomId: String): Flow<List<TimelineEvent>> {
        return getPollHistoryService(roomId).getPollEvents().asFlow()
    }

    suspend fun getLoadedPollsStatus(roomId: String): LoadedPollsStatus {
        return getPollHistoryService(roomId).getLoadedPollsStatus()
    }

    suspend fun loadMorePolls(roomId: String): LoadedPollsStatus {
        return getPollHistoryService(roomId).loadMore()
    }

    suspend fun syncPolls(roomId: String) {
        getPollHistoryService(roomId).syncPolls()
    }
}
