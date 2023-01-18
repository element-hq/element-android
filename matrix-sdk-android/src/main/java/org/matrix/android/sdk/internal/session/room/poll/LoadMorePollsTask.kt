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
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.poll.PollConstants.MILLISECONDS_PER_DAY
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.session.room.timeline.PaginationResponse
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface LoadMorePollsTask : Task<LoadMorePollsTask.Params, LoadedPollsStatus> {
    data class Params(
            val roomId: String,
            val currentTimestampMs: Long,
            val loadingPeriodInDays: Int,
            val eventsPageSize: Int,
    )
}

internal class DefaultLoadMorePollsTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
) : LoadMorePollsTask {

    override suspend fun execute(params: LoadMorePollsTask.Params): LoadedPollsStatus {
        var currentPollHistoryStatus = updatePollHistoryStatus(params)

        while (shouldFetchMoreEventsBackward(currentPollHistoryStatus)) {
            currentPollHistoryStatus = fetchMorePollEventsBackward(params, currentPollHistoryStatus)
        }
        // TODO
        //   unmock and check how it behaves when cancelling the process: it should resume where it was stopped
        //   check the network calls done using Flipper
        //   check forward of error in case of call api failure

        return LoadedPollsStatus(
                canLoadMore = currentPollHistoryStatus.isEndOfPollsBackward.not(),
                nbSyncedDays = currentPollHistoryStatus.getNbSyncedDays(params.currentTimestampMs),
        )
    }

    private fun shouldFetchMoreEventsBackward(status: PollHistoryStatusEntity): Boolean {
        return status.currentTimestampTargetBackwardReached.not() && status.isEndOfPollsBackward.not()
    }

    private suspend fun updatePollHistoryStatus(params: LoadMorePollsTask.Params): PollHistoryStatusEntity {
        return monarchy.awaitTransaction { realm ->
            val status = PollHistoryStatusEntity.getOrCreate(realm, params.roomId)
            val currentTargetTimestampMs = status.currentTimestampTargetBackwardMs
            val lastTargetTimestampMs = status.oldestTimestampReachedMs
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

    private suspend fun fetchMorePollEventsBackward(
            params: LoadMorePollsTask.Params,
            status: PollHistoryStatusEntity
    ): PollHistoryStatusEntity {
        val chunk = executeRequest(globalErrorReceiver) {
            roomAPI.getRoomMessagesFrom(
                    roomId = params.roomId,
                    from = status.tokenEndBackward,
                    dir = PaginationDirection.BACKWARDS.value,
                    limit = params.eventsPageSize,
                    filter = null
            )
        }

        // TODO decrypt events and filter in only polls to store them in local: see to mutualize with FetchPollResponseEventsTask

        return updatePollHistoryStatus(roomId = params.roomId, paginationResponse = chunk)
    }

    private suspend fun updatePollHistoryStatus(roomId: String, paginationResponse: PaginationResponse): PollHistoryStatusEntity {
        return monarchy.awaitTransaction { realm ->
            val status = PollHistoryStatusEntity.getOrCreate(realm, roomId)
            val tokenStartForward = status.tokenStartForward

            if (tokenStartForward == null) {
                // save the start token for next forward call
                status.tokenEndBackward = paginationResponse.start
            }

            val oldestEventTimestamp = paginationResponse.events
                    .minByOrNull { it.originServerTs ?: Long.MAX_VALUE }
                    ?.originServerTs

            val currentTargetTimestamp = status.currentTimestampTargetBackwardMs

            if (paginationResponse.end == null) {
                // start of the timeline is reached, there are no more events
                status.isEndOfPollsBackward = true
                status.oldestTimestampReachedMs = oldestEventTimestamp
            } else if(oldestEventTimestamp != null && currentTargetTimestamp != null && oldestEventTimestamp <= currentTargetTimestamp) {
                // target has been reached
                status.oldestTimestampReachedMs = oldestEventTimestamp
                status.tokenEndBackward = paginationResponse.end
            } else {
                status.tokenEndBackward = paginationResponse.end
            }

            // return a copy of the Realm object
            status.copy()
        }
    }
}
