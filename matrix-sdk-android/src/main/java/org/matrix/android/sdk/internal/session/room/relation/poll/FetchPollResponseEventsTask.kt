/*
 * Copyright (c) 2022 New Vector Ltd
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

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
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

private const val FETCH_RELATED_EVENTS_LIMIT = 50

/**
 * Task to fetch all the vote events to ensure full aggregation for a given poll.
 */
internal interface FetchPollResponseEventsTask : Task<FetchPollResponseEventsTask.Params, Result<Unit>> {
    data class Params(
            val roomId: String,
            val startPollEventId: String,
    )
}

// TODO add unit tests
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
            val existingIds = EventEntity.where(realm, eventIdsToCheck).findAll().toList().map { it.eventId }

            events.filterNot { it.eventId in existingIds }
                    .map {
                        val ageLocalTs = clock.epochMillis() - (it.unsignedData?.age ?: 0)
                        it.toEntity(roomId = roomId, sendState = SendState.SYNCED, ageLocalTs = ageLocalTs)
                    }
                    .forEach { it.copyToRealmOrIgnore(realm, EventInsertType.PAGINATION) }
        }
    }

    private suspend fun decryptEventIfNeeded(event: Event): Event {
        // TODO move into a reusable task
        if (event.isEncrypted()) {
            tryOrNull(message = "Unable to decrypt the event") {
                eventDecryptor.decryptEvent(event, "")
            }
                    ?.let { result ->
                        event.mxDecryptionResult = OlmDecryptionResult(
                                payload = result.clearEvent,
                                senderKey = result.senderCurve25519Key,
                                keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                                forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                                isSafe = result.isSafe
                        )
                    }
        }

        event.ageLocalTs = clock.epochMillis() - (event.unsignedData?.age ?: 0)

        return event
    }
}
