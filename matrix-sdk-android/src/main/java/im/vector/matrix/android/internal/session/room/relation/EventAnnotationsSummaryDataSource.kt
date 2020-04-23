package im.vector.matrix.android.internal.session.room.relation

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import im.vector.matrix.android.api.session.room.model.*
import im.vector.matrix.android.internal.database.mapper.EventAnnotationsSummaryMapper
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class EventAnnotationsSummaryDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                                     private val coroutineDispatchers: MatrixCoroutineDispatchers ) {

    fun hasAlreadySendReaction(eventId: String, reaction: String): Boolean {
        val reactionSummary = sessionDatabase.eventAnnotationsQueries.selectReaction(eventId, reaction).executeAsOneOrNull()
        return reactionSummary != null && reactionSummary.added_by_me
    }

    fun getEventAnnotationsSummary(eventId: String): EventAnnotationsSummary {
        val reactions = sessionDatabase.eventAnnotationsQueries.selectReactionsForEvent(eventId, EventAnnotationsSummaryMapper::mapReactionSummary).executeAsList()
        val edit = sessionDatabase.eventAnnotationsQueries.selectEditForEvent(eventId, EventAnnotationsSummaryMapper::mapEditSummary).executeAsOneOrNull()
        val references = sessionDatabase.eventAnnotationsQueries.selectReferenceForEvent(eventId, EventAnnotationsSummaryMapper::mapReferencesSummary).executeAsOneOrNull()
        val poll = sessionDatabase.eventAnnotationsQueries.selectPollForEvent(eventId, EventAnnotationsSummaryMapper::mapPollSummary).executeAsOneOrNull()
        return EventAnnotationsSummaryMapper.mapAnnotationsSummary(eventId, reactions, edit, references, poll)
    }

    fun getEventAnnotationsSummaryLive(eventId: String): Flow<EventAnnotationsSummary> {
        val reactionsFlow = sessionDatabase.eventAnnotationsQueries.selectReactionsForEvent(eventId, EventAnnotationsSummaryMapper::mapReactionSummary).asFlow().mapToList(coroutineDispatchers.dbQuery)
        val editFlow = sessionDatabase.eventAnnotationsQueries.selectEditForEvent(eventId, EventAnnotationsSummaryMapper::mapEditSummary).asFlow().mapToOneOrNull(coroutineDispatchers.io)
        val referencesFlow = sessionDatabase.eventAnnotationsQueries.selectReferenceForEvent(eventId, EventAnnotationsSummaryMapper::mapReferencesSummary).asFlow().mapToOneOrNull(coroutineDispatchers.io)
        val pollFlow = sessionDatabase.eventAnnotationsQueries.selectPollForEvent(eventId,  EventAnnotationsSummaryMapper::mapPollSummary).asFlow().mapToOneOrNull(coroutineDispatchers.io)
        return combine(reactionsFlow, editFlow, referencesFlow, pollFlow) { reactions: List<ReactionAggregatedSummary>, edit: EditAggregatedSummary?, references: ReferencesAggregatedSummary?, poll: PollResponseAggregatedSummary? ->
            EventAnnotationsSummaryMapper.mapAnnotationsSummary(eventId, reactions, edit, references, poll)
        }
    }


}
