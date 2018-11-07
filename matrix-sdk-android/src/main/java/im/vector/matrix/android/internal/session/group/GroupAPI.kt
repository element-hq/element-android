package im.vector.matrix.android.internal.session.group

import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.group.model.GroupRooms
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import im.vector.matrix.android.internal.session.group.model.GroupUsers
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GroupAPI {

    /**
     * Request a group summary
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/summary")
    fun getSummary(@Path("groupId") groupId: String): Call<GroupSummaryResponse>

    /**
     * Request the rooms list.
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/rooms")
    fun getRooms(@Path("groupId") groupId: String): Call<GroupRooms>


    /**
     * Request the users list.
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/users")
    fun getUsers(@Path("groupId") groupId: String): Call<GroupUsers>


}