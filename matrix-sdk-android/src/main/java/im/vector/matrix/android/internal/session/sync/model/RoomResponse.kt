package im.vector.matrix.android.internal.session.sync.model


import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

/**
 * Class representing a room from a JSON response from room or global initial sync.
 */
@JsonClass(generateAdapter = true)
data class RoomResponse(
        // The room identifier.
        val roomId: String? = null,

        // The last recent messages of the room.
        val messages: TokensChunkResponse<Event>? = null,

        // The state events.
        val state: List<Event>? = null,

        // The private data that this user has attached to this room.
        val accountData: List<Event>? = null,

        // The current user membership in this room.
        val membership: String? = null,

        // The room visibility (public/private).
        val visibility: String? = null,

        // The matrix id of the inviter in case of pending invitation.
        val inviter: String? = null,

        // The invite event if membership is invite.
        val invite: Event? = null,

        // The presence status of other users (Provided in case of room initial sync @see http://matrix.org/docs/api/client-server/#!/-rooms/get_room_sync_data)).
        val presence: List<Event>? = null,

        // The read receipts (Provided in case of room initial sync).
        val receipts: List<Event>? = null
)
