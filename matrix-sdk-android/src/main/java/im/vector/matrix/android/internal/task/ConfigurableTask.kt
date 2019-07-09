/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.task

import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable

internal fun <PARAMS, RESULT> Task<PARAMS, RESULT>.configureWith(params: PARAMS): ConfigurableTask<PARAMS, RESULT> {
    return ConfigurableTask(this, params)
}

/**
 * Convert a Task to a ConfigurableTask without parameter
 */
internal fun <RESULT> Task<Unit, RESULT>.toConfigurableTask(): ConfigurableTask<Unit, RESULT> {
    return ConfigurableTask(this, Unit)
}

internal data class ConfigurableTask<PARAMS, RESULT>(
        val task: Task<PARAMS, RESULT>,
        val params: PARAMS,
        val callbackThread: TaskThread = TaskThread.MAIN,
        val executionThread: TaskThread = TaskThread.IO,
        val retryCount: Int = 0,
        val callback: MatrixCallback<RESULT> = object : MatrixCallback<RESULT> {}
) : Task<PARAMS, RESULT> {


    override suspend fun execute(params: PARAMS): Try<RESULT> {
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

    fun enableRetry(retryCount: Int = Int.MAX_VALUE): ConfigurableTask<PARAMS, RESULT> {
        return copy(retryCount = retryCount)
    }

    fun executeBy(taskExecutor: TaskExecutor): Cancelable {
        return taskExecutor.execute(this)
    }

    override fun toString(): String {
        return task.javaClass.name
    }

}


