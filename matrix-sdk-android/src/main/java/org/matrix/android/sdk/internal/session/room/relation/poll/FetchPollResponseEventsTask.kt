/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.relation.poll

import androidx.annotation.VisibleForTesting
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.isPollResponse
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.event.FilterAndStoreEventsTask
import org.matrix.android.sdk.internal.session.room.relation.RelationsResponse
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

@VisibleForTesting
const val FETCH_RELATED_EVENTS_LIMIT = 50

/**
 * Task to fetch all the vote events to ensure full aggregation for a given poll.
 */
internal interface FetchPollResponseEventsTask : Task<FetchPollResponseEventsTask.Params, Result<Unit>> {
    data class Params(
            val roomId: String,
            val startPollEventId: String,
    )
}

internal class DefaultFetchPollResponseEventsTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val filterAndStoreEventsTask: FilterAndStoreEventsTask,

        ) : FetchPollResponseEventsTask {

    override suspend fun execute(params: FetchPollResponseEventsTask.Params): Result<Unit> = runCatching {
        var nextBatch: String? = fetchAndProcessRelatedEventsFrom(params)

        while (nextBatch?.isNotEmpty() == true) {
            nextBatch = fetchAndProcessRelatedEventsFrom(params, from = nextBatch)
        }
    }

    private suspend fun fetchAndProcessRelatedEventsFrom(params: FetchPollResponseEventsTask.Params, from: String? = null): String? {
        val response = getRelatedEvents(params, from)

        val filterTaskParams = FilterAndStoreEventsTask.Params(
                roomId = params.roomId,
                events = response.chunks,
                filterPredicate = { it.isPollResponse() }
        )
        filterAndStoreEventsTask.execute(filterTaskParams)

        return response.nextBatch
    }

    private suspend fun getRelatedEvents(params: FetchPollResponseEventsTask.Params, from: String? = null): RelationsResponse {
        return executeRequest(globalErrorReceiver, canRetry = true) {
            roomAPI.getRelations(
                    roomId = params.roomId,
                    eventId = params.startPollEventId,
                    relationType = RelationType.REFERENCE,
                    from = from,
                    limit = FETCH_RELATED_EVENTS_LIMIT,
            )
        }
    }
}
