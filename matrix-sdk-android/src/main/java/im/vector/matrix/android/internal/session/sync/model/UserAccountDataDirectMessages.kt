package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserAccountDataDirectMessages(
        @Json(name = "content") val content: Map<String, List<String>>
) : UserAccountData

