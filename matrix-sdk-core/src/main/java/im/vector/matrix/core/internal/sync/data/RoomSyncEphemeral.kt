package im.vector.matrix.core.internal.sync.data


import com.squareup.moshi.JsonClass
import im.vector.matrix.core.api.events.Event

// RoomSyncEphemeral represents the ephemeral events in the room that aren't recorded in the timeline or state of the room (e.g. typing).
@JsonClass(generateAdapter = true)
data class RoomSyncEphemeral(
        /**
         * List of ephemeral events (array of Event).
         */
        var events: List<Event>? = null
)