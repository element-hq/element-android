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
