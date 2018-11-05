package im.vector.matrix.android.internal.session.group

import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface GroupAPI {

    /**
     * Request a group summary
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/summary")
    fun getSummary(@Path("groupId") groupId: String): Deferred<Response<GroupSummaryResponse>>


}