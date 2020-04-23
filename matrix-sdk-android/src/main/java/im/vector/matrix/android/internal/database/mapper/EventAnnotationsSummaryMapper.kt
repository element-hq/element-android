/*
 * Copyright 2019 New Vector Ltd
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

import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.*

internal object EventAnnotationsSummaryMapper {

    fun mapAnnotationsSummary(eventId: String,
                              reactions: List<ReactionAggregatedSummary>,
                              edit: EditAggregatedSummary?,
                              references: ReferencesAggregatedSummary?,
                              poll: PollResponseAggregatedSummary?): EventAnnotationsSummary {
        return EventAnnotationsSummary(
                eventId = eventId,
                reactionsSummary = reactions,
                referencesAggregatedSummary = references,
                editSummary = edit,
                pollResponseSummary = poll
        )
    }

    fun mapReactionSummary(key: String,
                           count: Long,
                           added_by_me: Boolean,
                           first_timestamp: Long,
                           source_event_ids: List<String>,
                           source_local_echo_ids: List<String>): ReactionAggregatedSummary {
        return ReactionAggregatedSummary(
                key = key,
                count = count.toInt(),
                addedByMe = added_by_me,
                firstTimestamp = first_timestamp,
                sourceEvents = source_event_ids,
                localEchoEvents = source_local_echo_ids
        )
    }


    fun mapEditSummary(aggregated_content: String?,
                       last_edit_ts: Long,
                       source_event_ids: List<String>,
                       source_local_echo_ids: List<String>): EditAggregatedSummary {
        return EditAggregatedSummary(
                ContentMapper.map(aggregated_content),
                source_event_ids,
                source_local_echo_ids,
                last_edit_ts
        )
    }

    fun mapReferencesSummary(event_id: String,
                             content: String?,
                             source_event_ids: List<String>,
                             source_local_echo_ids: List<String>): ReferencesAggregatedSummary {
        return ReferencesAggregatedSummary(
                event_id,
                ContentMapper.map(content),
                source_event_ids,
                source_local_echo_ids
        )
    }

    fun mapPollSummary(content: String?,
                       closed_time: Long?,
                       nb_options: Int,
                       source_event_ids: List<String>,
                       source_local_echo_ids: List<String>): PollResponseAggregatedSummary {
        return PollResponseAggregatedSummary(
                aggregatedContent = ContentMapper.map(content).toModel(),
                closedTime = closed_time,
                localEchos = source_local_echo_ids,
                sourceEvents = source_event_ids,
                nbOptions = nb_options
        )
    }
}
