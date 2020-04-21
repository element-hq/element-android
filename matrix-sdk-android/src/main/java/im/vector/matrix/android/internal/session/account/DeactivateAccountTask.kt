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

package im.vector.matrix.android.internal.session.account

import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.cleanup.CleanupSession
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface DeactivateAccountTask : Task<DeactivateAccountTask.Params, Unit> {
    data class Params(
            val password: String,
            val eraseAllData: Boolean
    )
}

internal class DefaultDeactivateAccountTask @Inject constructor(
        private val accountAPI: AccountAPI,
        private val eventBus: EventBus,
        @UserId private val userId: String,
        private val cleanupSession: CleanupSession
) : DeactivateAccountTask {

    override suspend fun execute(params: DeactivateAccountTask.Params) {
        val deactivateAccountParams = DeactivateAccountParams.create(userId, params.password, params.eraseAllData)
        try {
            executeRequest<Unit>(eventBus) {
                apiCall = accountAPI.deactivate(deactivateAccountParams)
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.OtherServerError
                    && throwable.httpCode == 401
                    /* Avoid infinite loop */
                    && deactivateAccountParams.auth?.session == null) {
                try {
                    MoshiProvider.providesMoshi()
                            .adapter(RegistrationFlowResponse::class.java)
                            .fromJson(throwable.errorBody)
                } catch (e: Exception) {
                    null
                }?.let {
                    // Retry with authentication
                    try {
                        executeRequest<Unit>(eventBus) {
                            apiCall = accountAPI.deactivate(
                                    deactivateAccountParams.copy(auth = deactivateAccountParams.auth?.copy(session = it.session))
                            )
                        }
                        return
                    } catch (failure: Throwable) {
                        throw failure
                    }
                }
            }
            throw throwable
        }

        cleanupSession.handle()
    }
}
