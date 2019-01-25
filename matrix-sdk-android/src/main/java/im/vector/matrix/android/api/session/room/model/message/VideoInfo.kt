package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoInfo(
        @Json(name = "mimetype") val mimeType: String,
        @Json(name = "w") val w: Int,
        @Json(name = "h") val h: Int,
        @Json(name = "size") val size: Long,
        @Json(name = "duration") val duration: Int,
        @Json(name = "thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null,
        @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)