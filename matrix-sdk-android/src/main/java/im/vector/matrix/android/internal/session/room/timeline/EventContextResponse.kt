package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

@JsonClass(generateAdapter = true)
data class EventContextResponse(
        @Json(name = "event") val event: Event,
        @Json(name = "start") val prevToken: String? = null,
        @Json(name = "events_before") val eventsBefore: List<Event> = emptyList(),
        @Json(name = "events_after") val eventsAfter: List<Event> = emptyList(),
        @Json(name = "end") val nextToken: String? = null,
        @Json(name = "state") val stateEvents: List<Event> = emptyList()
) {

    val timelineEvents: List<Event> by lazy {
        eventsBefore + event + eventsAfter
    }


}
