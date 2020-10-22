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
package org.matrix.android.sdk.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class EventAnnotationsSummaryEntity(
        @PrimaryKey
        var eventId: String = "",
        var roomId: String? = null,
        var reactionsSummary: RealmList<ReactionAggregatedSummaryEntity> = RealmList(),
        var editSummary: EditAggregatedSummaryEntity? = null,
        var referencesSummaryEntity: ReferencesAggregatedSummaryEntity? = null,
        var pollResponseSummary: PollResponseAggregatedSummaryEntity? = null
) : RealmObject() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventAnnotationsSummaryEntity) return false
        if (eventId != other.eventId) return false
        if (reactionsSummary != other.reactionsSummary) return false
        if (editSummary != other.editSummary) return false
        if (referencesSummaryEntity != other.referencesSummaryEntity) return false
        if (pollResponseSummary != other.pollResponseSummary) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + reactionsSummary.hashCode()
        result = 31 * result + (editSummary?.hashCode() ?: 0)
        result = 31 * result + (referencesSummaryEntity?.hashCode() ?: 0)
        result = 31 * result + (pollResponseSummary?.hashCode() ?: 0)
        return result
    }

    companion object
}
