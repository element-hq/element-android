package im.vector.riotredesign.features.home.room.detail.timeline.action

import android.graphics.Typeface
import com.airbnb.epoxy.TypedEpoxyController

/**
 * Epoxy controller for reaction event list
 */
class ViewReactionsEpoxyController(private val emojiCompatTypeface: Typeface?) : TypedEpoxyController<DisplayReactionsViewState>() {

    override fun buildModels(state: DisplayReactionsViewState) {
        val map = state.mapReactionKeyToMemberList() ?: return
        map.forEach {
            reactionInfoSimpleItem {
                id(it.eventId)
                emojiTypeFace(emojiCompatTypeface)
                timeStamp(it.timestamp)
                reactionKey(it.reactionKey)
                authorDisplayName(it.authorName ?: it.authorId)
            }
        }
    }
}