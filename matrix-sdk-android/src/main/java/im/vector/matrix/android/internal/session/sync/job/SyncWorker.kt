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
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterRepository
import im.vector.matrix.android.internal.session.sync.SyncAPI
import im.vector.matrix.android.internal.session.sync.SyncResponseHandler
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit


private const val DEFAULT_LONG_POOL_TIMEOUT = 0L

internal class SyncWorker(context: Context,
                 workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), MatrixKoinComponent {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val timeout: Long = DEFAULT_LONG_POOL_TIMEOUT,
            val automaticallyRetry: Boolean = false
    )

    private val syncAPI by inject<SyncAPI>()
    private val filterRepository by inject<FilterRepository>()
    private val syncResponseHandler by inject<SyncResponseHandler>()
    private val sessionParamsStore by inject<SessionParamsStore>()
    private val syncTokenStore by inject<SyncTokenStore>()


    override suspend fun doWork(): Result {
        Timber.i("Sync work starting")
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: Params()

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
                        sessionParamsStore.delete()
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
        fun requireBackgroundSync(serverTimeout: Long = 0) {
            val data = WorkerParamsFactory.toData(Params(serverTimeout, false))
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInputData(data)
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1_000, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance().enqueueUniqueWork("BG_SYNCP", ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun automaticallyBackgroundSync(serverTimeout: Long = 0, delay: Long = 30_000) {
            val data = WorkerParamsFactory.toData(Params(serverTimeout, true))
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInputData(data)
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, delay, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance().enqueueUniqueWork("BG_SYNCP", ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun stopAnyBackgroundSync() {
            WorkManager.getInstance().cancelUniqueWork("BG_SYNCP")
        }
    }

}