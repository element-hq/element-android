package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThumbnailInfo(
        @Json(name = "w") val width: Int,
        @Json(name = "h") val height: Int,
        @Json(name = "size") val size: Long,
        @Json(name = "mimetype") val mimeType: String
)