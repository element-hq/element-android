package im.vector.matrix.android.api.rooms

import com.squareup.moshi.Json

data class Invite(
        @Json(name = "display_name") val displayName: String,
        @Json(name = "signed") val signed: Signed
)