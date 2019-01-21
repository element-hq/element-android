package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImageInfo(
        @Json(name = "mimetype") val mimeType: String,
        @Json(name = "w") val w: Int,
        @Json(name = "h") val h: Int,
        @Json(name = "size") val size: Long,
        @Json(name = "rotation") val rotation: Int? = null,
        @Json(name = "orientation") val orientation: Int? = null,
        @Json(name = "thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null,
        @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)