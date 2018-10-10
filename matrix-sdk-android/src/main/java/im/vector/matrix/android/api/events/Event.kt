package im.vector.matrix.android.api.events

import com.google.gson.JsonObject
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.legacy.util.JsonUtils

@JsonClass(generateAdapter = true)
data class Event(
        @Json(name = "type") val type: String,
        @Json(name = "event_id") val eventId: String? = null,
        @Json(name = "content") val content: Map<String, Any>? = null,
        @Json(name = "prev_content") val prevContent: Map<String, Any>? = null,
        @Json(name = "origin_server_ts") val originServerTs: Long? = null,
        @Json(name = "sender") val sender: String? = null,
        @Json(name = "state_key") val stateKey: String? = null,
        @Json(name = "room_id") val roomId: String? = null,
        @Json(name = "unsigned_data") val unsignedData: UnsignedData? = null
){

    fun contentAsJsonObject(): JsonObject? {
        return JsonUtils.toJson(content)
    }



}


