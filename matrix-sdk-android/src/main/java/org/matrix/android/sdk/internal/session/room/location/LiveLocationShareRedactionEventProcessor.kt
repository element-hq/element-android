/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.location

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import timber.log.Timber
import javax.inject.Inject

/**
 * Listens to the database for the insertion of any redaction event.
 * Delete specifically the aggregated summary related to a redacted live location share event.
 */
internal class LiveLocationShareRedactionEventProcessor @Inject constructor() : EventInsertLiveProcessor {

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        return eventType == EventType.REDACTION && insertType != EventInsertType.LOCAL_ECHO
    }

    override suspend fun process(realm: Realm, event: Event) {
        if (event.redacts.isNullOrBlank() || LocalEcho.isLocalEchoId(event.eventId.orEmpty())) {
            return
        }

        val redactedEvent = EventEntity.where(realm, eventId = event.redacts).findFirst()
                ?: return

        if (redactedEvent.type in EventType.STATE_ROOM_BEACON_INFO) {
            val liveSummary = LiveLocationShareAggregatedSummaryEntity.get(realm, eventId = redactedEvent.eventId)

            if (liveSummary != null) {
                Timber.d("deleting live summary with id: ${liveSummary.eventId}")
                liveSummary.deleteFromRealm()
                val annotationsSummary = EventAnnotationsSummaryEntity.get(realm, eventId = redactedEvent.eventId)
                if (annotationsSummary != null) {
                    Timber.d("deleting annotation summary with id: ${annotationsSummary.eventId}")
                    annotationsSummary.deleteFromRealm()
                }
            }
        }
    }
}
