/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.network

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.failure.ConsentNotGivenError
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.di.MoshiProvider
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import timber.log.Timber
import java.io.IOException

internal suspend inline fun <DATA> executeRequest(block: Request<DATA>.() -> Unit) = Request<DATA>().apply(block).execute()

internal class Request<DATA> {

    private val moshi: Moshi = MoshiProvider.providesMoshi()
    lateinit var apiCall: Call<DATA>

    suspend fun execute(): DATA {
        return try {
            val response = apiCall.awaitResponse()
            if (response.isSuccessful) {
                response.body()
                        ?: throw IllegalStateException("The request returned a null body")
            } else {
                throw manageFailure(response.errorBody(), response.code())
            }
        } catch (exception: Throwable) {
            throw when (exception) {
                is IOException              -> Failure.NetworkConnection(exception)
                is Failure.ServerError,
                is Failure.OtherServerError -> exception
                is CancellationException    -> Failure.Cancelled(exception)
                else                        -> Failure.Unknown(exception)
            }
        }
    }

    private fun manageFailure(errorBody: ResponseBody?, httpCode: Int): Throwable {
        if (errorBody == null) {
            return RuntimeException("Error body should not be null")
        }

        val errorBodyStr = errorBody.string()

        val matrixErrorAdapter = moshi.adapter(MatrixError::class.java)

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
}