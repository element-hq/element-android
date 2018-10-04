package im.vector.matrix.android.internal.sync.data

import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.events.Event

//  PresenceSyncResponse represents the updates to the presence status of other users during server sync v2.
@JsonClass(generateAdapter = true)
data class PresenceSyncResponse(

        /**
         * List of presence events (array of Event with type m.presence).
         */
        var events: List<Event>? = null
)