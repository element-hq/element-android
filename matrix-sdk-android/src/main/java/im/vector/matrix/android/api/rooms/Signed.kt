package im.vector.matrix.android.api.rooms

import com.squareup.moshi.Json

data class Signed(
        @Json(name = "token") val token: String,
        @Json(name = "signatures") val signatures: Any,
        @Json(name = "mxid") val mxid: String
)