/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.toRegistrationFlowResponse
import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task

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
                authAPI.register(params.registrationParams)
            }
        } catch (throwable: Throwable) {
            throw throwable.toRegistrationFlowResponse()
                    ?.let { Failure.RegistrationFlowError(it) }
                    ?: throwable
        }
    }
}
