package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json

enum class RoomDirectoryVisibility {
    @Json(name = "private") PRIVATE,
    @Json(name = "public") PUBLIC
}