package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UserAccountDataFallback(
        @Json(name = "content") val content: Map<String, Any>
) : UserAccountData
