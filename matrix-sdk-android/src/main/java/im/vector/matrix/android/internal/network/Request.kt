package im.vector.matrix.android.internal.network

import arrow.core.Try
import arrow.core.failure
import arrow.core.recoverWith
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.async.async
import arrow.integrations.retrofit.adapter.runAsync
import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.di.MoshiProvider
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.IOException

inline fun <DATA> executeRequest(block: Request<DATA>.() -> Unit) = Request<DATA>().apply(block).execute()

class Request<DATA> {

    var moshi: Moshi = MoshiProvider.providesMoshi()
    lateinit var apiCall: Call<DATA>

    fun execute(): Try<DATA> {
        return Try {
            val response = apiCall.runAsync(IO.async()).fix().unsafeRunSync()
            if (response.isSuccessful) {
                response.body() ?: throw IllegalStateException("The request returned a null body")
            } else {
                throw manageFailure(response.errorBody())
            }
        }.recoverWith {
            when (it) {
                is IOException         -> Failure.NetworkConnection(it)
                is Failure.ServerError -> it
                else                   -> Failure.Unknown(it)
            }.failure()
        }
    }

    private fun manageFailure(errorBody: ResponseBody?): Throwable {
        val matrixError = errorBody?.let {
            val matrixErrorAdapter = moshi.adapter(MatrixError::class.java)
            matrixErrorAdapter.fromJson(errorBody.source())
        } ?: return RuntimeException("Matrix error should not be null")
        return Failure.ServerError(matrixError)
    }

}