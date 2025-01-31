/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
            val newPassword: String,
            val logoutAllDevices: Boolean
    )
}

internal class DefaultChangePasswordTask @Inject constructor(
        private val accountAPI: AccountAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        @UserId private val userId: String
) : ChangePasswordTask {

    override suspend fun execute(params: ChangePasswordTask.Params) {
        val changePasswordParams = ChangePasswordParams.create(userId, params.password, params.newPassword, params.logoutAllDevices)
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
