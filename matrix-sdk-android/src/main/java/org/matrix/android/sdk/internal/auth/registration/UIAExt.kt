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

package org.matrix.android.sdk.internal.auth.registration

import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.toRegistrationFlowResponse
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

/**
 * Handle a UIA challenge
 *
 * @param failure the failure to handle
 * @param interceptor see doc in [UserInteractiveAuthInterceptor]
 * @param retryBlock called at the end of the process, in this block generally retry executing the task, with
 * provided authUpdate
 * @return true if UIA is handled without error
 */
internal suspend fun handleUIA(failure: Throwable,
                               interceptor: UserInteractiveAuthInterceptor,
                               retryBlock: suspend (UIABaseAuth) -> Unit): Boolean {
    Timber.d("## UIA: check error ${failure.message}")
    val flowResponse = failure.toRegistrationFlowResponse()
            ?: return false.also {
                Timber.d("## UIA: not a UIA error")
            }

    Timber.d("## UIA: error can be passed to interceptor")
    Timber.d("## UIA: type = ${flowResponse.flows}")

    Timber.d("## UIA: delegate to interceptor...")
    val authUpdate = try {
        suspendCoroutine<UIABaseAuth> { continuation ->
            interceptor.performStage(flowResponse, (failure as? Failure.ServerError)?.error?.code, continuation)
        }
    } catch (failure2: Throwable) {
        Timber.w(failure2, "## UIA: failed to participate")
        return false
    }

    Timber.d("## UIA: updated auth")
    return try {
        retryBlock(authUpdate)
        true
    } catch (failure3: Throwable) {
        handleUIA(failure3, interceptor, retryBlock)
    }
}
