package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

//  PresenceSyncResponse represents the updates to the presence status of other users during server sync v2.
@JsonClass(generateAdapter = true)
data class PresenceSyncResponse(

        /**
         * List of presence events (array of Event with type m.presence).
         */
        val events: List<Event>? = null
)