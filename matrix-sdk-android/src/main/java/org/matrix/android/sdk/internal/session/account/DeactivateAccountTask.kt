/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.account

import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.session.uia.UiaResult
import org.matrix.android.sdk.api.session.uia.exceptions.UiaCancelledException
import org.matrix.android.sdk.internal.auth.registration.handleUIA
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.cleanup.CleanupSession
import org.matrix.android.sdk.internal.session.identity.IdentityDisconnectTask
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface DeactivateAccountTask : Task<DeactivateAccountTask.Params, Unit> {
    data class Params(
            val eraseAllData: Boolean,
            val userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor,
            val userAuthParam: UIABaseAuth? = null
    )
}

internal class DefaultDeactivateAccountTask @Inject constructor(
        private val accountAPI: AccountAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val identityDisconnectTask: IdentityDisconnectTask,
        private val cleanupSession: CleanupSession
) : DeactivateAccountTask {

    override suspend fun execute(params: DeactivateAccountTask.Params) {
        val deactivateAccountParams = DeactivateAccountParams.create(params.userAuthParam, params.eraseAllData)
        cleanupSession.stopActiveTasks()
        val canCleanup = try {
            executeRequest(globalErrorReceiver) {
                accountAPI.deactivate(deactivateAccountParams)
            }
            true
        } catch (throwable: Throwable) {
            when (handleUIA(
                    failure = throwable,
                    interceptor = params.userInteractiveAuthInterceptor,
                    retryBlock = { authUpdate ->
                        execute(params.copy(userAuthParam = authUpdate))
                    }
            )) {
                UiaResult.SUCCESS -> {
                    false
                }
                UiaResult.FAILURE -> {
                    Timber.d("## UIA: propagate failure")
                    throw throwable
                }
                UiaResult.CANCELLED -> {
                    Timber.d("## UIA: cancelled")
                    throw UiaCancelledException()
                }
            }
        }

        if (canCleanup) {
            // Logout from identity server if any, ignoring errors
            runCatching { identityDisconnectTask.execute(Unit) }
                    .onFailure { Timber.w(it, "Unable to disconnect identity server") }

            cleanupSession.cleanup()
        }
    }
}
