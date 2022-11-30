/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.room.model.EditAggregatedSummary
import org.matrix.android.sdk.internal.database.model.EditAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.EditionOfEvent

internal object EditAggregatedSummaryEntityMapper {

    fun map(summary: EditAggregatedSummaryEntity?): EditAggregatedSummary? {
        summary ?: return null
        /**
         * The most recent event is determined by comparing origin_server_ts;
         * if two or more replacement events have identical origin_server_ts,
         * the event with the lexicographically largest event_id is treated as more recent.
         */
        val latestEdition = summary.editions.sortedWith(compareBy<EditionOfEvent> { it.timestamp }.thenBy { it.eventId })
                .lastOrNull() ?: return null
        val editEvent = latestEdition.event

        return EditAggregatedSummary(
                latestEdit = editEvent?.asDomain(),
                sourceEvents = summary.editions.filter { editionOfEvent -> !editionOfEvent.isLocalEcho }
                        .map { editionOfEvent -> editionOfEvent.eventId },
                localEchos = summary.editions.filter { editionOfEvent -> editionOfEvent.isLocalEcho }
                        .map { editionOfEvent -> editionOfEvent.eventId },
                lastEditTs = latestEdition.timestamp
        )
    }
}
