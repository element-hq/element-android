package im.vector.matrix.android.internal.session.sync

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.leftIfNull
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.legacy.rest.model.filter.FilterBody
import im.vector.matrix.android.internal.legacy.util.FilterUtil
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SyncRequest(private val syncAPI: SyncAPI,
                           private val coroutineDispatchers: MatrixCoroutineDispatchers,
                           private val syncResponseHandler: SyncResponseHandler) {


    fun execute(token: String?, callback: MatrixCallback<SyncResponse>): Cancelable {
        val job = GlobalScope.launch {
            val syncOrFailure = execute(token)
            syncOrFailure.bimap({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun execute(token: String?) = withContext(coroutineDispatchers.io) {
        val params = HashMap<String, String>()
        val filterBody = FilterBody()
        FilterUtil.enableLazyLoading(filterBody, true)
        var timeout = 0
        if (token != null) {
            params["since"] = token
            timeout = 30000
        }
        params["timeout"] = timeout.toString()
        params["filter"] = filterBody.toJSONString()
        executeRequest<SyncResponse> {
            apiCall = syncAPI.sync(params)
        }.leftIfNull {
            Failure.Unknown(RuntimeException("Sync response shouln't be null"))
        }.flatMap {
            try {
                syncResponseHandler.handleResponse(it, token, false)
                Either.right(it)
            } catch (exception: Exception) {
                Either.Left(Failure.Unknown(exception))
            }
        }
    }

}