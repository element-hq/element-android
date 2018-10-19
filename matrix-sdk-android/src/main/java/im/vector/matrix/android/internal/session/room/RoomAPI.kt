package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.room.model.TokenChunkEvent
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RoomAPI {

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
    ): Deferred<Response<TokenChunkEvent>>


}