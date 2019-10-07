/*
 *
 *  * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.network

import com.squareup.moshi.JsonDataException
import im.vector.matrix.android.api.failure.ConsentNotGivenError
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.di.MoshiProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> Call<T>.awaitResponse(): Response<T> {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
}

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
internal fun <T> Response<T>.toFailure(): Failure {
    return toFailure(errorBody(), code())
}

/**
 * Convert a okhttp3 Response to a Failure, and eventually parse errorBody to convert it to a MatrixError
 */
internal fun okhttp3.Response.toFailure(): Failure {
    return toFailure(body, code)
}

private fun toFailure(errorBody: ResponseBody?, httpCode: Int): Failure {
    if (errorBody == null) {
        return Failure.Unknown(RuntimeException("errorBody should not be null"))
    }

    val errorBodyStr = errorBody.string()

    val matrixErrorAdapter = MoshiProvider.providesMoshi().adapter(MatrixError::class.java)

    try {
        val matrixError = matrixErrorAdapter.fromJson(errorBodyStr)

        if (matrixError != null) {
            if (matrixError.code == MatrixError.M_CONSENT_NOT_GIVEN && !matrixError.consentUri.isNullOrBlank()) {
                // Also send this error to the bus, for a global management
                EventBus.getDefault().post(ConsentNotGivenError(matrixError.consentUri))
            }

            return Failure.ServerError(matrixError, httpCode)
        }
    } catch (ex: JsonDataException) {
        // This is not a MatrixError
        Timber.w("The error returned by the server is not a MatrixError")
    }

    return Failure.OtherServerError(errorBodyStr, httpCode)
}
