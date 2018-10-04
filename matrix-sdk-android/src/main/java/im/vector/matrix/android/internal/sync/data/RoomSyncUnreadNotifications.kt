package im.vector.matrix.android.internal.sync.data


import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.events.Event

/**
 * `MXRoomSyncUnreadNotifications` represents the unread counts for a room.
 */
@JsonClass(generateAdapter = true)
data class RoomSyncUnreadNotifications(
        /**
         * List of account data events (array of Event).
         */
        val events: List<Event>? = null,

        /**
         * The number of unread messages that match the push notification rules.
         */
        val notificationCount: Int? = null,

        /**
         * The number of highlighted unread messages (subset of notifications).
         */
        val highlightCount: Int? = null)