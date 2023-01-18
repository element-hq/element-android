/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.poll

import androidx.lifecycle.LiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import org.matrix.android.sdk.api.session.room.model.PollResponseAggregatedSummary
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.poll.PollHistoryService
import org.matrix.android.sdk.internal.util.time.Clock

private const val LOADING_PERIOD_IN_DAYS = 30
private const val EVENTS_PAGE_SIZE = 250

// TODO add unit tests
internal class DefaultPollHistoryService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val clock: Clock,
        private val loadMorePollsTask: LoadMorePollsTask,
        private val getLoadedPollsStatusTask: GetLoadedPollsStatusTask,
) : PollHistoryService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultPollHistoryService
    }

    override val loadingPeriodInDays: Int
        get() = LOADING_PERIOD_IN_DAYS

    override suspend fun loadMore(): LoadedPollsStatus {
        val params = LoadMorePollsTask.Params(
                roomId = roomId,
                currentTimestampMs = clock.epochMillis(),
                loadingPeriodInDays = loadingPeriodInDays,
                eventsPageSize = EVENTS_PAGE_SIZE,
        )
        return loadMorePollsTask.execute(params)
    }

    override suspend fun getLoadedPollsStatus(): LoadedPollsStatus {
        val params = GetLoadedPollsStatusTask.Params(
                roomId = roomId,
                currentTimestampMs = clock.epochMillis(),
        )
        return getLoadedPollsStatusTask.execute(params)
    }

    override suspend fun syncPolls() {
        // TODO unmock
        delay(1000)
    }

    override fun getPolls(): LiveData<List<PollResponseAggregatedSummary>> {
        TODO("listen database and update query depending on latest PollHistoryStatusEntity.oldestTimestampReachedMs")
    }
}
