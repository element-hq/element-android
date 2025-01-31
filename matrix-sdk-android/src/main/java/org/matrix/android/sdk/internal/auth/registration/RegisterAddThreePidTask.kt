/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task

internal interface RegisterAddThreePidTask : Task<RegisterAddThreePidTask.Params, AddThreePidRegistrationResponse> {
    data class Params(
            val threePid: RegisterThreePid,
            val clientSecret: String,
            val sendAttempt: Int
    )
}

internal class DefaultRegisterAddThreePidTask(
        private val authAPI: AuthAPI
) : RegisterAddThreePidTask {

    override suspend fun execute(params: RegisterAddThreePidTask.Params): AddThreePidRegistrationResponse {
        return executeRequest(null) {
            authAPI.add3Pid(params.threePid.toPath(), AddThreePidRegistrationParams.from(params))
        }
    }

    private fun RegisterThreePid.toPath(): String {
        return when (this) {
            is RegisterThreePid.Email -> "email"
            is RegisterThreePid.Msisdn -> "msisdn"
        }
    }
}
