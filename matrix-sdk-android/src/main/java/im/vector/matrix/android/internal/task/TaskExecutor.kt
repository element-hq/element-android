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

import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.di.MatrixScope
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.toCancelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@MatrixScope
internal class TaskExecutor @Inject constructor(private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    private val executorScope = CoroutineScope(SupervisorJob())

    fun <PARAMS, RESULT> execute(task: ConfigurableTask<PARAMS, RESULT>): Cancelable {
        return executorScope
                .launch(task.callbackThread.toDispatcher()) {
                    val resultOrFailure = runCatching {
                        withContext(task.executionThread.toDispatcher()) {
                            Timber.v("Enqueue task $task")
                            retry(task.retryCount) {
                                Timber.v("Execute task $task on ${Thread.currentThread().name}")
                                task.execute(task.params)
                            }
                        }
                    }
                    resultOrFailure
                            .onFailure {
                                Timber.d(it, "Task failed")
                            }
                            .foldToCallback(task.callback)
                }
                .toCancelable()
    }

    fun cancelAll() = executorScope.coroutineContext.cancelChildren()

    private suspend fun <T> retry(
            times: Int = Int.MAX_VALUE,
            initialDelay: Long = 100, // 0.1 second
            maxDelay: Long = 10_000,    // 10 second
            factor: Double = 2.0,
            block: suspend () -> T): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                Timber.v("Retry task after $currentDelay ms")
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
        TaskThread.CRYPTO      -> coroutineDispatchers.crypto
    }
}
