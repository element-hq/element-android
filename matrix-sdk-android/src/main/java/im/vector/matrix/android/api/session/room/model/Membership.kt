package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json

enum class Membership(val value: String) {

    @Json(name = "invite")
    INVITE("invite"),

    @Json(name = "join")
    JOIN("join"),

    @Json(name = "knock")
    KNOCK("knock"),

    @Json(name = "leave")
    LEAVE("leave"),

    @Json(name = "ban")
    BAN("ban");

}
