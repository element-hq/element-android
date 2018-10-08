package im.vector.matrix.android.internal.events.sync

import im.vector.matrix.android.internal.events.sync.data.SyncResponse
import im.vector.matrix.android.internal.network.NetworkConstants
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface SyncAPI {

    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "sync")
    fun sync(@QueryMap params: Map<String, String>): Deferred<Response<SyncResponse>>

}