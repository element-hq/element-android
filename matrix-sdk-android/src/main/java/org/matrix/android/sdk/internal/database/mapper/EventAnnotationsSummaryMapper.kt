/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.room.model.EditAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.ReactionAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.ReferencesAggregatedSummary
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity

internal object EventAnnotationsSummaryMapper {
    fun map(annotationsSummary: EventAnnotationsSummaryEntity): EventAnnotationsSummary {
        return EventAnnotationsSummary(
                eventId = annotationsSummary.eventId,
                reactionsSummary = annotationsSummary.reactionsSummary.toList().map {
                    ReactionAggregatedSummary(
                            it.key,
                            it.count,
                            it.addedByMe,
                            it.firstTimestamp,
                            it.sourceEvents.toList(),
                            it.sourceLocalEcho.toList()
                    )
                },
                editSummary = annotationsSummary.editSummary
                        ?.let {
                            val latestEdition = it.editions.maxByOrNull { editionOfEvent -> editionOfEvent.timestamp } ?: return@let null
                            EditAggregatedSummary(
                                    latestContent = ContentMapper.map(latestEdition.content),
                                    sourceEvents = it.editions.filter { editionOfEvent -> !editionOfEvent.isLocalEcho }
                                            .map { editionOfEvent -> editionOfEvent.eventId },
                                    localEchos = it.editions.filter { editionOfEvent -> editionOfEvent.isLocalEcho }
                                            .map { editionOfEvent -> editionOfEvent.eventId },
                                    lastEditTs = latestEdition.timestamp
                            )
                        },
                referencesAggregatedSummary = annotationsSummary.referencesSummaryEntity?.let {
                    ReferencesAggregatedSummary(
                            it.eventId,
                            ContentMapper.map(it.content),
                            it.sourceEvents.toList(),
                            it.sourceLocalEcho.toList()
                    )
                },
                pollResponseSummary = annotationsSummary.pollResponseSummary?.let {
                    PollResponseAggregatedSummaryEntityMapper.map(it)
                }

        )
    }
}

internal fun EventAnnotationsSummaryEntity.asDomain(): EventAnnotationsSummary {
    return EventAnnotationsSummaryMapper.map(this)
}
