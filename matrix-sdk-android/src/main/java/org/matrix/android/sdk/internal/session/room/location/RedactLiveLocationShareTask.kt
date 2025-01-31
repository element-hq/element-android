/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
