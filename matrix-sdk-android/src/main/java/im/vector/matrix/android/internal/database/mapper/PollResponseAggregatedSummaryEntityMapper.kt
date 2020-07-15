/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.PollResponseAggregatedSummary
import im.vector.matrix.android.internal.database.model.PollResAggregatedSummaryEntity
import io.realm.RealmList

internal object PollResponseAggregatedSummaryEntityMapper {

    fun map(entity: PollResAggregatedSummaryEntity): PollResponseAggregatedSummary {
        return PollResponseAggregatedSummary(
                aggregatedContent = ContentMapper.map(entity.aggregatedContent).toModel(),
                closedTime = entity.closedTime,
                localEchos = entity.sourceLocalEchoEvents.toList(),
                sourceEvents = entity.sourceEvents.toList(),
                nbOptions = entity.nbOptions
        )
    }

    fun map(model: PollResponseAggregatedSummary): PollResAggregatedSummaryEntity {
        return PollResAggregatedSummaryEntity(
                aggregatedContent = ContentMapper.map(model.aggregatedContent.toContent()),
                nbOptions = model.nbOptions,
                closedTime = model.closedTime,
                sourceEvents = RealmList<String>().apply { addAll(model.sourceEvents) },
                sourceLocalEchoEvents = RealmList<String>().apply { addAll(model.localEchos) }
        )
    }
}

internal fun PollResAggregatedSummaryEntity.asDomain(): PollResponseAggregatedSummary {
    return PollResponseAggregatedSummaryEntityMapper.map(this)
}
