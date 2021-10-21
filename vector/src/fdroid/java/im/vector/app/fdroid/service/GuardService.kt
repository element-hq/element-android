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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.features.notifications.NotificationUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * This no-op foreground service acts as a deterrent for the system
 * to avoid eagerly killing the app process.
 * Keeping the app process alive avoids some OEMs ignoring scheduled WorkManager and AlarmManager tasks
 * when the app is not in the foreground.
 */
@AndroidEntryPoint
class GuardService : Service() {

    @Inject lateinit var notificationUtils: NotificationUtils

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("## Sync: onCreate GuardService")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationSubtitleRes = R.string.notification_listening_for_events
        val notification = notificationUtils.buildForegroundServiceNotification(notificationSubtitleRes, false)
        startForeground(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
        return START_STICKY
    }
}
