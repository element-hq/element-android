/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

internal interface RegisterCustomTask : Task<RegisterCustomTask.Params, Credentials> {
    data class Params(
            val registrationCustomParams: RegistrationCustomParams
    )
}

internal class DefaultRegisterCustomTask(
        private val authAPI: AuthAPI
) : RegisterCustomTask {

    override suspend fun execute(params: RegisterCustomTask.Params): Credentials {
        try {
            return executeRequest(null) {
                authAPI.registerCustom(params.registrationCustomParams)
            }
        } catch (throwable: Throwable) {
            throw throwable.toRegistrationFlowResponse()
                    ?.let { Failure.RegistrationFlowError(it) }
                    ?: throwable
        }
    }
}
