package im.vector.matrix.android.internal.session.room.send

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SendResponse(
        @Json(name = "event_id") val eventId: String
)