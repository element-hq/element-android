package im.vector.matrix.android.internal.events.sync.data


import com.squareup.moshi.Json
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
        @Json(name = "events") val events: List<Event>? = null,

        /**
         * The number of unread messages that match the push notification rules.
         */
        @Json(name = "notification_count") val notificationCount: Int? = null,

        /**
         * The number of highlighted unread messages (subset of notifications).
         */
        @Json(name = "highlight_count") val highlightCount: Int? = null)