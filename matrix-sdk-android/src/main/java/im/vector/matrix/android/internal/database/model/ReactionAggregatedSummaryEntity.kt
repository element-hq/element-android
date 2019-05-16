package im.vector.matrix.android.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject

/**
 * Aggregated Summary of a reaction.
 */
internal open class ReactionAggregatedSummaryEntity(
        // The reaction String 😀
        var key: String = "",
        // Number of time this reaction was selected
        var count: Int = 0,
        // Did the current user sent this reaction
        var addedByMe: Boolean = false,
        // The first time this reaction was added (for ordering purpose)
        var firstTimestamp: Long = 0,
        // The list of the eventIDs used to build the summary (might be out of sync if chunked received from message chunk)
        var sourceEvents: RealmList<String> = RealmList()
) : RealmObject() {

    companion object

}
