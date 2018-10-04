package im.vector.matrix.android.internal.network

import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.util.Either
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

suspend inline fun <DATA> executeRequest(block: Request<DATA>.() -> Unit) = Request<DATA>().apply(block).execute()

class Request<DATA> {

    lateinit var apiCall: Deferred<Response<DATA>>
    lateinit var moshi: Moshi
    lateinit var dispatcher: CoroutineDispatcher

    suspend fun execute(): Either<Failure, DATA?> = withContext(dispatcher) {
        return@withContext try {
            
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