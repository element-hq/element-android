/*
 * Copyright (c) 2021 New Vector Ltd
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
package im.vector.app.fdroid.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.content.getSystemService
import timber.log.Timber
import im.vector.app.R
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.features.notifications.NotificationUtils
import java.util.Timer
import java.util.TimerTask
import im.vector.app.core.services.VectorSyncService
import androidx.core.content.ContextCompat
import org.matrix.android.sdk.internal.session.sync.job.SyncService
import android.app.AlarmManager
import android.os.Build
import android.app.PendingIntent
import im.vector.app.features.settings.BackgroundSyncMode

/**
 */
class GuardService : Service() {

    private var timer = Timer()
    private var sessionId: String? = null
    private var running: Boolean = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("## Sync: onStartCommand GuardService running:$running")
        if (running) {
            if (intent != null) {
                val lifeLine = intent.getBooleanExtra(EXTRA_LIFELINE, false)
                if (lifeLine) {
                    // called from lifeLine?
                    scheduleLifeLine()
                    return START_STICKY
                }
            }
        }
        try {
            timer.cancel()
        } catch (ex: Exception) {
        }
        val notificationSubtitleRes = R.string.notification_listening_for_events
        val notification = notificationUtils.buildForegroundServiceNotification(notificationSubtitleRes, false)
        startForeground(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE + 1, notification)
        try {
            val sharedPref = getSharedPreferences(PREF_NAME_SESSION_ID, 0)
            var delayInSeconds = BackgroundSyncMode.DEFAULT_SYNC_DELAY_SECONDS
            if (intent != null) {
                sessionId = intent.getStringExtra(SyncService.EXTRA_SESSION_ID)
                delayInSeconds = intent.getIntExtra(SyncService.EXTRA_DELAY_SECONDS, BackgroundSyncMode.DEFAULT_SYNC_DELAY_SECONDS)
                val lifeLine = intent.getBooleanExtra(EXTRA_LIFELINE, false)
                if (lifeLine) {
                    Timber.i("## Sync: GuardService restarted by lifeLine")
                }
                if (sessionId.isNullOrEmpty()) {
                    Timber.i("## Sync: GuardService getting sessionId from sharedPreferences")
                    sessionId = sharedPref.getString(PREF_NAME_SESSION_ID, null)
                } else {
                    Timber.i("## Sync: GuardService saving sessionId to sharedPreferences")
                    val editor = sharedPref.edit()
                    editor.putString(PREF_NAME_SESSION_ID, sessionId)
                    editor.apply()
                }
            } else {
                Timber.i("## Sync: GuardService intent is null in GuardService, getting sessionId from sharedPreferences")
                sessionId = sharedPref.getString(PREF_NAME_SESSION_ID, null)
            }
            timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (sessionId.isNullOrEmpty()) {
                        Timber.i("## Sync: timer still alive GuardService sessionId:nullOrEmpty")
                    } else {
                        Timber.i("## Sync: timer still alive GuardService sessionId:$sessionId")
                        try {
                            val syncIntent = Intent(applicationContext, VectorSyncService::class.java)
                            syncIntent.putExtra(SyncService.EXTRA_SESSION_ID, sessionId)
                            syncIntent.putExtra(SyncService.EXTRA_PERIODIC, true)
                            ContextCompat.startForegroundService(applicationContext, syncIntent)
                        } catch (ex: Throwable) {
                        }
                    }
                }
            }, delayInSeconds * 1000L, delayInSeconds * 1000L)
        } catch (ex: Exception) {
            Timber.e("## Sync: error in GuardService when creating timer")
        }
        if (!running) {
            scheduleLifeLine()
        }
        running = true
        return START_STICKY
    }

    private lateinit var notificationUtils: NotificationUtils

    override fun onCreate() {
        Timber.i("## Sync: onCreate GuardService")
        super.onCreate()
        notificationUtils = vectorComponent().notificationUtils()
    }

    override fun onDestroy() {
        Timber.i("## Sync: onDestroy GuardService")
        try {
            timer.cancel()
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun scheduleLifeLine() {
        Timber.d("## Sync: GuardService scheduleLifeLine")
        val intent = Intent(applicationContext, GuardService::class.java)
        intent.putExtra(EXTRA_LIFELINE, true)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 0, intent, 0)
        } else {
            PendingIntent.getService(this, 0, intent, 0)
        }
        val firstMillis = System.currentTimeMillis() + 60 * 1000L
        val alarmMgr = getSystemService<AlarmManager>()!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstMillis, pendingIntent)
        } else {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, firstMillis, pendingIntent)
        }
    }

    companion object {
        const val PREF_NAME_SESSION_ID = "GuardServiceLastActiveSessionId"
        const val EXTRA_LIFELINE = "GuardServiceLifeLine"
    }
}
