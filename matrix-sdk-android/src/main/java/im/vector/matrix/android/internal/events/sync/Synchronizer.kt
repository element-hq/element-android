package im.vector.matrix.android.internal.events.sync

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.events.sync.data.SyncResponse
import im.vector.matrix.android.internal.legacy.rest.model.filter.FilterBody
import im.vector.matrix.android.internal.legacy.util.FilterUtil
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Synchronizer(private val syncAPI: SyncAPI,
                   private val coroutineDispatchers: MatrixCoroutineDispatchers,
                   private val syncResponseHandler: SyncResponseHandler) {

    fun synchronize(callback: MatrixCallback<SyncResponse>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val syncOrFailure = synchronize()
            syncOrFailure.bimap({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun synchronize() = withContext(coroutineDispatchers.io) {
        val params = HashMap<String, String>()
        val filterBody = FilterBody()
        FilterUtil.enableLazyLoading(filterBody, true)
        params["timeout"] = "0"
        params["filter"] = filterBody.toJSONString()
        executeRequest<SyncResponse> {
            apiCall = syncAPI.sync(params)
        }.map {
            syncResponseHandler.handleResponse(it, null, false)
            it
        }
    }

}