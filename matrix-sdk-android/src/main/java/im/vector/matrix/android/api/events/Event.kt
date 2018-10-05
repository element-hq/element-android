package im.vector.matrix.android.api.events

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi

data class Event(
        @Json(name = "event_id") val eventId: String,
        @Json(name = "type") val type: EventType,
        @Json(name = "content") val content: String,
        @Json(name = "origin_server_ts") val originServerTs: Long,
        @Json(name = "prev_content") val prevContent: String? = null,
        @Json(name = "sender") val sender: String? = null,
        @Json(name = "state_key") val stateKey: String? = null,
        @Json(name = "room_id") val roomId: String? = null,
        @Json(name = "unsigned_data") val unsignedData: UnsignedData? = null
) {

    inline fun <reified T> content(): T? {
        val moshi = Moshi.Builder().build()
        return moshi.adapter<T>(T::class.java).fromJson(content)
    }

    inline fun <reified T> prevContent(): T? {
        if (prevContent == null) {
            return null
        }
        val moshi = Moshi.Builder().build()
        return moshi.adapter<T>(T::class.java).fromJson(prevContent)
    }

}
