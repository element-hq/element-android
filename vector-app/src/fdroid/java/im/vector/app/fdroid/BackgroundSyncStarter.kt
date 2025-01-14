/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.fdroid

import android.content.Context
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.fdroid.receiver.AlarmSyncBroadcastReceiver
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.timer.Clock
import timber.log.Timber
import javax.inject.Inject

class BackgroundSyncStarter @Inject constructor(
        private val context: Context,
        private val vectorPreferences: VectorPreferences,
        private val clock: Clock
) {
    fun start(activeSessionHolder: ActiveSessionHolder) {
        if (vectorPreferences.areNotificationEnabledForDevice()) {
            val activeSession = activeSessionHolder.getSafeActiveSession() ?: return
            when (vectorPreferences.getFdroidSyncBackgroundMode()) {
                BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY -> {
                    // we rely on periodic worker
                    Timber.i("## Sync: Work scheduled to periodically sync in ${vectorPreferences.backgroundSyncDelay()}s")
                    activeSession.syncService().startAutomaticBackgroundSync(
                            vectorPreferences.backgroundSyncTimeOut().toLong(),
                            vectorPreferences.backgroundSyncDelay().toLong()
                    )
                }
                BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME -> {
                    // We need to use alarm in this mode
                    AlarmSyncBroadcastReceiver.scheduleAlarm(
                            context,
                            activeSession.sessionId,
                            vectorPreferences.backgroundSyncDelay(),
                            clock
                    )
                    Timber.i("## Sync: Alarm scheduled to start syncing")
                }
                BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_DISABLED -> {
                    // we do nothing
                    Timber.i("## Sync: background sync is disabled")
                }
            }
        }
    }
}
