/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.sync

import arrow.core.Try
import im.vector.matrix.android.internal.task.Task
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