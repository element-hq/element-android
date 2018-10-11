package im.vector.matrix.android.internal.events.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StateEvent(
        @Json(name = "name") val name: String? = null,
        @Json(name = "topic") val topic: String? = null,
        @Json(name = "join_rule") val joinRule: String? = null,
        @Json(name = "guest_access") val guestAccess: String? = null,
        @Json(name = "alias") val canonicalAlias: String? = null,
        @Json(name = "aliases") val aliases: List<String>? = null,
        @Json(name = "algorithm") val algorithm: String? = null,
        @Json(name = "history_visibility") val historyVisibility: String? = null,
        @Json(name = "url") val url: String? = null,
        @Json(name = "groups") val groups: List<String>? = null
)