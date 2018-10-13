package im.vector.matrix.android.internal.network

import arrow.core.Either
import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.di.MoshiProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

suspend inline fun <DATA> executeRequest(block: Request<DATA>.() -> Unit) = Request<DATA>().apply(block).execute()

class Request<DATA> {

    var moshi: Moshi = MoshiProvider.providesMoshi()
    lateinit var apiCall: Deferred<Response<DATA>>

    suspend fun execute(): Either<Failure, DATA?> = coroutineScope {
        try {
            val response = apiCall.await()
            if (response.isSuccessful) {
                val result = response.body()
                Either.Right(result)
            } else {
                val failure = manageFailure(response.errorBody())
                Either.Left(failure)
            }

        } catch (e: Exception) {
            when (e) {
                is IOException -> Either.Left(Failure.NetworkConnection)
                else -> Either.Left(Failure.Unknown(e))
            }
        }
    }

    private fun manageFailure(errorBody: ResponseBody?): Failure {
        return try {
            val matrixError = errorBody?.let {
                val matrixErrorAdapter = moshi.adapter(MatrixError::class.java)
                matrixErrorAdapter.fromJson(errorBody.source())
            } ?: throw RuntimeException("Matrix error should not be null")

            Failure.ServerError(matrixError)

        } catch (e: Exception) {
            Failure.Unknown(e)
        }
    }

}