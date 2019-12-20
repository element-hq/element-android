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

import im.vector.matrix.android.R
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.filter.FilterRepository
import im.vector.matrix.android.internal.session.homeserver.GetHomeServerCapabilitiesTask
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.session.user.UserStore
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal interface SyncTask : Task<SyncTask.Params, Unit> {

    data class Params(var timeout: Long = 30_000L)
}

internal class DefaultSyncTask @Inject constructor(private val syncAPI: SyncAPI,
                                                   @UserId private val userId: String,
                                                   private val filterRepository: FilterRepository,
                                                   private val syncResponseHandler: SyncResponseHandler,
                                                   private val sessionParamsStore: SessionParamsStore,
                                                   private val initialSyncProgressService: DefaultInitialSyncProgressService,
                                                   private val syncTokenStore: SyncTokenStore,
                                                   private val getHomeServerCapabilitiesTask: GetHomeServerCapabilitiesTask,
                                                   private val userStore: UserStore
) : SyncTask {

    override suspend fun execute(params: SyncTask.Params) {
        // Maybe refresh the home server capabilities data we know
        getHomeServerCapabilitiesTask.execute(Unit)

        val requestParams = HashMap<String, String>()
        var timeout = 0L
        val token = syncTokenStore.getLastToken()
        if (token != null) {
            requestParams["since"] = token
            timeout = params.timeout
        }
        requestParams["timeout"] = timeout.toString()
        requestParams["filter"] = filterRepository.getFilter()

        val isInitialSync = token == null
        if (isInitialSync) {
            // We might want to get the user information in parallel too
            userStore.createOrUpdate(userId)
            initialSyncProgressService.endAll()
            initialSyncProgressService.startTask(R.string.initial_sync_start_importing_account, 100)
        }
        val syncResponse = executeRequest<SyncResponse> {
            apiCall = syncAPI.sync(requestParams)
        }
        syncResponseHandler.handleResponse(syncResponse, token)
        syncTokenStore.saveToken(syncResponse.nextBatch)
        if (isInitialSync) {
            initialSyncProgressService.endAll()
        }
    }
}
