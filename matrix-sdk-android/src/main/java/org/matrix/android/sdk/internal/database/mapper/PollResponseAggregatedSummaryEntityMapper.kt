/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import io.realm.RealmList
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PollResponseAggregatedSummary
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntity

internal object PollResponseAggregatedSummaryEntityMapper {

    fun map(entity: PollResponseAggregatedSummaryEntity): PollResponseAggregatedSummary {
        return PollResponseAggregatedSummary(
                aggregatedContent = ContentMapper.map(entity.aggregatedContent).toModel(),
                closedTime = entity.closedTime,
                localEchos = entity.sourceLocalEchoEvents.toList(),
                sourceEvents = entity.sourceEvents.toList(),
                nbOptions = entity.nbOptions
        )
    }

    fun map(model: PollResponseAggregatedSummary): PollResponseAggregatedSummaryEntity {
        return PollResponseAggregatedSummaryEntity(
                aggregatedContent = ContentMapper.map(model.aggregatedContent.toContent()),
                nbOptions = model.nbOptions,
                closedTime = model.closedTime,
                sourceEvents = RealmList<String>().apply { addAll(model.sourceEvents) },
                sourceLocalEchoEvents = RealmList<String>().apply { addAll(model.localEchos) }
        )
    }
}

internal fun PollResponseAggregatedSummaryEntity.asDomain(): PollResponseAggregatedSummary {
    return PollResponseAggregatedSummaryEntityMapper.map(this)
}
