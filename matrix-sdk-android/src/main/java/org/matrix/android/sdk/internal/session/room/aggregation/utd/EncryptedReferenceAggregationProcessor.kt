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

package org.matrix.android.sdk.internal.session.room.aggregation.utd

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntityFields
import javax.inject.Inject

internal class EncryptedReferenceAggregationProcessor @Inject constructor() {

    fun handle(
            realm: Realm,
            event: Event,
            isLocalEcho: Boolean,
            relatedEventId: String?
    ): Boolean {
        return if (isLocalEcho || relatedEventId.isNullOrEmpty()) {
            false
        } else {
            handlePollReference(realm = realm, event = event, relatedEventId = relatedEventId)
            true
        }
    }

    private fun handlePollReference(
            realm: Realm,
            event: Event,
            relatedEventId: String
    ) {
        event.eventId?.let { eventId ->
            val existingRelatedPoll = getPollSummaryWithEventId(realm, relatedEventId)
            if (eventId !in existingRelatedPoll?.encryptedRelatedEventIds.orEmpty()) {
                existingRelatedPoll?.encryptedRelatedEventIds?.add(eventId)
            }
        }
    }

    private fun getPollSummaryWithEventId(realm: Realm, eventId: String): PollResponseAggregatedSummaryEntity? {
        return realm.where(PollResponseAggregatedSummaryEntity::class.java)
                .containsValue(PollResponseAggregatedSummaryEntityFields.SOURCE_EVENTS.`$`, eventId)
                .findFirst()
    }
}
