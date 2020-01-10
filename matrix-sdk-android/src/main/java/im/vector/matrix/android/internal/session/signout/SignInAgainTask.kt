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

package im.vector.matrix.android.internal.session.signout

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.auth.data.PasswordLoginParams
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
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
        private val eventBus: EventBus
) : SignInAgainTask {

    override suspend fun execute(params: SignInAgainTask.Params) {
        val newCredentials = executeRequest<Credentials>(eventBus) {
            apiCall = signOutAPI.loginAgain(
                    PasswordLoginParams.userIdentifier(
                            // Reuse the same userId
                            sessionParams.credentials.userId,
                            params.password,
                            // The spec says the initial device name will be ignored
                            // https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-login
                            // but https://github.com/matrix-org/synapse/issues/6525
                            // Reuse the same deviceId
                            deviceId = sessionParams.credentials.deviceId
                    )
            )
        }

        sessionParamsStore.updateCredentials(newCredentials)
    }
}
