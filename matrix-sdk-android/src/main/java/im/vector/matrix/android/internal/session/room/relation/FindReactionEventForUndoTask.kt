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
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.task.Task
import io.realm.Realm
import javax.inject.Inject

internal interface FindReactionEventForUndoTask : Task<FindReactionEventForUndoTask.Params, FindReactionEventForUndoTask.Result> {

    data class Params(
            val roomId: String,
            val eventId: String,
            val reaction: String
    )

    data class Result(
            val redactEventId: String?
    )
}

internal class DefaultFindReactionEventForUndoTask @Inject constructor(
        private val monarchy: Monarchy,
        @UserId private val userId: String) : FindReactionEventForUndoTask {

    override suspend fun execute(params: FindReactionEventForUndoTask.Params): FindReactionEventForUndoTask.Result {
        val eventId = Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            getReactionToRedact(realm, params.reaction, params.eventId)?.eventId
        }
        return FindReactionEventForUndoTask.Result(eventId)
    }

    private fun getReactionToRedact(realm: Realm, reaction: String, eventId: String): EventEntity? {
        val summary = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst() ?: return null

        val rase = summary.reactionsSummary.where()
                .equalTo(ReactionAggregatedSummaryEntityFields.KEY, reaction)
                .findFirst() ?: return null

        // want to find the event originated by me!
        return rase.sourceEvents
                .asSequence()
                .mapNotNull {
                    // find source event
                    EventEntity.where(realm, it).findFirst()
                }
                .firstOrNull { eventEntity ->
                    // is it mine?
                    eventEntity.sender == userId
                }
    }
}
