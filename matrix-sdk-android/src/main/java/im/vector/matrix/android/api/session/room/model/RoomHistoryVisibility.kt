package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json

enum class RoomHistoryVisibility {
    @Json(name = "shared") SHARED,
    @Json(name = "invited") INVITED,
    @Json(name = "joined") JOINED,
    @Json(name = "word_readable") WORLD_READABLE
}