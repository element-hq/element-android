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
package im.vector.app.core.services

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import im.vector.app.R
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.features.notifications.NotificationUtils
import org.matrix.android.sdk.internal.session.sync.job.SyncService
import timber.log.Timber

class VectorSyncService : SyncService() {

    companion object {

        fun newOneShotIntent(context: Context, sessionId: String, timeoutSeconds: Int): Intent {
            return Intent(context, VectorSyncService::class.java).also {
                it.putExtra(EXTRA_SESSION_ID, sessionId)
                it.putExtra(EXTRA_TIMEOUT_SECONDS, timeoutSeconds)
                it.putExtra(EXTRA_PERIODIC, false)
            }
        }

        fun newPeriodicIntent(context: Context, sessionId: String, timeoutSeconds: Int, delayInSeconds: Int): Intent {
            return Intent(context, VectorSyncService::class.java).also {
                it.putExtra(EXTRA_SESSION_ID, sessionId)
                it.putExtra(EXTRA_TIMEOUT_SECONDS, timeoutSeconds)
                it.putExtra(EXTRA_PERIODIC, true)
                it.putExtra(EXTRA_DELAY_SECONDS, delayInSeconds)
            }
        }

        fun newPeriodicNetworkBackIntent(context: Context, sessionId: String, timeoutSeconds: Int, delayInSeconds: Int): Intent {
            return Intent(context, VectorSyncService::class.java).also {
                it.putExtra(EXTRA_SESSION_ID, sessionId)
                it.putExtra(EXTRA_TIMEOUT_SECONDS, timeoutSeconds)
                it.putExtra(EXTRA_PERIODIC, true)
                it.putExtra(EXTRA_DELAY_SECONDS, delayInSeconds)
                it.putExtra(EXTRA_NETWORK_BACK_RESTART, true)
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, VectorSyncService::class.java).also {
                it.action = ACTION_STOP
            }
        }
    }

    private lateinit var notificationUtils: NotificationUtils

    override fun onCreate() {
        super.onCreate()
        notificationUtils = vectorComponent().notificationUtils()
    }

    override fun onStart(isInitialSync: Boolean) {
        val notificationSubtitleRes = if (isInitialSync) {
            R.string.notification_initial_sync
        } else {
            R.string.notification_listening_for_events
        }
        val notification = notificationUtils.buildForegroundServiceNotification(notificationSubtitleRes, false)
        startForeground(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
    }

    override fun onRescheduleAsked(sessionId: String, isInitialSync: Boolean, timeout: Int, delay: Int) {
        reschedule(sessionId, timeout, delay)
    }

    override fun onNetworkError(sessionId: String, isInitialSync: Boolean, timeout: Int, delay: Int) {
        Timber.d("## Sync: A network error occured during sync")
        val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<RestartWhenNetworkOn>()
                        .setInputData(Data.Builder()
                                .putString("sessionId", sessionId)
                                .putInt("timeout", timeout)
                                .putInt("delay", delay)
                                .build()
                        )
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()

        Timber.d("## Sync: Schedule a work to restart service when network will be on")
        WorkManager
                .getInstance(applicationContext)
                .enqueue(uploadWorkRequest)
    }

    override fun onDestroy() {
        removeForegroundNotification()
        super.onDestroy()
    }

    private fun removeForegroundNotification() {
        val notificationManager = getSystemService<NotificationManager>()!!
        notificationManager.cancel(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE)
    }

    private fun reschedule(sessionId: String, timeout: Int, delay: Int) {
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 0, newPeriodicIntent(this, sessionId, timeout, delay), 0)
        } else {
            PendingIntent.getService(this, 0, newPeriodicIntent(this, sessionId, timeout, delay), 0)
        }
        val firstMillis = System.currentTimeMillis() + delay * 1000L
        val alarmMgr = getSystemService<AlarmManager>()!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstMillis, pendingIntent)
        } else {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, firstMillis, pendingIntent)
        }
    }

    class RestartWhenNetworkOn(appContext: Context, workerParams: WorkerParameters) :
            Worker(appContext, workerParams) {
        override fun doWork(): Result {
            val sessionId = inputData.getString("sessionId") ?: return Result.failure()
            val timeout = inputData.getInt("timeout", 6)
            val delay = inputData.getInt("delay", 60)

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(applicationContext, 0, newPeriodicNetworkBackIntent(applicationContext, sessionId, timeout, delay), 0)
            } else {
                PendingIntent.getService(applicationContext, 0, newPeriodicNetworkBackIntent(applicationContext, sessionId, timeout, delay), 0)
            }
            val firstMillis = System.currentTimeMillis() + delay * 1000L
            val alarmMgr = getSystemService<AlarmManager>(applicationContext, AlarmManager::class.java)!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstMillis, pendingIntent)
            } else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, firstMillis, pendingIntent)
            }
            // Indicate whether the work finished successfully with the Result
            return Result.success()
        }
    }
}
