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

import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.sqldelight.session.SessionDatabase
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
        private val sessionDatabase: SessionDatabase,
        @UserId private val userId: String) : FindReactionEventForUndoTask {

    override suspend fun execute(params: FindReactionEventForUndoTask.Params): FindReactionEventForUndoTask.Result {
        val eventId = getReactionToRedact(params.eventId, params.reaction)
        return FindReactionEventForUndoTask.Result(eventId)
    }

    private fun getReactionToRedact(eventId: String, reaction: String): String? {
        val reactionsSummary = sessionDatabase.eventAnnotationsQueries.selectReaction(eventId, reaction).executeAsOneOrNull()
                ?: return null

        val sourceEvents = reactionsSummary.source_event_ids
        return sessionDatabase.eventQueries.findWithSender(userId, sourceEvents).executeAsList().firstOrNull()
    }
}
