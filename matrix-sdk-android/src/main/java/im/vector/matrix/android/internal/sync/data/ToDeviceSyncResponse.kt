package im.vector.matrix.android.internal.sync.data


import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.events.Event

// ToDeviceSyncResponse represents the data directly sent to one of user's devices.
@JsonClass(generateAdapter = true)
data class ToDeviceSyncResponse(

        /**
         * List of direct-to-device events.
         */
        var events: List<Event>? = null
)