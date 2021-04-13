/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
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
            executeRequest(null) {
                params.identityAuthAPI.ping()
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.ServerError && throwable.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                // Check if API v1 is available
                executeRequest(null) {
                    params.identityAuthAPI.pingV1()
                }
                // API V1 is responding, but not V2 -> Outdated
                throw IdentityServiceError.OutdatedIdentityServer
            } else {
                throw throwable
            }
        }
    }
}
