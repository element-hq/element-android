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
package im.vector.matrix.android.internal.session.room

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.*
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.relation.ReactionContent
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.tryTransactionAsync
import io.realm.Realm
import timber.log.Timber

internal interface EventRelationsAggregationTask : Task<EventRelationsAggregationTask.Params, Unit> {

    data class Params(
            val events: List<Pair<Event, SendState>>,
            val userId: String
    )
}

/**
 * Called by EventRelationAggregationUpdater, when new events that can affect relations are inserted in base.
 */
internal class DefaultEventRelationsAggregationTask(private val monarchy: Monarchy) : EventRelationsAggregationTask {

    override fun execute(params: EventRelationsAggregationTask.Params): Try<Unit> {
        return monarchy.tryTransactionAsync { realm ->
            update(realm, params.events, params.userId)
        }
    }

    fun update(realm: Realm, events: List<Pair<Event, SendState>>?, userId: String) {
        events?.forEach { pair ->
            val roomId = pair.first.roomId ?: return@forEach
            val event = pair.first
            val sendState = pair.second
            val isLocalEcho = sendState == SendState.UNSENT
            when (event.type) {
                EventType.REACTION -> {
                    //we got a reaction!!
                    Timber.v("###REACTION in room $roomId")
                    handleReaction(event, roomId, realm, userId, isLocalEcho)
                }
                EventType.MESSAGE -> {
                    if (event.unsignedData?.relations?.annotations != null) {
                        Timber.v("###REACTION Agreggation in room $roomId for event ${event.eventId}")
                        handleInitialAggregatedRelations(event, roomId, event.unsignedData.relations.annotations, realm)
                    } else {
                        val content: MessageContent? = event.content.toModel()
                        if (content?.relatesTo?.type == RelationType.REPLACE) {
                            Timber.v("###REPLACE in room $roomId for event ${event.eventId}")
                            //A replace!
                            handleReplace(realm, event, content, roomId, isLocalEcho)
                        }
                    }

                }
                EventType.REDACTION -> {
                    val eventToPrune = event.redacts?.let { EventEntity.where(realm, eventId = it).findFirst() }
                            ?: return
                    when (eventToPrune.type) {
                        EventType.MESSAGE -> {
                            Timber.d("REDACTION for message ${eventToPrune.eventId}")
                            val unsignedData = EventMapper.map(eventToPrune).unsignedData
                                    ?: UnsignedData(null, null)

                            //was this event a m.replace
                            val contentModel = ContentMapper.map(eventToPrune.content)?.toModel<MessageContent>()
                            if (RelationType.REPLACE == contentModel?.relatesTo?.type && contentModel.relatesTo?.eventId != null) {
                                handleRedactionOfReplace(eventToPrune, contentModel.relatesTo!!.eventId!!, realm)
                            }

                        }
                        EventType.REACTION -> {
                            handleReactionRedact(eventToPrune, realm, userId)
                        }
                    }
                }
            }
        }
    }

    private fun handleReplace(realm: Realm, event: Event, content: MessageContent, roomId: String, isLocalEcho: Boolean) {
        val eventId = event.eventId ?: return
        val targetEventId = content.relatesTo?.eventId ?: return
        val newContent = content.newContent ?: return
        //ok, this is a replace
        var existing = EventAnnotationsSummaryEntity.where(realm, targetEventId).findFirst()
        if (existing == null) {
            Timber.v("###REPLACE creating no relation summary for ${targetEventId}")
            existing = EventAnnotationsSummaryEntity.create(realm, targetEventId)
            existing.roomId = roomId
        }

        //we have it
        val existingSummary = existing.editSummary
        if (existingSummary == null) {
            Timber.v("###REPLACE no edit summary for ${targetEventId}, creating one (localEcho:$isLocalEcho)")
            //create the edit summary
            val editSummary = realm.createObject(EditAggregatedSummaryEntity::class.java)
            editSummary.lastEditTs = event.originServerTs ?: System.currentTimeMillis()
            editSummary.aggregatedContent = ContentMapper.map(newContent)
            if (isLocalEcho) {
                editSummary.sourceLocalEchoEvents.add(eventId)
            } else {
                editSummary.sourceEvents.add(eventId)
            }

            existing.editSummary = editSummary
        } else {
            if (existingSummary.sourceEvents.contains(eventId)) {
                //ignore this event, we already know it (??)
                Timber.v("###REPLACE ignoring event for summary, it's known ${eventId}")
                return
            }
            val txId = event.unsignedData?.transactionId
            //is it a remote echo?
            if (!isLocalEcho && existingSummary.sourceLocalEchoEvents.contains(txId)) {
                //ok it has already been managed
                Timber.v("###REPLACE Receiving remote echo of edit (edit already done)")
                existingSummary.sourceLocalEchoEvents.remove(txId)
                existingSummary.sourceEvents.add(event.eventId)
            } else if (event.originServerTs ?: 0 > existingSummary.lastEditTs) {
                Timber.v("###REPLACE Computing aggregated edit summary (isLocalEcho:$isLocalEcho)")
                existingSummary.lastEditTs = event.originServerTs ?: System.currentTimeMillis()
                existingSummary.aggregatedContent = ContentMapper.map(newContent)
                existingSummary.sourceEvents.add(eventId)
            } else {
                //ignore this event for the summary
                Timber.v("###REPLACE ignoring event for summary, it's to old ${eventId}")
            }
        }

    }

    private fun handleInitialAggregatedRelations(event: Event, roomId: String, aggregation: AggregatedAnnotation, realm: Realm) {
        aggregation.chunk?.forEach {
            if (it.type == EventType.REACTION) {
                val eventId = event.eventId ?: ""
                val existing = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
                if (existing == null) {
                    val eventSummary = EventAnnotationsSummaryEntity.create(realm, eventId)
                    eventSummary.roomId = roomId
                    val sum = realm.createObject(ReactionAggregatedSummaryEntity::class.java)
                    sum.key = it.key
                    sum.firstTimestamp = event.originServerTs ?: 0 //TODO how to maintain order?
                    sum.count = it.count
                    eventSummary.reactionsSummary.add(sum)
                } else {
                    //TODO how to handle that
                }
            }
        }
    }

    fun handleReaction(event: Event, roomId: String, realm: Realm, userId: String, isLocalEcho: Boolean) {
        event.content.toModel<ReactionContent>()?.let { content ->
            //rel_type must be m.annotation
            if (RelationType.ANNOTATION == content.relatesTo?.type) {
                val reaction = content.relatesTo.key
                val eventId = content.relatesTo.eventId
                val eventSummary = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
                        ?: EventAnnotationsSummaryEntity.create(realm, eventId).apply { this.roomId = roomId }

                var sum = eventSummary.reactionsSummary.find { it.key == reaction }
                val txId = event.unsignedData?.transactionId
                if (isLocalEcho && txId.isNullOrBlank()) {
                    Timber.w("Received a local echo with no transaction ID")
                }
                if (sum == null) {
                    sum = realm.createObject(ReactionAggregatedSummaryEntity::class.java)
                    sum.key = reaction
                    sum.firstTimestamp = event.originServerTs ?: 0
                    if (isLocalEcho) {
                        Timber.v("Adding local echo reaction $reaction")
                        sum.sourceLocalEcho.add(txId)
                        sum.count = 1
                    } else {
                        Timber.v("Adding synced reaction $reaction")
                        sum.count = 1
                        sum.sourceEvents.add(event.eventId)
                    }
                    sum.addedByMe = sum.addedByMe || (userId == event.sender)
                    eventSummary.reactionsSummary.add(sum)
                } else {
                    //is this a known event (is possible? pagination?)
                    if (!sum.sourceEvents.contains(eventId)) {

                        //check if it's not the sync of a local echo
                        if (!isLocalEcho && sum.sourceLocalEcho.contains(txId)) {
                            //ok it has already been counted, just sync the list, do not touch count
                            Timber.v("Ignoring synced of local echo for reaction $reaction")
                            sum.sourceLocalEcho.remove(txId)
                            sum.sourceEvents.add(event.eventId)
                        } else {
                            sum.count += 1
                            if (isLocalEcho) {
                                Timber.v("Adding local echo reaction $reaction")
                                sum.sourceLocalEcho.add(txId)
                            } else {
                                Timber.v("Adding synced reaction $reaction")
                                sum.sourceEvents.add(event.eventId)
                            }

                            sum.addedByMe = sum.addedByMe || (userId == event.sender)
                        }

                    }
                }

            }
        }
    }

    /**
     * Called when an event is deleted
     */
    fun handleRedactionOfReplace(redacted: EventEntity, relatedEventId: String, realm: Realm) {
        Timber.d("Handle redaction of m.replace")
        val eventSummary = EventAnnotationsSummaryEntity.where(realm, relatedEventId).findFirst()
        if (eventSummary == null) {
            Timber.w("Redaction of a replace targeting an unknown event $relatedEventId")
            return
        }
        val sourceEvents = eventSummary.editSummary?.sourceEvents
        val sourceToDiscard = sourceEvents?.indexOf(redacted.eventId)
        if (sourceToDiscard == null) {
            Timber.w("Redaction of a replace that was not known in aggregation $sourceToDiscard")
            return
        }
        //Need to remove this event from the redaction list and compute new aggregation state
        sourceEvents.removeAt(sourceToDiscard)
        val previousEdit = sourceEvents.mapNotNull { EventEntity.where(realm, it).findFirst() }.sortedBy { it.originServerTs }.lastOrNull()
        if (previousEdit == null) {
            //revert to original
            eventSummary.editSummary?.deleteFromRealm()
        } else {
            //I have the last event
            ContentMapper.map(previousEdit.content)?.toModel<MessageContent>()?.newContent?.let { newContent ->
                eventSummary.editSummary?.lastEditTs = previousEdit.originServerTs
                        ?: System.currentTimeMillis()
                eventSummary.editSummary?.aggregatedContent = ContentMapper.map(newContent)
            } ?: run {
                Timber.e("Failed to udate edited summary")
                //TODO how to reccover that
            }

        }
    }

    fun handleReactionRedact(eventToPrune: EventEntity, realm: Realm, userId: String) {
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