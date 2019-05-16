package im.vector.matrix.android.api.session.room.model

data class ReactionAggregatedSummary(
        val key: String,                // "👍"
        val count: Int,                 // 8
        val addedByMe: Boolean,         // true
        val firstTimestamp: Long,       // unix timestamp
        val sourceEvents: List<String>
)