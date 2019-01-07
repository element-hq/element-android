package im.vector.matrix.android.internal.task

import arrow.core.Try
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext

internal class TaskExecutor(private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    fun <PARAMS, RESULT> execute(task: ConfigurableTask<PARAMS, RESULT>): Cancelable {

        val job = GlobalScope.launch(task.callbackThread.toDispatcher()) {
            val resultOrFailure = withContext(task.executionThread.toDispatcher()) {
                Timber.v("Executing $task on ${Thread.currentThread().name}")
                retry(task.retryCount) {
                    task.execute(task.params)
                }
            }
            resultOrFailure.fold({ task.callback.onFailure(it) }, { task.callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun <T> retry(
            times: Int = Int.MAX_VALUE,
            initialDelay: Long = 100, // 0.1 second
            maxDelay: Long = 10_000,    // 10 second
            factor: Double = 2.0,
            block: suspend () -> Try<T>): Try<T> {

        var currentDelay = initialDelay
        repeat(times - 1) {
            val blockResult = block()
            if (blockResult.isSuccess()) {
                return blockResult
            } else {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block()
    }

    private fun TaskThread.toDispatcher() = when (this) {
        TaskThread.MAIN        -> coroutineDispatchers.main
        TaskThread.COMPUTATION -> coroutineDispatchers.computation
        TaskThread.IO          -> coroutineDispatchers.io
        TaskThread.CALLER      -> EmptyCoroutineContext
    }


}