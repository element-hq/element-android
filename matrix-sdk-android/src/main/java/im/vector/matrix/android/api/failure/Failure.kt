package im.vector.matrix.android.api.failure

sealed class Failure {

    data class Unknown(val exception: Exception? = null) : Failure()
    object NetworkConnection : Failure()
    data class ServerError(val error: MatrixError) : Failure()

    abstract class FeatureFailure : Failure()

}