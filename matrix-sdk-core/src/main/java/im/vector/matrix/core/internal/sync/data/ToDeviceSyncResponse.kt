package im.vector.matrix.core.internal.sync.data


import com.squareup.moshi.JsonClass
import im.vector.matrix.core.api.events.Event

// ToDeviceSyncResponse represents the data directly sent to one of user's devices.
@JsonClass(generateAdapter = true)
data class ToDeviceSyncResponse(

        /**
         * List of direct-to-device events.
         */
        var events: List<Event>? = null
)