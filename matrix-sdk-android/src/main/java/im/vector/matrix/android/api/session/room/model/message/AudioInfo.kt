package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AudioInfo(
        @Json(name = "mimetype") val mimeType: String,
        @Json(name = "size") val size: Long,
        @Json(name = "duration") val duration: Int
)