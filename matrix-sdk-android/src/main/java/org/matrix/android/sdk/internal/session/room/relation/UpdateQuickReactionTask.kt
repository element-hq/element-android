/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.room.relation

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.ReactionAggregatedSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdateQuickReactionTask : Task<UpdateQuickReactionTask.Params, UpdateQuickReactionTask.Result> {

    data class Params(
            val roomId: String,
            val eventId: String,
            val reaction: String,
            val oppositeReaction: String
    )

    data class Result(
            val reactionToAdd: String?,
            val reactionToRedact: List<String>
    )
}

internal class DefaultUpdateQuickReactionTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        @UserId private val userId: String
) : UpdateQuickReactionTask {

    override suspend fun execute(params: UpdateQuickReactionTask.Params): UpdateQuickReactionTask.Result {
        var res: Pair<String?, List<String>?>? = null
        monarchy.doWithRealm { realm ->
            res = updateQuickReaction(realm, params)
        }
        return UpdateQuickReactionTask.Result(res?.first, res?.second.orEmpty())
    }

    private fun updateQuickReaction(realm: Realm, params: UpdateQuickReactionTask.Params): Pair<String?, List<String>?> {
        // the emoji reaction has been selected, we need to check if we have reacted it or not
        val existingSummary = EventAnnotationsSummaryEntity.where(realm, params.roomId, params.eventId).findFirst()
                ?: return Pair(params.reaction, null)

        // Ok there is already reactions on this event, have we reacted to it
        val aggregationForReaction = existingSummary.reactionsSummary.where()
                .equalTo(ReactionAggregatedSummaryEntityFields.KEY, params.reaction)
                .findFirst()
        val aggregationForOppositeReaction = existingSummary.reactionsSummary.where()
                .equalTo(ReactionAggregatedSummaryEntityFields.KEY, params.oppositeReaction)
                .findFirst()

        if (aggregationForReaction == null || !aggregationForReaction.addedByMe) {
            // i haven't yet reacted to it, so need to add it, but do I need to redact the opposite?
            val toRedact = aggregationForOppositeReaction?.sourceEvents?.mapNotNull {
                // find source event
                val entity = EventEntity.where(realm, it).findFirst()
                if (entity?.sender == userId) entity.eventId else null
            }
            return Pair(params.reaction, toRedact)
        } else {
            // I already added it, so i need to undo it (like a toggle)
            // find all m.redaction coming from me to readact them
            val toRedact = aggregationForReaction.sourceEvents.mapNotNull {
                // find source event
                val entity = EventEntity.where(realm, it).findFirst()
                if (entity?.sender == userId) entity.eventId else null
            }
            return Pair(null, toRedact)
        }
    }
}
