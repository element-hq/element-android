/*
 *
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.matrix.android.sdk.internal.network

import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.internal.di.MoshiProvider
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun okhttp3.Call.awaitResponse(): okhttp3.Response {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }

        enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
}

/**
 * Convert a retrofit Response to a Failure, and eventually parse errorBody to convert it to a MatrixError
 */
internal fun <T> Response<T>.toFailure(globalErrorReceiver: GlobalErrorReceiver?): Failure {
    return toFailure(errorBody(), code(), globalErrorReceiver)
}

/**
 * Convert a HttpException to a Failure, and eventually parse errorBody to convert it to a MatrixError
 */
internal fun HttpException.toFailure(globalErrorReceiver: GlobalErrorReceiver?): Failure {
    return toFailure(response()?.errorBody(), code(), globalErrorReceiver)
}

/**
 * Convert a okhttp3 Response to a Failure, and eventually parse errorBody to convert it to a MatrixError
 */
internal fun okhttp3.Response.toFailure(globalErrorReceiver: GlobalErrorReceiver?): Failure {
    return toFailure(body, code, globalErrorReceiver)
}

private fun toFailure(errorBody: ResponseBody?, httpCode: Int, globalErrorReceiver: GlobalErrorReceiver?): Failure {
    if (errorBody == null) {
        return Failure.Unknown(RuntimeException("errorBody should not be null"))
    }

    val errorBodyStr = errorBody.string()

    val matrixErrorAdapter = MoshiProvider.providesMoshi().adapter(MatrixError::class.java)

    try {
        val matrixError = matrixErrorAdapter.fromJson(errorBodyStr)

        if (matrixError != null) {
            // Also send following errors to the globalErrorReceiver, for a global management
            when {
                matrixError.code == MatrixError.M_CONSENT_NOT_GIVEN && !matrixError.consentUri.isNullOrBlank() -> {
                    globalErrorReceiver?.handleGlobalError(GlobalError.ConsentNotGivenError(matrixError.consentUri))
                }
                httpCode == HttpURLConnection.HTTP_UNAUTHORIZED && /* 401 */
                        matrixError.code == MatrixError.M_UNKNOWN_TOKEN                                        -> {
                    globalErrorReceiver?.handleGlobalError(GlobalError.InvalidToken(matrixError.isSoftLogout.orFalse()))
                }
                matrixError.code == MatrixError.ORG_MATRIX_EXPIRED_ACCOUNT                                     -> {
                    globalErrorReceiver?.handleGlobalError(GlobalError.ExpiredAccount)
                }
            }

            return Failure.ServerError(matrixError, httpCode)
        }
    } catch (ex: Exception) {
        // This is not a MatrixError
        Timber.w("The error returned by the server is not a MatrixError")
    } catch (ex: JsonEncodingException) {
        // This is not a MatrixError, HTML code?
        Timber.w("The error returned by the server is not a MatrixError, probably HTML string")
    }

    return Failure.OtherServerError(errorBodyStr, httpCode)
}
