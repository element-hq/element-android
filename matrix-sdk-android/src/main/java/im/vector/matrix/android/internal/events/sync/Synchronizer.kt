package im.vector.matrix.android.internal.events.sync

import arrow.core.Either
import arrow.core.flatMap
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
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

    private var token: String? = null

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
        var timeout = 0
        if (token != null) {
            params["since"] = token as String
            timeout = 30
        }
        params["timeout"] = timeout.toString()
        params["filter"] = filterBody.toJSONString()
        executeRequest<SyncResponse> {
            apiCall = syncAPI.sync(params)
        }.flatMap {
            token = it?.nextBatch
            try {
                syncResponseHandler.handleResponse(it, null, false)
                Either.right(it)
            } catch (exception: Exception) {
                Either.Left(Failure.Unknown(exception))
            }
        }
    }

}