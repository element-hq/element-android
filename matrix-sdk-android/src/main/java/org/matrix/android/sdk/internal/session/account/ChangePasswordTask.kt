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

package org.matrix.android.sdk.internal.session.account

import org.matrix.android.sdk.api.failure.toRegistrationFlowResponse
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ChangePasswordTask : Task<ChangePasswordTask.Params, Unit> {
    data class Params(
            val password: String,
            val newPassword: String
    )
}

internal class DefaultChangePasswordTask @Inject constructor(
        private val accountAPI: AccountAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        @UserId private val userId: String
) : ChangePasswordTask {

    override suspend fun execute(params: ChangePasswordTask.Params) {
        val changePasswordParams = ChangePasswordParams.create(userId, params.password, params.newPassword)
        try {
            executeRequest(globalErrorReceiver) {
                accountAPI.changePassword(changePasswordParams)
            }
        } catch (throwable: Throwable) {
            val registrationFlowResponse = throwable.toRegistrationFlowResponse()

            if (registrationFlowResponse != null &&
                    /* Avoid infinite loop */
                    changePasswordParams.auth?.session == null) {
                // Retry with authentication
                executeRequest(globalErrorReceiver) {
                    accountAPI.changePassword(
                            changePasswordParams.copy(auth = changePasswordParams.auth?.copy(session = registrationFlowResponse.session))
                    )
                }
            } else {
                throw throwable
            }
        }
    }
}
