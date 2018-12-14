package im.vector.matrix.android.session.room.timeline

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEvent

internal data class FakeTokenChunkEvent(override val start: String?,
                                        override val end: String?,
                                        override val events: List<Event> = emptyList(),
                                        override val stateEvents: List<Event> = emptyList()
) : TokenChunkEvent