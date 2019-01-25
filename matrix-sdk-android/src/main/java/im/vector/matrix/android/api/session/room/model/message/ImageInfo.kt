package im.vector.matrix.android.api.session.room.model.message

import android.media.ExifInterface
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImageInfo(
        @Json(name = "mimetype") val mimeType: String,
        @Json(name = "w") val width: Int = 0,
        @Json(name = "h") val height: Int = 0,
        @Json(name = "size") val size: Int = 0,
        @Json(name = "rotation") val rotation: Int = 0,
        @Json(name = "orientation") val orientation: Int = ExifInterface.ORIENTATION_NORMAL,
        @Json(name = "thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null,
        @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)