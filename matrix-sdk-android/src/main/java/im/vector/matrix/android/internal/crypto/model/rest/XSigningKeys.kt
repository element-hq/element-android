package im.vector.matrix.android.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.crypto.model.MXKeysObject

@JsonClass(generateAdapter = true)
data class XSigningKeys(
        @Json(name = "user_id")
        override val userId: String,

        @Json(name = "usage")
        val usage: List<String>,

        @Json(name = "keys")
        override val keys: Map<String, String>,

        @Json(name = "signatures")
        override val signatures: Map<String, Map<String, String>>?
) : MXKeysObject
