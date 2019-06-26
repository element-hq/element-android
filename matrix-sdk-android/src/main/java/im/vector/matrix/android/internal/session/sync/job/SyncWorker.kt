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
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterRepository
import im.vector.matrix.android.internal.session.sync.SyncAPI
import im.vector.matrix.android.internal.session.sync.SyncResponseHandler
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
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
            val userId: String,
            val timeout: Long = DEFAULT_LONG_POOL_TIMEOUT,
            val automaticallyRetry: Boolean = false
    )

    @Inject lateinit var syncAPI: SyncAPI
    @Inject lateinit var filterRepository: FilterRepository
    @Inject lateinit var syncResponseHandler: SyncResponseHandler
    @Inject lateinit var sessionParamsStore: SessionParamsStore
    @Inject lateinit var syncTokenStore: SyncTokenStore


    override suspend fun doWork(): Result {
        Timber.i("Sync work starting")
        val params = WorkerParamsFactory.fromData<Params>(inputData) ?: return Result.success()
        val sessionComponent = getSessionComponent(params.userId) ?: return Result.success()
        sessionComponent.inject(this)

        val requestParams = HashMap<String, String>()
        requestParams["timeout"] = params.timeout.toString()
        requestParams["filter"] = filterRepository.getFilter()
        val token = syncTokenStore.getLastToken()?.also { requestParams["since"] = it }
        Timber.i("Sync work last token $token")

        return executeRequest<SyncResponse> {
            apiCall = syncAPI.sync(requestParams)
        }.fold(
                {
                    if (it is Failure.ServerError
                            && it.error.code == MatrixError.UNKNOWN_TOKEN) {
                        sessionParamsStore.delete(params.userId)
                        Result.failure()
                    } else {
                        Timber.i("Sync work failed $it")
                        Result.retry()
                    }
                },
                {
                    Timber.i("Sync work success next batch ${it.nextBatch}")
                    if (!isStopped) {
                        syncResponseHandler.handleResponse(it, token, false)
                        syncTokenStore.saveToken(it.nextBatch)
                    }
                    if (params.automaticallyRetry) Result.retry() else Result.success()
                }
        )
    }

    companion object {
        fun requireBackgroundSync(context: Context, userId: String, serverTimeout: Long = 0) {
            val data = WorkerParamsFactory.toData(Params(userId, serverTimeout, false))
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInputData(data)
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1_000, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork("BG_SYNCP", ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun automaticallyBackgroundSync(context: Context, userId: String, serverTimeout: Long = 0, delay: Long = 30_000) {
            val data = WorkerParamsFactory.toData(Params(userId, serverTimeout, true))
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInputData(data)
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, delay, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork("BG_SYNCP", ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun stopAnyBackgroundSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("BG_SYNCP")
        }
    }

}