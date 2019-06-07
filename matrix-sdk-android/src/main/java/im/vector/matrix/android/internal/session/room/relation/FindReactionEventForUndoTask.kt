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

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.ReactionAggregatedSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.Task
import io.realm.Realm


internal interface FindReactionEventForUndoTask : Task<FindReactionEventForUndoTask.Params, FindReactionEventForUndoTask.Result> {

    data class Params(
            val roomId: String,
            val eventId: String,
            val reaction: String,
            val myUserId: String
    )

    data class Result(
            val redactEventId: String?
    )

}

internal class DefaultFindReactionEventForUndoTask(private val monarchy: Monarchy) : FindReactionEventForUndoTask {

    override suspend fun execute(params: FindReactionEventForUndoTask.Params): Try<FindReactionEventForUndoTask.Result> {
        return Try {
            var eventId: String? = null
            monarchy.doWithRealm { realm ->
                eventId = getReactionToRedact(realm, params.reaction, params.eventId, params.myUserId)?.eventId
            }
            FindReactionEventForUndoTask.Result(eventId)
        }
    }

    private fun getReactionToRedact(realm: Realm, reaction: String, eventId: String, userId: String): EventEntity? {
        val summary = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
        if (summary != null) {
            summary.reactionsSummary.where()
                    .equalTo(ReactionAggregatedSummaryEntityFields.KEY, reaction)
                    .findFirst()?.let {
                        //want to find the event orignated by me!
                        it.sourceEvents.forEach {
                            //find source event
                            EventEntity.where(realm, it).findFirst()?.let { eventEntity ->
                                //is it mine?
                                if (eventEntity.sender == userId) {
                                    return eventEntity
                                }
                            }
                        }
                    }
        }
        return null
    }
}