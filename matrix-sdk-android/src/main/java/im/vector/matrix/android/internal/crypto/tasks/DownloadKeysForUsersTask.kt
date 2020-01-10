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

package im.vector.matrix.android.internal.crypto.tasks

import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.model.rest.KeysQueryBody
import im.vector.matrix.android.internal.crypto.model.rest.KeysQueryResponse
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface DownloadKeysForUsersTask : Task<DownloadKeysForUsersTask.Params, KeysQueryResponse> {
    data class Params(
            // the list of users to get keys for.
            val userIds: List<String>?,
            // the up-to token
            val token: String?)
}

internal class DefaultDownloadKeysForUsers @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val eventBus: EventBus
) : DownloadKeysForUsersTask {

    override suspend fun execute(params: DownloadKeysForUsersTask.Params): KeysQueryResponse {
        val downloadQuery = params.userIds?.associateWith { emptyMap<String, Any>() }.orEmpty()

        val body = KeysQueryBody(
                deviceKeys = downloadQuery
        )

        if (!params.token.isNullOrEmpty()) {
            body.token = params.token
        }

        return executeRequest(eventBus) {
            apiCall = cryptoApi.downloadKeysForUsers(body)
        }
    }
}
