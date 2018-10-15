package im.vector.matrix.android.internal.events.sync.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.events.Event

@JsonClass(generateAdapter = true)
data class RoomSyncAccountData(
        /**
         * List of account data events (array of Event).
         */
        @Json(name = "events") val events: List<Event>? = null
)