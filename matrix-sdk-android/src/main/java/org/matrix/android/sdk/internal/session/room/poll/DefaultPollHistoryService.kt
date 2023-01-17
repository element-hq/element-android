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
import org.matrix.android.sdk.api.session.room.model.PollResponseAggregatedSummary
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.poll.PollHistoryService
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber

private const val LOADING_PERIOD_IN_DAYS = 30
private const val EVENTS_PAGE_SIZE = 250

// TODO add unit tests
internal class DefaultPollHistoryService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val clock: Clock,
        private val loadMorePollsTask: LoadMorePollsTask,
) : PollHistoryService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultPollHistoryService
    }

    init {
        Timber.d("init with roomId: $roomId")
    }

    override val loadingPeriodInDays: Int
        get() = LOADING_PERIOD_IN_DAYS

    override suspend fun loadMore(): LoadedPollsStatus {
        // TODO when to set currentTimestampMs and who is responsible for it?
        val params = LoadMorePollsTask.Params(
                roomId = roomId,
                currentTimestampMs = clock.epochMillis(),
                loadingPeriodInDays = loadingPeriodInDays,
                eventsPageSize = EVENTS_PAGE_SIZE,
        )
        return loadMorePollsTask.execute(params)
    }

    override fun canLoadMore(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getLoadedPollsStatus(): LoadedPollsStatus {
        TODO("Not yet implemented")
    }

    override suspend fun syncPolls() {
        TODO("Not yet implemented")
    }

    override fun getPolls(): LiveData<List<PollResponseAggregatedSummary>> {
        TODO("Not yet implemented")
    }
}
