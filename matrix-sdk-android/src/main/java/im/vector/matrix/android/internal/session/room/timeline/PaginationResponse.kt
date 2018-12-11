package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

@JsonClass(generateAdapter = true)
internal data class PaginationResponse(
        @Json(name = "start") override val start: String? = null,
        @Json(name = "end") override val end: String? = null,
        @Json(name = "chunk") override val events: List<Event> = emptyList(),
        @Json(name = "state") override val stateEvents: List<Event> = emptyList()
) : TokenChunkEvent