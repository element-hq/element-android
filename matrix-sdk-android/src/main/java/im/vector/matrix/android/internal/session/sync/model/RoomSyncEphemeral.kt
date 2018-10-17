package im.vector.matrix.android.internal.session.sync.model


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

// RoomSyncEphemeral represents the ephemeral events in the room that aren't recorded in the timeline or state of the room (e.g. typing).
@JsonClass(generateAdapter = true)
data class RoomSyncEphemeral(
        /**
         * List of ephemeral events (array of Event).
         */
        @Json(name = "events") val events: List<Event>? = null
)