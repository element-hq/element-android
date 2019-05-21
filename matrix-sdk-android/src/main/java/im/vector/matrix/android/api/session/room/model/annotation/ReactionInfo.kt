package im.vector.matrix.android.api.session.room.model.annotation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReactionInfo(
        @Json(name = "rel_type") override val type: String?,
        @Json(name = "event_id") override val eventId: String,
        val key: String
) : RelationContent