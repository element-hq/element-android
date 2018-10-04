package im.vector.matrix.android.internal.sync.data


import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.events.Event

// RoomSyncTimeline represents the timeline of messages and state changes for a room during server sync v2.
@JsonClass(generateAdapter = true)
data class RoomSyncTimeline(

        /**
         * List of events (array of Event).
         */
        var events: List<Event>? = null,

        /**
         * Boolean which tells whether there are more events on the server
         */
        var limited: Boolean = false,

        /**
         * If the batch was limited then this is a token that can be supplied to the server to retrieve more events
         */
        var prevBatch: String? = null
)