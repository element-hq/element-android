package im.vector.riotredesign.features.home.room.detail.timeline

import im.vector.matrix.android.api.session.events.model.TimelineEvent

class DefaultItemFactory {

    fun create(event: TimelineEvent): DefaultItem? {
        val text = "${event.root.type} events are not yet handled"
        return DefaultItem(text = text)
    }

}