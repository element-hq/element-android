package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.room.Signed

@JsonClass(generateAdapter = true)
data class Invite(
        @Json(name = "display_name") val displayName: String,
        @Json(name = "signed") val signed: Signed

)