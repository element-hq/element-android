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

package im.vector.matrix.android.internal.auth.registration

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.auth.AuthAPI
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task

internal interface RegisterTask : Task<RegisterTask.Params, Credentials> {
    data class Params(
            val registrationParams: RegistrationParams
    )
}

internal class DefaultRegisterTask(
        private val authAPI: AuthAPI
) : RegisterTask {

    override suspend fun execute(params: RegisterTask.Params): Credentials {
        try {
            return executeRequest(null) {
                apiCall = authAPI.register(params.registrationParams)
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.OtherServerError && throwable.httpCode == 401) {
                // Parse to get a RegistrationFlowResponse
                val registrationFlowResponse = try {
                    MoshiProvider.providesMoshi()
                            .adapter(RegistrationFlowResponse::class.java)
                            .fromJson(throwable.errorBody)
                } catch (e: Exception) {
                    null
                }
                // check if the server response can be cast
                if (registrationFlowResponse != null) {
                    throw Failure.RegistrationFlowError(registrationFlowResponse)
                } else {
                    throw throwable
                }
            } else {
                // Other error
                throw throwable
            }
        }
    }
}
