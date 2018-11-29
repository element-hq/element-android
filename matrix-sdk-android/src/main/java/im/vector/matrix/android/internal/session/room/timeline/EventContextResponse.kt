package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

@JsonClass(generateAdapter = true)
data class EventContextResponse(
        @Json(name = "event") val event: Event,
        @Json(name = "start") override val prevToken: String? = null,
        @Json(name = "events_before") val eventsBefore: List<Event> = emptyList(),
        @Json(name = "events_after") val eventsAfter: List<Event> = emptyList(),
        @Json(name = "end") override val nextToken: String? = null,
        @Json(name = "state") override val stateEvents: List<Event> = emptyList()
) : TokenChunkEvent {

    override val events: List<Event>
        get() = listOf(event)

}
