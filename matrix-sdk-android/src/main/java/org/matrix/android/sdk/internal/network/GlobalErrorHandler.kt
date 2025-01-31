/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.TaskExecutor
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class GlobalErrorHandler @Inject constructor(
        private val taskExecutor: TaskExecutor,
        private val sessionParamsStore: SessionParamsStore,
        @SessionId private val sessionId: String
) : GlobalErrorReceiver {

    var listener: Listener? = null

    override fun handleGlobalError(globalError: GlobalError) {
        Timber.e("Global error received: $globalError")

        if (globalError is GlobalError.InvalidToken && globalError.softLogout) {
            // Mark the token has invalid
            taskExecutor.executorScope.launch(Dispatchers.IO) {
                sessionParamsStore.setTokenInvalid(sessionId)
            }
        }
        listener?.onGlobalError(globalError)
    }

    internal interface Listener {
        fun onGlobalError(globalError: GlobalError)
    }
}
