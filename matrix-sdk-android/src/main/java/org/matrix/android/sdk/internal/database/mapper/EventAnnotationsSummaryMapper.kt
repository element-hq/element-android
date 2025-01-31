/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
                            ContentMapper.map(it.content),
                            it.sourceEvents.toList(),
                            it.sourceLocalEcho.toList()
                    )
                },
                pollResponseSummary = annotationsSummary.pollResponseSummary?.let {
                    PollResponseAggregatedSummaryEntityMapper.map(it)
                },
                liveLocationShareAggregatedSummary = annotationsSummary.liveLocationShareAggregatedSummary?.let {
                    LiveLocationShareAggregatedSummaryMapper().map(it)
                }
        )
    }
}

internal fun EventAnnotationsSummaryEntity.asDomain(): EventAnnotationsSummary {
    return EventAnnotationsSummaryMapper.map(this)
}
