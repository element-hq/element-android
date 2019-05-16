package im.vector.matrix.android.api.session.room.model.annotation

import com.squareup.moshi.Json

data class RelationDefaultContent(
        @Json(name = "rel_type") override val type: String,
        @Json(name = "event_id") override val eventId: String
) : RelationContent
