/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.push.fcm.worker

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import im.vector.app.core.services.VectorSyncService
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BackgroundSyncWorker(appContext: Context, workerParams: WorkerParameters):
        Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val sessionId = inputData.getString("sessionId") ?: return Result.failure()

        Timber.d("Start VectorSyncService")
        VectorSyncService.newIntent(applicationContext, sessionId).let {
            try {
                ContextCompat.startForegroundService(applicationContext, it)
            } catch (ex: Throwable) {
                // TODO
                Timber.e(ex)
            }
        }

        scheduleWork(applicationContext, sessionId)

        return Result.success()
    }

    companion object {
        // TODO: Make this configurable
        private const val DEFAULT_DELAY_SECONDS = 30L

        fun scheduleWork(context: Context, sessionId: String) {
            scheduleWork(context, sessionId, DEFAULT_DELAY_SECONDS, TimeUnit.SECONDS)
        }

        fun scheduleWork(context: Context, sessionId: String, delay: Long, timeUnit: TimeUnit) {
            Timber.v("## scheduleWork()")
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val inputData = Data.Builder()
                    .putString("sessionId", sessionId)
                    .build()

            val workRequest = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
                    .setInitialDelay(delay, timeUnit)
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .build()

            WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork("backgroundSync", ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun cancelWork(context: Context) {
            Timber.v("## cancelWork()")
            WorkManager
                    .getInstance(context)
                    .cancelUniqueWork("backgroundSync")
        }
    }
}
