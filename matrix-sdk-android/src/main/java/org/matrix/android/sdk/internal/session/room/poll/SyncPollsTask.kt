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

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface SyncPollsTask : Task<SyncPollsTask.Params, Unit> {
    data class Params(
            val timeline: Timeline,
            val roomId: String,
            val currentTimestampMs: Long,
            val eventsPageSize: Int,
    )
}

internal class DefaultSyncPollsTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) : SyncPollsTask {

    override suspend fun execute(params: SyncPollsTask.Params) {
        val currentPollHistoryStatus = getCurrentPollHistoryStatus(params.roomId)

        params.timeline.restartWithEventId(currentPollHistoryStatus.mostRecentEventIdReached)

        var loadStatus = LoadStatus(shouldLoadMore = true)
        while (loadStatus.shouldLoadMore) {
            loadStatus = fetchMorePollEventsForward(params)
        }

        params.timeline.restartWithEventId(currentPollHistoryStatus.oldestEventIdReached)
    }

    private suspend fun getCurrentPollHistoryStatus(roomId: String): PollHistoryStatusEntity {
        return monarchy.awaitTransaction { realm ->
            PollHistoryStatusEntity
                    .getOrCreate(realm, roomId)
                    .copy()
        }
    }

    private suspend fun fetchMorePollEventsForward(params: SyncPollsTask.Params): LoadStatus {
        val events = params.timeline.awaitPaginate(
                direction = Timeline.Direction.FORWARDS,
                count = params.eventsPageSize,
        )

        val paginationState = params.timeline.getPaginationState(direction = Timeline.Direction.FORWARDS)

        return updatePollHistoryStatus(
                roomId = params.roomId,
                currentTimestampMs = params.currentTimestampMs,
                events = events,
                paginationState = paginationState,
        )
    }

    private suspend fun updatePollHistoryStatus(
            roomId: String,
            currentTimestampMs: Long,
            events: List<TimelineEvent>,
            paginationState: Timeline.PaginationState,
    ): LoadStatus {
        return monarchy.awaitTransaction { realm ->
            val status = PollHistoryStatusEntity.getOrCreate(realm, roomId)
            val mostRecentEvent = events
                    .maxByOrNull { it.root.originServerTs ?: Long.MIN_VALUE }
                    ?.root
            val mostRecentEventIdReached = mostRecentEvent?.eventId

            if (mostRecentEventIdReached != null) {
                // save it for next forward pagination
                status.mostRecentEventIdReached = mostRecentEventIdReached
            }

            val mostRecentTimestamp = mostRecentEvent?.originServerTs

            val shouldLoadMore = paginationState.hasMoreToLoad &&
                    (mostRecentTimestamp == null || mostRecentTimestamp < currentTimestampMs)

            LoadStatus(shouldLoadMore = shouldLoadMore)
        }
    }

    private class LoadStatus(
            val shouldLoadMore: Boolean,
    )
}
