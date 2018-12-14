package im.vector.matrix.android.internal.session.sync

import arrow.core.Try
import im.vector.matrix.android.internal.Task
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterBody
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.util.FilterUtil

internal interface SyncTask : Task<SyncTask.Params, SyncResponse> {

    data class Params(val token: String?)

}

internal class DefaultSyncTask(private val syncAPI: SyncAPI,
                               private val syncResponseHandler: SyncResponseHandler
) : SyncTask {


    override fun execute(params: SyncTask.Params): Try<SyncResponse> {
        val requestParams = HashMap<String, String>()
        val filterBody = FilterBody()
        FilterUtil.enableLazyLoading(filterBody, true)
        var timeout = 0
        if (params.token != null) {
            requestParams["since"] = params.token
            timeout = 30000
        }
        requestParams["timeout"] = timeout.toString()
        requestParams["filter"] = filterBody.toJSONString()

        return executeRequest<SyncResponse> {
            apiCall = syncAPI.sync(requestParams)
        }.flatMap { syncResponse ->
            syncResponseHandler.handleResponse(syncResponse, params.token, false)
        }
    }


}