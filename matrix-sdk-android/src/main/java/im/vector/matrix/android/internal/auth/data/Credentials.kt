package im.vector.matrix.android.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Credentials(
        @Json(name = "user_id") val userId: String,
        @Json(name = "home_server") val homeServer: String,
        @Json(name = "access_token") val accessToken: String,
        @Json(name = "refresh_token") val refreshToken: String?,
        @Json(name = "device_id") val deviceId: String?)
