package im.vector.matrix.android.api.session.events.model

import com.google.gson.JsonObject
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.legacy.util.JsonUtils

@JsonClass(generateAdapter = true)
data class Event(
        @Json(name = "type") val type: String,
        @Json(name = "event_id") val eventId: String?,
        @Json(name = "content") val content: Map<String, Any>? = null,
        @Json(name = "prev_content") val prevContent: Map<String, Any>? = null,
        @Json(name = "origin_server_ts") val originServerTs: Long? = null,
        @Json(name = "sender") val sender: String? = null,
        @Json(name = "state_key") val stateKey: String? = null,
        @Json(name = "room_id") var roomId: String? = null,
        @Json(name = "unsigned") val unsignedData: UnsignedData? = null
) {

    val contentAsJsonObject: JsonObject? by lazy {
        val gson = JsonUtils.getGson(true)
        gson.toJsonTree(content).asJsonObject
    }

    val prevContentAsJsonObject: JsonObject? by lazy {
        val gson = JsonUtils.getGson(true)
        gson.toJsonTree(prevContent).asJsonObject
    }

    inline fun <reified T> content(): T? {
        return toModel(content)
    }

    inline fun <reified T> prevContent(): T? {
        return toModel(prevContent)
    }

    inline fun <reified T> toModel(data: Map<String, Any>?): T? {
        val moshi = MoshiProvider.providesMoshi()
        val moshiAdapter = moshi.adapter(T::class.java)
        return moshiAdapter.fromJsonValue(data)
    }

}