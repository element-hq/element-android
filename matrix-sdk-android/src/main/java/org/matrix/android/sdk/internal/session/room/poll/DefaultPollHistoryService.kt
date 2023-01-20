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
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.kotlin.where
import kotlinx.coroutines.delay
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.poll.PollHistoryService
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntity
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber

private const val LOADING_PERIOD_IN_DAYS = 30
private const val EVENTS_PAGE_SIZE = 250

// TODO add unit tests
internal class DefaultPollHistoryService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val clock: Clock,
        private val loadMorePollsTask: LoadMorePollsTask,
        private val getLoadedPollsStatusTask: GetLoadedPollsStatusTask,
        private val timelineEventMapper: TimelineEventMapper,
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

    override fun getPollEvents(): LiveData<List<TimelineEvent>> {
        val pollHistoryStatusLiveData = getPollHistoryStatus()

        return Transformations.switchMap(pollHistoryStatusLiveData) { results ->
            val oldestTimestamp = results.firstOrNull()?.oldestTimestampReachedMs ?: clock.epochMillis()
            Timber.d("oldestTimestamp=$oldestTimestamp")
            getPollStartEventsAfter(oldestTimestamp)
        }
    }

    private fun getPollStartEventsAfter(timestampMs: Long): LiveData<List<TimelineEvent>> {
        val eventsLiveData = monarchy.findAllMappedWithChanges(
                { realm ->
                    val pollTypes = (EventType.POLL_START.values + EventType.ENCRYPTED).toTypedArray()
                    realm.where<TimelineEventEntity>()
                            .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
                            .`in`(TimelineEventEntityFields.ROOT.TYPE, pollTypes)
                            .greaterThan(TimelineEventEntityFields.ROOT.ORIGIN_SERVER_TS, timestampMs)
                },
                { result ->
                    timelineEventMapper.map(result, buildReadReceipts = false)
                }
        )

        return Transformations.map(eventsLiveData) { events ->
            events.filter { it.root.getClearType() in EventType.POLL_START.values }
        }
    }

    private fun getPollHistoryStatus(): LiveData<List<PollHistoryStatusEntity>> {
        return monarchy.findAllMappedWithChanges(
                { realm ->
                    realm.where<PollHistoryStatusEntity>()
                            .equalTo(PollHistoryStatusEntityFields.ROOM_ID, roomId)
                },
                { result ->
                    // make a copy of the Realm object since it will be used in another transformations
                    result.copy()
                }
        )
    }
}
