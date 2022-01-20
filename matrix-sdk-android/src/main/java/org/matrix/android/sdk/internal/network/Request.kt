/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.failure.getRetryDelay
import org.matrix.android.sdk.api.failure.isLimitExceededError
import org.matrix.android.sdk.api.failure.shouldBeRetried
import org.matrix.android.sdk.internal.network.ssl.CertUtil
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

/**
 * Execute a request from the requestBlock and handle some of the Exception it could generate
 * Ref: https://github.com/matrix-org/matrix-js-sdk/blob/develop/src/scheduler.js#L138-L175
 *
 * @param globalErrorReceiver will be use to notify error such as invalid token error. See [GlobalError]
 * @param canRetry if set to true, the request will be executed again in case of error, after a delay
 * @param maxDelayBeforeRetry the max delay to wait before a retry. Note that in the case of a 429, if the provided delay exceeds this value, the error will
 * be propagated as it does not make sense to retry it with a shorter delay.
 * @param maxRetriesCount the max number of retries
 * @param requestBlock a suspend lambda to perform the network request
 */
internal suspend inline fun <DATA> executeRequest(globalErrorReceiver: GlobalErrorReceiver?,
                                                  canRetry: Boolean = false,
                                                  maxDelayBeforeRetry: Long = 32_000L,
                                                  maxRetriesCount: Int = 4,
                                                  noinline requestBlock: suspend () -> DATA): DATA {
    var currentRetryCount = 0
    var currentDelay = 1_000L

    while (true) {
        try {
            return requestBlock()
        } catch (throwable: Throwable) {
            val exception = when (throwable) {
                is KotlinNullPointerException -> IllegalStateException("The request returned a null body")
                is HttpException              -> throwable.toFailure(globalErrorReceiver)
                else                          -> throwable
            }

            // Log some details about the request which has failed.
            val request = (throwable as? HttpException)?.response()?.raw()?.request
            if (request == null) {
                Timber.e("Exception when executing request")
            } else {
                Timber.e("Exception when executing request ${request.method} ${request.url.toString().substringBefore("?")}")
            }

            // Check if this is a certificateException
            CertUtil.getCertificateException(exception)
                    // TODO Support certificate error once logged
                    // ?.also { unrecognizedCertificateException ->
                    //    // Send the error to the bus, for a global management
                    //    eventBus?.post(GlobalError.CertificateError(unrecognizedCertificateException))
                    // }
                    ?.also { unrecognizedCertificateException -> throw unrecognizedCertificateException }

            currentRetryCount++

            if (exception.isLimitExceededError() && currentRetryCount < maxRetriesCount) {
                // 429, we can retry
                val retryDelay = exception.getRetryDelay(1_000)
                if (retryDelay <= maxDelayBeforeRetry) {
                    delay(retryDelay)
                } else {
                    // delay is too high to be retried, propagate the exception
                    throw exception
                }
            } else if (canRetry && currentRetryCount < maxRetriesCount && exception.shouldBeRetried()) {
                delay(currentDelay)
                currentDelay = currentDelay.times(2L).coerceAtMost(maxDelayBeforeRetry)
                // Try again (loop)
            } else {
                throw when (exception) {
                    is IOException           -> Failure.NetworkConnection(exception)
                    is Failure.ServerError,
                    is Failure.OtherServerError,
                    is CancellationException -> exception
                    else                     -> Failure.Unknown(exception)
                }
            }
        }
    }
}
