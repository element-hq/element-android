package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.api.session.room.model.ReactionAggregatedSummary
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity

internal object EventAnnotationsSummaryMapper {
    fun map(annotationsSummary: EventAnnotationsSummaryEntity): EventAnnotationsSummary {
        return EventAnnotationsSummary(
                eventId = annotationsSummary.eventId,
                reactionsSummary = annotationsSummary.reactionsSummary.toList().map {
                    ReactionAggregatedSummary(
                            it.key,
                            it.count,
                            it.addedByMe,
                            it.firstTimestamp,
                            it.sourceEvents.toList()
                    )
                }
        )
    }
}

internal fun EventAnnotationsSummaryEntity.asDomain(): EventAnnotationsSummary {
    return EventAnnotationsSummaryMapper.map(this)
}