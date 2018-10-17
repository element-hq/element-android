package im.vector.matrix.android.internal.session.sync.model


import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

// ToDeviceSyncResponse represents the data directly sent to one of user's devices.
@JsonClass(generateAdapter = true)
data class ToDeviceSyncResponse(

        /**
         * List of direct-to-device events.
         */
        val events: List<Event>? = null
)