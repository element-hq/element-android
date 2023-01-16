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

private const val MILLISECONDS_PER_DAY = 24 * 60 * 60_000

internal class DefaultLoadMorePollsTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) : LoadMorePollsTask {

    override suspend fun execute(params: LoadMorePollsTask.Params): LoadedPollsStatus {
        updatePollHistoryStatus(params)

        return LoadedPollsStatus(
                canLoadMore = true,
                nbLoadedDays = 10,
        )
    }

    private suspend fun updatePollHistoryStatus(params: LoadMorePollsTask.Params) {
        monarchy.awaitTransaction { realm ->
            val status = PollHistoryStatusEntity.getOrCreate(realm, params.roomId)
            val currentTargetTimestamp = status.currentTimestampTargetBackwardMs
            val loadingPeriodMs = MILLISECONDS_PER_DAY * params.loadingPeriodInDays
            if (currentTargetTimestamp == null) {
                // first load, compute the target timestamp
                status.currentTimestampTargetBackwardMs = params.currentTimestampMs - loadingPeriodMs
            } else if (status.currentTimestampTargetBackwardReached) {
                // previous load has finished, update the target timestamp
                status.currentTimestampTargetBackwardMs = currentTargetTimestamp - loadingPeriodMs
                status.currentTimestampTargetBackwardReached = false
            }
        }
    }
}
