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
import org.matrix.android.sdk.api.failure.shouldBeRetried
import org.matrix.android.sdk.internal.network.ssl.CertUtil
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

// To use when there is no init block to provide
internal suspend inline fun <DATA : Any> executeRequest(globalErrorReceiver: GlobalErrorReceiver?,
                                                        noinline requestBlock: suspend () -> DATA): DATA {
    return executeRequest(globalErrorReceiver, requestBlock, {})
}

internal suspend inline fun <DATA : Any> executeRequest(globalErrorReceiver: GlobalErrorReceiver?,
                                                        noinline requestBlock: suspend () -> DATA,
                                                        initBlock: Request<DATA>.() -> Unit): DATA {
    return Request(globalErrorReceiver, requestBlock)
            .apply(initBlock)
            .execute()
}

internal class Request<DATA : Any>(
        private val globalErrorReceiver: GlobalErrorReceiver?,
        private val requestBlock: suspend () -> DATA
) {

    var isRetryable = false
    var initialDelay: Long = 100L
    var maxDelay: Long = 10_000L
    var maxRetryCount = Int.MAX_VALUE
    private var currentRetryCount = 0
    private var currentDelay = initialDelay

    suspend fun execute(): DATA {
        return try {
            try {
                requestBlock()
            } catch (exception: Throwable) {
                throw when (exception) {
                    is KotlinNullPointerException -> IllegalStateException("The request returned a null body")
                    is HttpException              -> exception.toFailure(globalErrorReceiver)
                    else                          -> exception
                }
            }
        } catch (exception: Throwable) {
            // Log some details about the request which has failed. This is less useful than before...
            // Timber.e("Exception when executing request ${apiCall.request().method} ${apiCall.request().url.toString().substringBefore("?")}")
            Timber.e("Exception when executing request")

            // Check if this is a certificateException
            CertUtil.getCertificateException(exception)
                    // TODO Support certificate error once logged
                    // ?.also { unrecognizedCertificateException ->
                    //    // Send the error to the bus, for a global management
                    //    eventBus?.post(GlobalError.CertificateError(unrecognizedCertificateException))
                    // }
                    ?.also { unrecognizedCertificateException -> throw unrecognizedCertificateException }

            if (isRetryable && currentRetryCount++ < maxRetryCount && exception.shouldBeRetried()) {
                delay(currentDelay)
                currentDelay = (currentDelay * 2L).coerceAtMost(maxDelay)
                return execute()
            } else {
                throw when (exception) {
                    is IOException              -> Failure.NetworkConnection(exception)
                    is Failure.ServerError,
                    is Failure.OtherServerError -> exception
                    is CancellationException    -> Failure.Cancelled(exception)
                    else                        -> Failure.Unknown(exception)
                }
            }
        }
    }
}
