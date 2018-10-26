package im.vector.matrix.android.internal.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

@JsonClass(generateAdapter = true)
data class TokenChunkEvent(
        @Json(name = "start") val nextToken: String? = null,
        @Json(name = "end") val prevToken: String? = null,
        @Json(name = "chunk") val events: List<Event> = emptyList(),
        @Json(name = "state") val stateEvents: List<Event> = emptyList()
)