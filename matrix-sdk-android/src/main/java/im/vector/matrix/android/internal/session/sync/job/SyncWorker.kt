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
import arrow.core.failure
import arrow.core.recoverWith
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
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


private const val DEFAULT_LONG_POOL_TIMEOUT = 0L

class SyncWorker(context: Context,
                 workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), MatrixKoinComponent {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val timeout: Long = DEFAULT_LONG_POOL_TIMEOUT
    )

    private val syncAPI by inject<SyncAPI>()
    private val filterRepository by inject<FilterRepository>()
    private val syncResponseHandler by inject<SyncResponseHandler>()
    private val sessionParamsStore by inject<SessionParamsStore>()
    private val syncTokenStore by inject<SyncTokenStore>()

    val autoMode = false

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
        }.recoverWith { throwable ->
            // Intercept 401
            if (throwable is Failure.ServerError
                    && throwable.error.code == MatrixError.UNKNOWN_TOKEN) {
                sessionParamsStore.delete()
            }
            Timber.i("Sync work failed $throwable")
            // Transmit the throwable
            throwable.failure()
        }.fold(
                {
                    Timber.i("Sync work failed $it")
                    again()
                    if (it is Failure.NetworkConnection && it.cause is SocketTimeoutException) {
                        // Timeout are not critical
                        Result.Success()
                    } else {
                        Result.Success()
                    }
                },
                {
                    Timber.i("Sync work success next batch ${it.nextBatch}")
                    syncResponseHandler.handleResponse(it, token, false)
                    syncTokenStore.saveToken(it.nextBatch)
                    again()
                    Result.success()
                }
        )

    }

    fun again() {
        if (autoMode) {
            Timber.i("Sync work Again!!")
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInitialDelay(30_000, TimeUnit.MILLISECONDS)
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance().enqueueUniqueWork("BG_SYNCP", ExistingWorkPolicy.APPEND, workRequest)

        }

    }

}