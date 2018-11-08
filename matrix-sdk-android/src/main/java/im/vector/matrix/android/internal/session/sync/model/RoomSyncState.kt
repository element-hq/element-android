package im.vector.matrix.android.internal.session.sync.model


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

// RoomSyncState represents the state updates for a room during server sync v2.
@JsonClass(generateAdapter = true)
internal data class RoomSyncState(

        /**
         * List of state events (array of Event). The resulting state corresponds to the *start* of the timeline.
         */
        @Json(name = "events") val events: List<Event> = emptyList()
)
