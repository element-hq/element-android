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
package im.vector.riotx.fdroid.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import im.vector.matrix.android.internal.session.sync.job.SyncService
import im.vector.riotx.R
import im.vector.riotx.core.extensions.vectorComponent
import im.vector.riotx.features.notifications.NotificationUtils
import timber.log.Timber

class VectorSyncService : SyncService() {

    private lateinit var notificationUtils: NotificationUtils

    override fun onCreate() {
        super.onCreate()
        notificationUtils = vectorComponent().notificationUtils()
    }

    override fun onDestroy() {
        removeForegroundNotif()
        super.onDestroy()
    }

    private fun removeForegroundNotif() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE)
    }

    /**
     * Service is started only in fdroid mode when no FCM is available
     * Otherwise it is bounded
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("VectorSyncService - onStartCommand ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = notificationUtils.buildForegroundServiceNotification(R.string.notification_listening_for_events, false)
            startForeground(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
