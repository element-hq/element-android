package im.vector.matrix.android.internal.sync.data


import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.events.Event

/**
 * Class representing a room from a JSON response from room or global initial sync.
 */
@JsonClass(generateAdapter = true)
data class RoomResponse(
        // The room identifier.
        var roomId: String? = null,

        // The last recent messages of the room.
        var messages: TokensChunkResponse<Event>? = null,

        // The state events.
        var state: List<Event>? = null,

        // The private data that this user has attached to this room.
        var accountData: List<Event>? = null,

        // The current user membership in this room.
        var membership: String? = null,

        // The room visibility (public/private).
        var visibility: String? = null,

        // The matrix id of the inviter in case of pending invitation.
        var inviter: String? = null,

        // The invite event if membership is invite.
        var invite: Event? = null,

        // The presence status of other users (Provided in case of room initial sync @see http://matrix.org/docs/api/client-server/#!/-rooms/get_room_sync_data)).
        var presence: List<Event>? = null,

        // The read receipts (Provided in case of room initial sync).
        var receipts: List<Event>? = null
)
