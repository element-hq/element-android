package im.vector.matrix.android.api.failure

import java.io.IOException

sealed class Failure(cause: Throwable? = null) : Throwable(cause = cause) {

    data class Unknown(val throwable: Throwable? = null) : Failure(throwable)
    data class NetworkConnection(val ioException: IOException? = null) : Failure(ioException)
    data class ServerError(val error: MatrixError) : Failure(RuntimeException(error.toString()))

    abstract class FeatureFailure : Failure()

}