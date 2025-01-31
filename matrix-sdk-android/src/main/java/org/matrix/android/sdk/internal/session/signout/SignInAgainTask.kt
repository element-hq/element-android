/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
