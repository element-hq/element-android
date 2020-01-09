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
package im.vector.matrix.android.internal.session.sync.job

import android.content.Context
import androidx.work.*
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.failure.isTokenError
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.session.sync.SyncTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import im.vector.matrix.android.internal.worker.WorkManagerUtil.matrixOneTimeWorkRequestBuilder
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val DEFAULT_LONG_POOL_TIMEOUT = 0L

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
        val params = WorkerParamsFactory.fromData<Params>(inputData) ?: return Result.success()
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

        const val BG_SYNC_WORK_NAME = "BG_SYNCP"

        fun requireBackgroundSync(context: Context, sessionId: String, serverTimeout: Long = 0) {
            val data = WorkerParamsFactory.toData(Params(sessionId, serverTimeout, false))
            val workRequest = matrixOneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(WorkManagerUtil.workConstraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1_000, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(BG_SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun automaticallyBackgroundSync(context: Context, sessionId: String, serverTimeout: Long = 0, delay: Long = 30_000) {
            val data = WorkerParamsFactory.toData(Params(sessionId, serverTimeout, true))
            val workRequest = matrixOneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(WorkManagerUtil.workConstraints)
                    .setInputData(data)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, delay, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(BG_SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun stopAnyBackgroundSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(BG_SYNC_WORK_NAME)
        }
    }
}
