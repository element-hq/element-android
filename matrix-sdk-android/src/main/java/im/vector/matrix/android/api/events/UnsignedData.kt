package im.vector.matrix.android.api.events

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnsignedData(
        @Json(name = "age") val age: Int,
        @Json(name = "redacted_because") val redactedEvent: Event? = null,
        @Json(name = "transaction_id") val transactionId: String
)