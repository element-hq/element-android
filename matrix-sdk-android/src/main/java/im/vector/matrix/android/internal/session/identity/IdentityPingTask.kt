/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.identity

import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.identity.IdentityServiceError
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal interface IdentityPingTask : Task<IdentityPingTask.Params, Unit> {
    data class Params(
            val identityAuthAPI: IdentityAuthAPI
    )
}

internal class DefaultIdentityPingTask @Inject constructor() : IdentityPingTask {

    override suspend fun execute(params: IdentityPingTask.Params) {
        try {
            executeRequest<Unit>(null) {
                apiCall = params.identityAuthAPI.ping()
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.ServerError && throwable.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                // Check if API v1 is available
                executeRequest<Unit>(null) {
                    apiCall = params.identityAuthAPI.pingV1()
                }
                // API V1 is responding, but not V2 -> Outdated
                throw IdentityServiceError.OutdatedIdentityServer
            } else {
                throw throwable
            }
        }
    }
}
