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
import arrow.core.failure
import arrow.core.recoverWith
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterRepository
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.task.Task

internal interface SyncTask : Task<SyncTask.Params, SyncResponse> {

    data class Params(val token: String?, var timeout: Long = 30_000L)

}

internal class DefaultSyncTask(private val syncAPI: SyncAPI,
                               private val filterRepository: FilterRepository,
                               private val syncResponseHandler: SyncResponseHandler,
                               private val sessionParamsStore: SessionParamsStore
) : SyncTask {


    override suspend fun execute(params: SyncTask.Params): Try<SyncResponse> {
        val requestParams = HashMap<String, String>()
        var timeout = 0L
        if (params.token != null) {
            requestParams["since"] = params.token
            timeout = params.timeout
        }
        requestParams["timeout"] = timeout.toString()
        requestParams["filter"] = filterRepository.getFilter()

        return executeRequest<SyncResponse> {
            apiCall = syncAPI.sync(requestParams)
        }.recoverWith { throwable ->
            // Intercept 401
            if (throwable is Failure.ServerError
                    && throwable.error.code == MatrixError.UNKNOWN_TOKEN) {
                sessionParamsStore.delete()
            }

            // Transmit the throwable
            throwable.failure()
        }.flatMap { syncResponse ->
            syncResponseHandler.handleResponse(syncResponse, params.token, false)
        }
    }


}