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
package im.vector.matrix.android.internal.session.room.relation

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.ReactionAggregatedSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.Task
import io.realm.Realm
import javax.inject.Inject


internal interface UpdateQuickReactionTask : Task<UpdateQuickReactionTask.Params, UpdateQuickReactionTask.Result> {

    data class Params(
            val roomId: String,
            val eventId: String,
            val reaction: String,
            val oppositeReaction: String,
            val myUserId: String
    )

    data class Result(
            val reactionToAdd: String?,
            val reactionToRedact: List<String>
    )
}

internal class DefaultUpdateQuickReactionTask @Inject constructor(private val monarchy: Monarchy) : UpdateQuickReactionTask {

    override suspend fun execute(params: UpdateQuickReactionTask.Params): UpdateQuickReactionTask.Result {
        var res: Pair<String?, List<String>?>? = null
        monarchy.doWithRealm { realm ->
            res = updateQuickReaction(realm, params.reaction, params.oppositeReaction, params.eventId, params.myUserId)
        }
        return UpdateQuickReactionTask.Result(res?.first, res?.second ?: emptyList())
    }


    private fun updateQuickReaction(realm: Realm, reaction: String, oppositeReaction: String, eventId: String, myUserId: String): Pair<String?, List<String>?> {
        //the emoji reaction has been selected, we need to check if we have reacted it or not
        val existingSummary = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
                ?: return Pair(reaction, null)

        //Ok there is already reactions on this event, have we reacted to it
        val aggregationForReaction = existingSummary.reactionsSummary.where()
                .equalTo(ReactionAggregatedSummaryEntityFields.KEY, reaction)
                .findFirst()
        val aggregationForOppositeReaction = existingSummary.reactionsSummary.where()
                .equalTo(ReactionAggregatedSummaryEntityFields.KEY, oppositeReaction)
                .findFirst()

        if (aggregationForReaction == null || !aggregationForReaction.addedByMe) {
            //i haven't yet reacted to it, so need to add it, but do I need to redact the opposite?
            val toRedact = aggregationForOppositeReaction?.sourceEvents?.mapNotNull {
                //find source event
                val entity = EventEntity.where(realm, it).findFirst()
                if (entity?.sender == myUserId) entity.eventId else null
            }
            return Pair(reaction, toRedact)
        } else {
            //I already added it, so i need to undo it (like a toggle)
            // find all m.redaction coming from me to readact them
            val toRedact = aggregationForReaction.sourceEvents.mapNotNull {
                //find source event
                val entity = EventEntity.where(realm, it).findFirst()
                if (entity?.sender == myUserId) entity.eventId else null
            }
            return Pair(null, toRedact)
        }

    }
}