package im.vector.matrix.android.api.session.room.model.annotation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReactionContent(
        @Json(name = "m.relates_to") val relatesTo: ReactionInfo? = null
)
