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

package org.matrix.android.sdk.internal.session.room.event

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.EventDecryptor
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal interface FilterAndStoreEventsTask : Task<FilterAndStoreEventsTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val events: List<Event>,
            val filterPredicate: (Event) -> Boolean,
    )
}

internal class DefaultFilterAndStoreEventsTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val clock: Clock,
        private val eventDecryptor: EventDecryptor,
) : FilterAndStoreEventsTask {

    override suspend fun execute(params: FilterAndStoreEventsTask.Params) {
        val filteredEvents = params.events
                .map { decryptEventIfNeeded(it) }
                // we also filter in the encrypted events since it means there was decryption error for them
                // and they may be decrypted later
                .filter { params.filterPredicate(it) || it.getClearType() == EventType.ENCRYPTED }

        addMissingEventsInDB(params.roomId, filteredEvents)
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
