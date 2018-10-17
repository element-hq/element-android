package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomNameContent(
        @Json(name = "name") val name: String
)