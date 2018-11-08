package im.vector.matrix.android.internal.network

import arrow.core.Try
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.retry
import kotlinx.coroutines.withContext

internal class RequestExecutor(private val networkConnectivityChecker: NetworkConnectivityChecker,
                               private val coroutineDispatchers: MatrixCoroutineDispatchers) {


    suspend fun <T> execute(retryTimes: Int = Int.MAX_VALUE,
                            initialDelay: Long = 100,
                            maxDelay: Long = 10_000,
                            factor: Double = 2.0,
                            block: suspend () -> Try<T>): Try<T> = withContext(coroutineDispatchers.io) {

        retry(retryTimes, initialDelay, maxDelay, factor) {
            executeIfConnected { block() }
        }

    }

    private suspend fun <T> executeIfConnected(block: suspend () -> Try<T>): Try<T> {
        return if (networkConnectivityChecker.isConnected()) {
            block()
        } else {
            Try.raise(Failure.NetworkConnection())
        }
    }


}