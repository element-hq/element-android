package im.vector.matrix.core.internal.sync.data

import com.squareup.moshi.JsonClass
import im.vector.matrix.core.api.events.Event

//  PresenceSyncResponse represents the updates to the presence status of other users during server sync v2.
@JsonClass(generateAdapter = true)
data class PresenceSyncResponse(

        /**
         * List of presence events (array of Event with type m.presence).
         */
        var events: List<Event>? = null
)