package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json

enum class Membership {

    @Json(name = "invite") INVITE,
    @Json(name = "join") JOIN,
    @Json(name = "knock") KNOCK,
    @Json(name = "leave") LEAVE,
    @Json(name = "ban") BAN;

}
