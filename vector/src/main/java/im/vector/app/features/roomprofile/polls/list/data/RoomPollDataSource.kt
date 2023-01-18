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

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.roomprofile.polls.list.ui.PollSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.poll.PollHistoryService
import timber.log.Timber
import javax.inject.Inject

// TODO add unit tests
class RoomPollDataSource @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    private val pollsFlow = MutableSharedFlow<List<PollSummary>>(replay = 1)
    private var fakeLoadCounter = 0

    private fun getPollHistoryService(roomId: String): PollHistoryService? {
        return activeSessionHolder
                .getSafeActiveSession()
                ?.getRoom(roomId)
                ?.pollHistoryService()
    }

    // TODO
    //  unmock using SDK service
    //  after unmock, expose domain layer model (entity) and do the mapping to PollSummary in the UI layer
    fun getPolls(roomId: String): Flow<List<PollSummary>> {
        Timber.d("roomId=$roomId")
        return pollsFlow.asSharedFlow()
    }

    fun getLoadedPollsStatus(roomId: String): LoadedPollsStatus {
        Timber.d("roomId=$roomId")
        // TODO unmock using SDK
        return LoadedPollsStatus(
                canLoadMore = canLoadMore(),
                nbSyncedDays = fakeLoadCounter * 30,
        )
    }

    private fun canLoadMore(): Boolean {
        return fakeLoadCounter < 2
    }

    suspend fun loadMorePolls(roomId: String): LoadedPollsStatus {
        return getPollHistoryService(roomId)?.loadMore() ?: throw PollHistoryError.LoadingError
    }

    suspend fun syncPolls(roomId: String) {
        Timber.d("roomId=$roomId")
        // TODO unmock using SDK service
        // fake sync
        delay(1000)
    }
}
