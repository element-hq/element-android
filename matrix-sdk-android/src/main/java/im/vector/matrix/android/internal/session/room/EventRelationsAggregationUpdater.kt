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
import im.vector.matrix.android.api.session.room.model.annotation.ReactionContent
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.model.EditAggregatedSummaryEntity
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.ReactionAggregatedSummaryEntity
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

    private fun handleReaction(event: Event, roomId: String, realm: Realm) {
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
}