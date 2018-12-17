package im.vector.matrix.android.internal.task

import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable

internal fun <PARAMS, RESULT> Task<PARAMS, RESULT>.configureWith(params: PARAMS): ConfigurableTask<PARAMS, RESULT> {
    return ConfigurableTask(this, params)
}

internal data class ConfigurableTask<PARAMS, RESULT>(
        val task: Task<PARAMS, RESULT>,
        val params: PARAMS,
        val callbackThread: TaskThread = TaskThread.MAIN,
        val executionThread: TaskThread = TaskThread.IO,
        val callback: MatrixCallback<RESULT> = object : MatrixCallback<RESULT> {}
) : Task<PARAMS, RESULT> {


    override fun execute(params: PARAMS): Try<RESULT> {
        return task.execute(params)
    }

    fun callbackOn(thread: TaskThread): ConfigurableTask<PARAMS, RESULT> {
        return copy(callbackThread = thread)
    }

    fun executeOn(thread: TaskThread): ConfigurableTask<PARAMS, RESULT> {
        return copy(executionThread = thread)
    }

    fun dispatchTo(matrixCallback: MatrixCallback<RESULT>): ConfigurableTask<PARAMS, RESULT> {
        return copy(callback = matrixCallback)
    }

    fun executeBy(taskExecutor: TaskExecutor): Cancelable {
        return taskExecutor.execute(this)
    }

}


