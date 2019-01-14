package im.vector.riotredesign.features.home.room.detail.timeline

import im.vector.matrix.android.api.session.events.model.TimelineEvent

class TextItemFactory {

    fun create(event: TimelineEvent): TextItem? {
        val text = "${event.root.type} events are not yet handled"
        return TextItem(text = text)
    }

}