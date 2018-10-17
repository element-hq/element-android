package im.vector.matrix.android.api.session.events.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnsignedData(
        @Json(name = "age") val age: Long?,
        @Json(name = "redacted_because") val redactedEvent: Event? = null,
        @Json(name = "transaction_id") val transactionId: String? = null
)