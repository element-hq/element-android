/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.webrtc

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.startForegroundCompat
import im.vector.app.core.services.VectorAndroidService
import im.vector.app.features.notifications.NotificationUtils
import im.vector.lib.core.utils.timer.Clock
import javax.inject.Inject

@AndroidEntryPoint
class ScreenCaptureAndroidService : VectorAndroidService() {

    @Inject lateinit var notificationUtils: NotificationUtils
    @Inject lateinit var clock: Clock
    private val binder = LocalBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showStickyNotification()

        return START_STICKY
    }

    private fun showStickyNotification() {
        val notificationId = clock.epochMillis().toInt()
        val notification = notificationUtils.buildScreenSharingNotification()
        startForegroundCompat(notificationId, notification)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun stopService() {
        stopSelf()
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureAndroidService = this@ScreenCaptureAndroidService
    }
}
