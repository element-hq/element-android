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
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.poll.PollConstants.MILLISECONDS_PER_DAY
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface LoadMorePollsTask : Task<LoadMorePollsTask.Params, LoadedPollsStatus> {
    data class Params(
            val timeline: Timeline,
            val roomId: String,
            val currentTimestampMs: Long,
            val loadingPeriodInDays: Int,
            val eventsPageSize: Int,
    )
}

internal class DefaultLoadMorePollsTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) : LoadMorePollsTask {

    override suspend fun execute(params: LoadMorePollsTask.Params): LoadedPollsStatus {
        var currentPollHistoryStatus = updatePollHistoryStatus(params)

        params.timeline.restartWithEventId(eventId = currentPollHistoryStatus.oldestEventIdReached)

        while (shouldFetchMoreEventsBackward(currentPollHistoryStatus)) {
            currentPollHistoryStatus = fetchMorePollEventsBackward(params)
        }

        return LoadedPollsStatus(
                canLoadMore = currentPollHistoryStatus.isEndOfPollsBackward.not(),
                daysSynced = currentPollHistoryStatus.getNbSyncedDays(params.currentTimestampMs),
                hasCompletedASyncBackward = currentPollHistoryStatus.hasCompletedASyncBackward,
        )
    }

    private fun shouldFetchMoreEventsBackward(status: PollHistoryStatusEntity): Boolean {
        return status.currentTimestampTargetBackwardReached.not() && status.isEndOfPollsBackward.not()
    }

    private suspend fun updatePollHistoryStatus(params: LoadMorePollsTask.Params): PollHistoryStatusEntity {
        return monarchy.awaitTransaction { realm ->
            val status = PollHistoryStatusEntity.getOrCreate(realm, params.roomId)
            val currentTargetTimestampMs = status.currentTimestampTargetBackwardMs
            val lastTargetTimestampMs = status.oldestTimestampTargetReachedMs
            val loadingPeriodMs: Long = MILLISECONDS_PER_DAY * params.loadingPeriodInDays.toLong()
            if (currentTargetTimestampMs == null) {
                // first load, compute the target timestamp
                status.currentTimestampTargetBackwardMs = params.currentTimestampMs - loadingPeriodMs
            } else if (lastTargetTimestampMs != null && status.currentTimestampTargetBackwardReached) {
                // previous load has finished, update the target timestamp
                status.currentTimestampTargetBackwardMs = lastTargetTimestampMs - loadingPeriodMs
            }
            // return a copy of the Realm object
            status.copy()
        }
    }

    private suspend fun fetchMorePollEventsBackward(params: LoadMorePollsTask.Params): PollHistoryStatusEntity {
        val events = params.timeline.awaitPaginate(
                direction = Timeline.Direction.BACKWARDS,
                count = params.eventsPageSize,
        )

        val paginationState = params.timeline.getPaginationState(direction = Timeline.Direction.BACKWARDS)

        return updatePollHistoryStatus(
                roomId = params.roomId,
                events = events,
                paginationState = paginationState,
        )
    }

    private suspend fun updatePollHistoryStatus(
            roomId: String,
            events: List<TimelineEvent>,
            paginationState: Timeline.PaginationState,
    ): PollHistoryStatusEntity {
        return monarchy.awaitTransaction { realm ->
            val status = PollHistoryStatusEntity.getOrCreate(realm, roomId)
            val mostRecentEventIdReached = status.mostRecentEventIdReached

            if (mostRecentEventIdReached == null) {
                // save it for next forward pagination
                val mostRecentEvent = events
                        .maxByOrNull { it.root.originServerTs ?: Long.MIN_VALUE }
                        ?.root
                status.mostRecentEventIdReached = mostRecentEvent?.eventId
            }

            val oldestEvent = events
                    .minByOrNull { it.root.originServerTs ?: Long.MAX_VALUE }
                    ?.root
            val oldestEventTimestamp = oldestEvent?.originServerTs
            val oldestEventId = oldestEvent?.eventId

            val currentTargetTimestamp = status.currentTimestampTargetBackwardMs

            if (paginationState.hasMoreToLoad.not()) {
                // start of the timeline is reached, there are no more events
                status.isEndOfPollsBackward = true

                if (oldestEventTimestamp != null && oldestEventTimestamp > 0) {
                    status.oldestTimestampTargetReachedMs = oldestEventTimestamp
                }
            } else if (oldestEventTimestamp != null && currentTargetTimestamp != null && oldestEventTimestamp <= currentTargetTimestamp) {
                // target has been reached
                status.oldestTimestampTargetReachedMs = oldestEventTimestamp
            }

            if (oldestEventId != null) {
                // save it for next backward pagination
                status.oldestEventIdReached = oldestEventId
            }

            // return a copy of the Realm object
            status.copy()
        }
    }
}
