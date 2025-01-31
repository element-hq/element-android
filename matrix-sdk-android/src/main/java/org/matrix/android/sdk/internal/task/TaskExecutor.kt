/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.di.MatrixScope
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.util.toCancelable
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@MatrixScope
internal class TaskExecutor @Inject constructor(private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    val executorScope = CoroutineScope(SupervisorJob())

    fun <PARAMS, RESULT> execute(task: ConfigurableTask<PARAMS, RESULT>): Cancelable {
        return executorScope
                .launch(task.callbackThread.toDispatcher()) {
                    val resultOrFailure = runCatching {
                        withContext(task.executionThread.toDispatcher()) {
                            Timber.v("## TASK: Enqueue task $task")
                            Timber.v("## TASK: Execute task $task on ${Thread.currentThread().name}")
                            task.executeRetry(task.params, task.maxRetryCount)
                        }
                    }
                    resultOrFailure
                            .onFailure {
                                Timber.e(it, "Task failed")
                            }
                            .foldToCallback(task.callback)
                }
                .toCancelable()
    }

    fun cancelAll() = executorScope.coroutineContext.cancelChildren()

    private fun TaskThread.toDispatcher() = when (this) {
        TaskThread.MAIN -> coroutineDispatchers.main
        TaskThread.COMPUTATION -> coroutineDispatchers.computation
        TaskThread.IO -> coroutineDispatchers.io
        TaskThread.CALLER -> EmptyCoroutineContext
        TaskThread.CRYPTO -> coroutineDispatchers.crypto
        TaskThread.DM_VERIF -> coroutineDispatchers.dmVerif
    }
}
