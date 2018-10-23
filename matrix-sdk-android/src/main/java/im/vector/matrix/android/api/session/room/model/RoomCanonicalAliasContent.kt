package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomCanonicalAliasContent(
        @Json(name = "alias") val canonicalAlias: String? = null
)