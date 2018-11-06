package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface SyncAPI {

    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "sync")
    fun sync(@QueryMap params: Map<String, String>): Call<SyncResponse>

}