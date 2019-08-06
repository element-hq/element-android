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

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import java.util.*

internal fun <PARAMS, RESULT> Task<PARAMS, RESULT>.configureWith(params: PARAMS,
                                                                 init: (ConfigurableTask.Builder<PARAMS, RESULT>.() -> Unit) = {}
): ConfigurableTask<PARAMS, RESULT> {
    return ConfigurableTask.Builder(this, params).apply(init).build()
}

internal fun <RESULT> Task<Unit, RESULT>.configureWith(init: (ConfigurableTask.Builder<Unit, RESULT>.() -> Unit) = {}): ConfigurableTask<Unit, RESULT> {
    return configureWith(Unit, init)
}

internal data class ConfigurableTask<PARAMS, RESULT>(
        val task: Task<PARAMS, RESULT>,
        val params: PARAMS,
        val id: UUID,
        val callbackThread: TaskThread,
        val executionThread: TaskThread,
        val constraints: TaskConstraints,
        val retryCount: Int,
        val callback: MatrixCallback<RESULT>

) : Task<PARAMS, RESULT> by task {


    class Builder<PARAMS, RESULT>(
            private val task: Task<PARAMS, RESULT>,
            private val params: PARAMS,
            var id: UUID = UUID.randomUUID(),
            var callbackThread: TaskThread = TaskThread.MAIN,
            var executionThread: TaskThread = TaskThread.IO,
            var constraints: TaskConstraints = TaskConstraints(),
            var retryCount: Int = 0,
            var callback: MatrixCallback<RESULT> = object : MatrixCallback<RESULT> {}
    ) {

        fun build() = ConfigurableTask(
                task = task,
                params = params,
                id = id,
                callbackThread = callbackThread,
                executionThread = executionThread,
                constraints = constraints,
                retryCount = retryCount,
                callback = callback
        )
    }

    fun executeBy(taskExecutor: TaskExecutor): Cancelable {
        return taskExecutor.execute(this)
    }

    override fun toString(): String {
        return "${task.javaClass.name} with ID: $id"
    }

}


