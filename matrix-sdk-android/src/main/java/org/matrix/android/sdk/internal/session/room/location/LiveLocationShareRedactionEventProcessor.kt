/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    override fun process(realm: Realm, event: Event) {
        if (event.redacts.isNullOrBlank() || LocalEcho.isLocalEchoId(event.eventId.orEmpty())) {
            return
        }

        val redactedEvent = EventEntity.where(realm, eventId = event.redacts).findFirst()
                ?: return

        if (redactedEvent.type in EventType.STATE_ROOM_BEACON_INFO.values) {
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
