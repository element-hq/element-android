package im.vector.matrix.android.internal.events.sync

import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.events.sync.data.SyncResponse
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Synchronizer(private val syncAPI: SyncAPI,
                   private val coroutineDispatchers: MatrixCoroutineDispatchers,
                   private val jsonMapper: Moshi) {

    fun synchronize(callback: MatrixCallback<SyncResponse>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val params = HashMap<String, String>()
            params["timeout"] = "0"
            params["filter"] = "{}"
            val syncResponse = executeRequest<SyncResponse> {
                apiCall = syncAPI.sync(params)
                moshi = jsonMapper
                dispatcher = coroutineDispatchers.io
            }
            syncResponse.either({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

}