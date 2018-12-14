package im.vector.matrix.android.internal

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class TaskExecutor(private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    fun <PARAMS, RESULT> executeTask(task: Task<PARAMS, RESULT>,
                                     params: PARAMS,
                                     callback: MatrixCallback<RESULT>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val resultOrFailure = withContext(coroutineDispatchers.io) {
                Timber.v("Executing ${task.javaClass} on ${Thread.currentThread().name}")
                task.execute(params)
            }
            resultOrFailure.fold({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

}