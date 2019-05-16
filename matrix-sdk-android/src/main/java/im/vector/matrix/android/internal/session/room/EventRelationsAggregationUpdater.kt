package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.*
import im.vector.matrix.android.api.session.room.model.annotation.ReactionContent
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.ReactionAggregatedSummaryEntity
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import timber.log.Timber


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
                    event.unsignedData?.relations?.annotations?.let {
                        Timber.v("###REACTION Agreggation in room $roomId for event ${event.eventId}")
                        handleInitialAggregatedRelations(event, roomId, it, realm)
                    }
                    //TODO message edits
                }
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
                    sum.addedByMe = sum.addedByMe || (credentials.userId == event.sender)
                    eventSummary.reactionsSummary.add(sum)
                } else {
                    //is this a known event (is possible? pagination?)
                    if (!sum.sourceEvents.contains(eventId)) {
                        sum.count += 1
                        sum.sourceEvents.add(eventId)
                        sum.addedByMe = sum.addedByMe || (credentials.userId == event.sender)
                    }
                }

            }
        }
    }
}