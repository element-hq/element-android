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
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.isPollResponse
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.EventDecryptor
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.relation.RelationsResponse
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
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
        @SessionDatabase private val monarchy: Monarchy,
        private val clock: Clock,
        private val eventDecryptor: EventDecryptor,
) : FetchPollResponseEventsTask {

    override suspend fun execute(params: FetchPollResponseEventsTask.Params): Result<Unit> = runCatching {
        var nextBatch: String? = fetchAndProcessRelatedEventsFrom(params)

        while (nextBatch?.isNotEmpty() == true) {
            nextBatch = fetchAndProcessRelatedEventsFrom(params, from = nextBatch)
        }
    }

    private suspend fun fetchAndProcessRelatedEventsFrom(params: FetchPollResponseEventsTask.Params, from: String? = null): String? {
        val response = getRelatedEvents(params, from)

        val filteredEvents = response.chunks
                .map { decryptEventIfNeeded(it) }
                .filter { it.isPollResponse() }

        addMissingEventsInDB(params.roomId, filteredEvents)

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

    private suspend fun addMissingEventsInDB(roomId: String, events: List<Event>) {
        monarchy.awaitTransaction { realm ->
            val eventIdsToCheck = events.mapNotNull { it.eventId }.filter { it.isNotEmpty() }
            if (eventIdsToCheck.isNotEmpty()) {
                val existingIds = EventEntity.where(realm, eventIdsToCheck).findAll().toList().map { it.eventId }

                events.filterNot { it.eventId in existingIds }
                        .map { it.toEntity(roomId = roomId, sendState = SendState.SYNCED, ageLocalTs = computeLocalTs(it)) }
                        .forEach { it.copyToRealmOrIgnore(realm, EventInsertType.PAGINATION) }
            }
        }
    }

    private suspend fun decryptEventIfNeeded(event: Event): Event {
        if (event.isEncrypted()) {
            eventDecryptor.decryptEventAndSaveResult(event, timeline = "")
        }

        event.ageLocalTs = computeLocalTs(event)

        return event
    }

    private fun computeLocalTs(event: Event) = clock.epochMillis() - (event.unsignedData?.age ?: 0)
}
