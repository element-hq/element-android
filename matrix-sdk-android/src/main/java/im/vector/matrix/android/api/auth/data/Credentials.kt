package im.vector.matrix.android.api.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
@JsonClass(generateAdapter = true)
data class Credentials(@Id var id: Long = 0,
                       @Json(name = "user_id") val userId: String,
                       @Json(name = "home_server") val homeServer: String,
                       @Json(name = "access_token") val accessToken: String,
                       @Json(name = "refresh_token") val refreshToken: String?,
                       @Json(name = "device_id") val deviceId: String?)
