/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.fdroid.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.platform.PendingIntentCompat
import im.vector.app.core.services.VectorSyncAndroidService
import im.vector.app.core.time.Clock
import org.matrix.android.sdk.api.session.sync.job.SyncAndroidService
import timber.log.Timber

class AlarmSyncBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("## Sync: AlarmSyncBroadcastReceiver received intent")
        val singletonEntryPoint = context.singletonEntryPoint()
        if (singletonEntryPoint.activeSessionHolder().getSafeActiveSession() == null) {
            Timber.v("No active session, so don't launch sync service.")
            return
        }
        val vectorPreferences = singletonEntryPoint.vectorPreferences()
        val clock = singletonEntryPoint.clock()

        val sessionId = intent.getStringExtra(SyncAndroidService.EXTRA_SESSION_ID) ?: return
        VectorSyncAndroidService.newPeriodicIntent(
                context = context,
                sessionId = sessionId,
                syncTimeoutSeconds = vectorPreferences.backgroundSyncTimeOut(),
                syncDelaySeconds = vectorPreferences.backgroundSyncDelay(),
                isNetworkBack = false
        )
                .let {
                    try {
                        ContextCompat.startForegroundService(context, it)
                    } catch (ex: Throwable) {
                        Timber.i("## Sync: Failed to start service, Alarm scheduled to restart service")
                        scheduleAlarm(context, sessionId, vectorPreferences.backgroundSyncDelay(), clock)
                        Timber.e(ex)
                    }
                }
    }

    companion object {
        private const val REQUEST_CODE = 0

        fun scheduleAlarm(context: Context, sessionId: String, delayInSeconds: Int, clock: Clock) {
            // Reschedule
            Timber.v("## Sync: Scheduling alarm for background sync in $delayInSeconds seconds")
            val intent = Intent(context, AlarmSyncBroadcastReceiver::class.java).apply {
                putExtra(SyncAndroidService.EXTRA_SESSION_ID, sessionId)
                putExtra(SyncAndroidService.EXTRA_PERIODIC, true)
            }
            val pIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
            )
            val firstMillis = clock.epochMillis() + delayInSeconds * 1000L
            val alarmMgr = context.getSystemService<AlarmManager>()!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstMillis, pIntent)
            } else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, firstMillis, pIntent)
            }
        }

        fun cancelAlarm(context: Context) {
            Timber.v("## Sync: Cancel alarm for background sync")
            val intent = Intent(context, AlarmSyncBroadcastReceiver::class.java)
            val pIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
            )
            val alarmMgr = context.getSystemService<AlarmManager>()!!
            alarmMgr.cancel(pIntent)

            // Stop current service to restart
            VectorSyncAndroidService.stopIntent(context).let {
                try {
                    ContextCompat.startForegroundService(context, it)
                } catch (ex: Throwable) {
                    Timber.i("## Sync: Cancel sync")
                }
            }
        }
    }
}
