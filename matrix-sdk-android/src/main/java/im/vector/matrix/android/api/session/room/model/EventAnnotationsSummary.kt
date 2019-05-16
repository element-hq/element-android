package im.vector.matrix.android.api.session.room.model


data class EventAnnotationsSummary(
        var eventId: String,
        var reactionsSummary: List<ReactionAggregatedSummary>
)