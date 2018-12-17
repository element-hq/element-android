package im.vector.matrix.android.internal.task

import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext

internal class TaskExecutor(private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    fun <PARAMS, RESULT> execute(task: ConfigurableTask<PARAMS, RESULT>): Cancelable {

        val job = GlobalScope.launch(task.callbackThread.toDispatcher()) {
            val resultOrFailure = withContext(task.executionThread.toDispatcher()) {
                Timber.v("Executing ${task.javaClass} on ${Thread.currentThread().name}")
                task.execute(task.params)
            }
            resultOrFailure.fold({ task.callback.onFailure(it) }, { task.callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private fun TaskThread.toDispatcher() = when (this) {
        TaskThread.MAIN        -> coroutineDispatchers.main
        TaskThread.COMPUTATION -> coroutineDispatchers.computation
        TaskThread.IO          -> coroutineDispatchers.io
        TaskThread.CALLER      -> EmptyCoroutineContext
    }


}