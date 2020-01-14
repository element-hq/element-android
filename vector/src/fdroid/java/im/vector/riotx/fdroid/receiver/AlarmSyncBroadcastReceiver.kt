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

package im.vector.riotx.fdroid.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import im.vector.matrix.android.internal.session.sync.job.SyncService
import im.vector.riotx.core.services.VectorSyncService
import timber.log.Timber

class AlarmSyncBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Acquire a lock to give enough time for the sync :/
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "riotx:fdroidSynclock").apply {
                acquire((10_000).toLong())
            }
        }

        val sessionId = intent.getStringExtra(SyncService.EXTRA_SESSION_ID)
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Timber.d("RestartBroadcastReceiver received intent")
        VectorSyncService.newIntent(context, sessionId).let {
            try {
                ContextCompat.startForegroundService(context, it)
            } catch (ex: Throwable) {
                // TODO
                Timber.e(ex)
            }
        }

        scheduleAlarm(context, sessionId, 30_000L)

        Timber.i("Alarm scheduled to restart service")
    }

    companion object {
        private const val REQUEST_CODE = 0

        fun scheduleAlarm(context: Context, sessionId: String, delay: Long) {
            // Reschedule
            val intent = Intent(context, AlarmSyncBroadcastReceiver::class.java).apply {
                putExtra(SyncService.EXTRA_SESSION_ID, sessionId)
            }
            val pIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val firstMillis = System.currentTimeMillis() + delay
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstMillis, pIntent)
            } else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, firstMillis, pIntent)
            }
        }

        fun cancelAlarm(context: Context) {
            Timber.v("Cancel alarm")
            val intent = Intent(context, AlarmSyncBroadcastReceiver::class.java)
            val pIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmMgr.cancel(pIntent)
        }
    }
}
