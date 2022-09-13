/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.signout

import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.auth.data.PasswordLoginParams
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface SignInAgainTask : Task<SignInAgainTask.Params, Unit> {
    data class Params(
            val password: String
    )
}

internal class DefaultSignInAgainTask @Inject constructor(
        private val signOutAPI: SignOutAPI,
        private val sessionParams: SessionParams,
        private val sessionParamsStore: SessionParamsStore,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SignInAgainTask {

    override suspend fun execute(params: SignInAgainTask.Params) {
        val newCredentials = executeRequest(globalErrorReceiver) {
            signOutAPI.loginAgain(
                    PasswordLoginParams.userIdentifier(
                            // Reuse the same userId
                            user = sessionParams.userId,
                            password = params.password,
                            // The spec says the initial device name will be ignored
                            // https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-login
                            // but https://github.com/matrix-org/synapse/issues/6525
                            deviceDisplayName = null,
                            // Reuse the same deviceId
                            deviceId = sessionParams.deviceId
                    )
            )
        }

        sessionParamsStore.updateCredentials(newCredentials)
    }
}
