/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session.sync.job

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.failure.isTokenError
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.network.NetworkConnectivityChecker
import org.matrix.android.sdk.internal.session.sync.SyncTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val DEFAULT_LONG_POOL_TIMEOUT = 0L

/**
 * Possible previous worker: None
 * Possible next worker    : None
 */
internal class SyncWorker(context: Context,
                          workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val timeout: Long = DEFAULT_LONG_POOL_TIMEOUT,
            val automaticallyRetry: Boolean = false,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var syncTask: SyncTask
    @Inject lateinit var taskExecutor: TaskExecutor
    @Inject lateinit var networkConnectivityChecker: NetworkConnectivityChecker

    override suspend fun doWork(): Result {
        Timber.i("Sync work starting")
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()
                        .also { Timber.e("Unable to parse work parameters") }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)
        return runCatching {
            doSync(params.timeout)
        }.fold(
                { Result.success() },
                { failure ->
                    if (failure.isTokenError() || !params.automaticallyRetry) {
                        Result.failure()
                    } else {
                        Result.retry()
                    }
                }
        )
    }

    private suspend fun doSync(timeout: Long) {
        val taskParams = SyncTask.Params(timeout)
        syncTask.execute(taskParams)
    }

    companion object {
        private const val BG_SYNC_WORK_NAME = "BG_SYNCP"

        fun requireBackgroundSync(workManagerProvider: WorkManagerProvider, sessionId: String, serverTimeout: Long = 0) {
            val data = WorkerParamsFactory.toData(Params(sessionId, serverTimeout, false))
            val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(WorkManagerProvider.workConstraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1_000, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build()
            workManagerProvider.workManager
                    .enqueueUniqueWork(BG_SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun automaticallyBackgroundSync(workManagerProvider: WorkManagerProvider, sessionId: String, serverTimeout: Long = 0, delay: Long = 30_000) {
            val data = WorkerParamsFactory.toData(Params(sessionId, serverTimeout, true))
            val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(WorkManagerProvider.workConstraints)
                    .setInputData(data)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, delay, TimeUnit.MILLISECONDS)
                    .build()
            workManagerProvider.workManager
                    .enqueueUniqueWork(BG_SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun stopAnyBackgroundSync(workManagerProvider: WorkManagerProvider) {
            workManagerProvider.workManager
                    .cancelUniqueWork(BG_SYNC_WORK_NAME)
        }
    }
}
