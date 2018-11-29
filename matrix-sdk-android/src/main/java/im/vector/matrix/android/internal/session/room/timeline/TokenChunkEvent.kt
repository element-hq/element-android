package im.vector.matrix.android.internal.session.room.timeline

import im.vector.matrix.android.api.session.events.model.Event

internal interface TokenChunkEvent {
    val start: String?
    val end: String?
    val events: List<Event>
    val stateEvents: List<Event>
}