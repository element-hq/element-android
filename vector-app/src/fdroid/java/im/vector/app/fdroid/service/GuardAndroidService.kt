/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.fdroid.service

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.services.VectorAndroidService
import im.vector.app.features.notifications.NotificationUtils
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

/**
 * This no-op foreground service acts as a deterrent to the system eagerly killing the app process.
 *
 * Keeping the app process alive avoids some OEMs ignoring scheduled WorkManager and AlarmManager tasks
 * when the app is not in the foreground.
 */
@AndroidEntryPoint
class GuardAndroidService : VectorAndroidService() {

    @Inject lateinit var notificationUtils: NotificationUtils

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationSubtitleRes = CommonStrings.notification_listening_for_notifications
        val notification = notificationUtils.buildForegroundServiceNotification(notificationSubtitleRes, false)
        startForeground(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
        return START_STICKY
    }
}
