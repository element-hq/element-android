package im.vector.matrix.android.api.failure

import java.io.IOException

sealed class Failure {

    data class Unknown(val exception: Exception? = null) : Failure()
    data class NetworkConnection(val ioException: IOException) : Failure()
    data class ServerError(val error: MatrixError) : Failure()

    abstract class FeatureFailure : Failure()

    fun toException(): Exception {
        return when (this) {
            is Unknown           -> this.exception ?: RuntimeException("Unknown error")
            is NetworkConnection -> this.ioException
            is ServerError       -> RuntimeException(this.error.toString())
            is FeatureFailure    -> RuntimeException("Feature error")
        }

    }

}