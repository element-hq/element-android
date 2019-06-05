package im.vector.riotredesign.features.home.room.detail.timeline.action

import com.airbnb.epoxy.TypedEpoxyController


class ViewReactionsEpoxyController : TypedEpoxyController<DisplayReactionsViewState>() {

    override fun buildModels(state: DisplayReactionsViewState) {
        val map = state.mapReactionKeyToMemberList() ?: return
        map.forEach {
            reactionInfoSimpleItem {
                id(it.eventId)
                timeStamp(it.timestamp)
                reactionKey(it.reactionKey)
                authorDisplayName(it.authorName ?: it.authorId)
            }
        }
    }
}