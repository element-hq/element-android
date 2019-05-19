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

package im.vector.matrix.android.internal.session.room.prune

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.annotation.ReactionContent
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.ReactionAggregatedSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.Realm
import org.koin.standalone.inject
import timber.log.Timber

//TODO should be a task instead of worker
internal class PruneEventWorker(context: Context,
                                workerParameters: WorkerParameters
) : Worker(context, workerParameters), MatrixKoinComponent {

    @JsonClass(generateAdapter = true)
    internal class Params(
            val redactionEvents: List<Event>,
            val userId: String
    )

    private val monarchy by inject<Monarchy>()

    override fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()

        val result = monarchy.tryTransactionSync { realm ->
            params.redactionEvents.forEach { event ->
                pruneEvent(realm, event, params.userId)
            }
        }
        return result.fold({
            Result.retry()
        }, {
            Result.success()
        })
    }

    private fun pruneEvent(realm: Realm, redactionEvent: Event, userId: String) {
        if (redactionEvent.redacts.isNullOrBlank()) {
            return
        }

        val eventToPrune = EventEntity.where(realm, eventId = redactionEvent.redacts).findFirst()
                ?: return

        val allowedKeys = computeAllowedKeys(eventToPrune.type)
        if (allowedKeys.isNotEmpty()) {
            val prunedContent = ContentMapper.map(eventToPrune.content)?.filterKeys { key -> allowedKeys.contains(key) }
            eventToPrune.content = ContentMapper.map(prunedContent)
        } else {
            when (eventToPrune.type) {
                EventType.MESSAGE -> {
                    Timber.d("REDACTION for message ${eventToPrune.eventId}")
                    val unsignedData = EventMapper.map(eventToPrune).unsignedData
                            ?: UnsignedData(null, null)
                    val modified = unsignedData.copy(redactedEvent = redactionEvent)
                    eventToPrune.content = ContentMapper.map(emptyMap())
                    eventToPrune.unsignedData = MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).toJson(modified)

                }
                EventType.REACTION -> {
                    Timber.d("REDACTION of reaction ${eventToPrune.eventId}")
                    //delete a reaction, need to update the annotation summary if any
                    val reactionContent: ReactionContent = EventMapper.map(eventToPrune).content.toModel()
                            ?: return
                    val eventThatWasReacted = reactionContent.relatesTo?.eventId ?: return

                    val reactionkey = reactionContent.relatesTo.key
                    Timber.d("REMOVE reaction for key $reactionkey")
                    val summary = EventAnnotationsSummaryEntity.where(realm, eventThatWasReacted).findFirst()
                    if (summary != null) {
                        summary.reactionsSummary.where()
                                .equalTo(ReactionAggregatedSummaryEntityFields.KEY, reactionkey)
                                .findFirst()?.let { summary ->
                                    Timber.d("Find summary for key with  ${summary.sourceEvents.size} known reactions (count:${summary.count})")
                                    Timber.d("Known reactions  ${summary.sourceEvents.joinToString(",")}")
                                    if (summary.sourceEvents.contains(eventToPrune.eventId)) {
                                        Timber.d("REMOVE reaction for key $reactionkey")
                                        summary.sourceEvents.remove(eventToPrune.eventId)
                                        Timber.d("Known reactions after  ${summary.sourceEvents.joinToString(",")}")
                                        summary.count = summary.count - 1
                                        if (eventToPrune.sender == userId) {
                                            //Was it a redact on my reaction?
                                            summary.addedByMe = false
                                        }
                                        if (summary.count == 0) {
                                            //delete!
                                            summary.deleteFromRealm()
                                        }
                                    } else {
                                        Timber.e("## Cannot remove summary from count, corresponding reaction ${eventToPrune.eventId} is not known")
                                    }
                                }
                    } else {
                        Timber.e("## Cannot find summary for key $reactionkey")
                    }
                }
            }
        }
    }

    private fun computeAllowedKeys(type: String): List<String> {
        // Add filtered content, allowed keys in content depends on the event type
        return when (type) {
            EventType.STATE_ROOM_MEMBER -> listOf("membership")
            EventType.STATE_ROOM_CREATE -> listOf("creator")
            EventType.STATE_ROOM_JOIN_RULES -> listOf("join_rule")
            EventType.STATE_ROOM_POWER_LEVELS -> listOf("users",
                    "users_default",
                    "events",
                    "events_default",
                    "state_default",
                    "ban",
                    "kick",
                    "redact",
                    "invite")
            EventType.STATE_ROOM_ALIASES -> listOf("aliases")
            EventType.STATE_CANONICAL_ALIAS -> listOf("alias")
            EventType.FEEDBACK -> listOf("type", "target_event_id")
            else -> emptyList()
        }
    }

}