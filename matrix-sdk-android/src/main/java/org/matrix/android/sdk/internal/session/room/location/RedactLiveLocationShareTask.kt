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

package org.matrix.android.sdk.internal.session.room.location

import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.database.awaitTransaction
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface RedactLiveLocationShareTask : Task<RedactLiveLocationShareTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val beaconInfoEventId: String,
            val reason: String?
    )
}

internal class DefaultRedactLiveLocationShareTask @Inject constructor(
        @SessionDatabase private val realmConfiguration: RealmConfiguration,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val eventSenderProcessor: EventSenderProcessor,
) : RedactLiveLocationShareTask {

    override suspend fun execute(params: RedactLiveLocationShareTask.Params) {
        val relatedEventIds = getRelatedEventIdsOfLive(params.beaconInfoEventId)
        Timber.d("beacon with id ${params.beaconInfoEventId} has related event ids: ${relatedEventIds.joinToString(", ")}")

        postRedactionWithLocalEcho(
                eventId = params.beaconInfoEventId,
                roomId = params.roomId,
                reason = params.reason
        )
        relatedEventIds.forEach { eventId ->
            postRedactionWithLocalEcho(
                    eventId = eventId,
                    roomId = params.roomId,
                    reason = params.reason
            )
        }
    }

    private suspend fun getRelatedEventIdsOfLive(beaconInfoEventId: String): List<String> {
        return awaitTransaction(realmConfiguration) { realm ->
            val aggregatedSummaryEntity = LiveLocationShareAggregatedSummaryEntity.get(
                    realm = realm,
                    eventId = beaconInfoEventId
            )
            aggregatedSummaryEntity?.relatedEventIds?.toList() ?: emptyList()
        }
    }

    private fun postRedactionWithLocalEcho(eventId: String, roomId: String, reason: String?) {
        Timber.d("posting redaction for event of id $eventId")
        val redactionEcho = localEchoEventFactory.createRedactEvent(roomId, eventId, reason)
        localEchoEventFactory.createLocalEcho(redactionEcho)
        eventSenderProcessor.postRedaction(redactionEcho, reason)
    }
}
