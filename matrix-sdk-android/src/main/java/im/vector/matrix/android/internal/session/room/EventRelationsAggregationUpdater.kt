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

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.*
import im.vector.matrix.android.api.session.room.model.relation.ReactionContent
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import timber.log.Timber

/**
 * Acts as a listener of incoming messages in order to incrementally computes a summary of annotations.
 * For reactions will build a EventAnnotationsSummaryEntity, ans for edits a EditAggregatedSummaryEntity.
 * The summaries can then be extracted and added (as a decoration) to a TimelineEvent for final display.
 */
internal class EventRelationsAggregationUpdater(private val credentials: Credentials) {

    fun update(realm: Realm, roomId: String, events: List<Event>?) {
        events?.forEach { event ->
            when (event.type) {
                EventType.REACTION -> {
                    //we got a reaction!!
                    Timber.v("###REACTION in room $roomId")
                    handleReaction(event, roomId, realm)
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
                            handleReplace(event, content, roomId, realm)
                        }
                    }
                }
            }
        }
    }

    private fun handleReplace(event: Event, content: MessageContent, roomId: String, realm: Realm) {
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
            Timber.v("###REPLACE no edit summary for ${targetEventId}, creating one")
            //create the edit summary
            val editSummary = realm.createObject(EditAggregatedSummaryEntity::class.java)
            editSummary.lastEditTs = event.originServerTs ?: System.currentTimeMillis()
            editSummary.aggregatedContent = ContentMapper.map(newContent)
            editSummary.sourceEvents.add(eventId)

            existing.editSummary = editSummary
        } else {
            if (existingSummary.sourceEvents.contains(eventId)) {
                //ignore this event, we already know it (??)
                Timber.v("###REPLACE ignoring event for summary, it's known ${eventId}")
                return
            }
            //This message has already been edited
            if (event.originServerTs ?: 0 > existingSummary.lastEditTs ?: 0) {
                Timber.v("###REPLACE Computing aggregated edit summary")
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

    fun handleReaction(event: Event, roomId: String, realm: Realm) {
        event.content.toModel<ReactionContent>()?.let { content ->
            //rel_type must be m.annotation
            if (RelationType.ANNOTATION == content.relatesTo?.type) {
                val reaction = content.relatesTo.key
                val eventId = content.relatesTo.eventId
                val eventSummary = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
                        ?: EventAnnotationsSummaryEntity.create(realm, eventId).apply { this.roomId = roomId }

                var sum = eventSummary.reactionsSummary.find { it.key == reaction }
                if (sum == null) {
                    sum = realm.createObject(ReactionAggregatedSummaryEntity::class.java)
                    sum.key = reaction
                    sum.firstTimestamp = event.originServerTs ?: 0
                    sum.count = 1
                    sum.sourceEvents.add(event.eventId)
                    sum.addedByMe = sum.addedByMe || (credentials.userId == event.sender)
                    eventSummary.reactionsSummary.add(sum)
                } else {
                    //is this a known event (is possible? pagination?)
                    if (!sum.sourceEvents.contains(eventId)) {
                        sum.count += 1
                        sum.sourceEvents.add(event.eventId)
                        sum.addedByMe = sum.addedByMe || (credentials.userId == event.sender)
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