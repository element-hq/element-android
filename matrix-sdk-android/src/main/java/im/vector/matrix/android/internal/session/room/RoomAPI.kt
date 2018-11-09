package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.room.members.RoomMembersResponse
import im.vector.matrix.android.internal.session.room.send.SendResponse
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEvent
import retrofit2.Call
import retrofit2.http.*

internal interface RoomAPI {

    /**
     * Get a list of messages starting from a reference.
     *
     * @param roomId the room id
     * @param from   the token identifying where to start. Required.
     * @param dir    The direction to return messages from. Required.
     * @param limit  the maximum number of messages to retrieve. Optional.
     * @param filter A JSON RoomEventFilter to filter returned events with. Optional.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/messages")
    fun getRoomMessagesFrom(@Path("roomId") roomId: String,
                            @Query("from") from: String,
                            @Query("dir") dir: String,
                            @Query("limit") limit: Int,
                            @Query("filter") filter: String?
    ): Call<TokenChunkEvent>


    /**
     * Get all members of a room
     *
     * @param roomId        the room id where to get the members
     * @param syncToken     the sync token (optional)
     * @param membership    to include only one type of membership (optional)
     * @param notMembership to exclude one type of membership (optional)
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/members")
    fun getMembers(@Path("roomId") roomId: String,
                   @Query("at") syncToken: String?,
                   @Query("membership") membership: String?,
                   @Query("not_membership") notMembership: String?
    ): Call<RoomMembersResponse>


    /**
     * Send an event to a room.
     *
     * @param txId      the transaction Id
     * @param roomId    the room id
     * @param eventType the event type
     * @param content   the event content
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/send/{eventType}/{txId}")
    fun send(@Path("txId") txId: String,
             @Path("roomId") roomId: String,
             @Path("eventType") eventType: String,
             @Body content: Content?
    ): Call<SendResponse>


}