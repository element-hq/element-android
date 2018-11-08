package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UserAccountDataSync(
        @Json(name = "events") val list: List<UserAccountData> = emptyList()
)