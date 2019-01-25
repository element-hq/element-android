package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocationInfo(
        @Json(name = "thumbnail_url") val thumbnailUrl: String,
        @Json(name = "thumbnail_info") val thumbnailInfo: ThumbnailInfo
)