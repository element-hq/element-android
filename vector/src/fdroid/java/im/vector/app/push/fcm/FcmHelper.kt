/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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
@file:Suppress("UNUSED_PARAMETER")

package im.vector.app.push.fcm

import android.app.Activity
import android.content.Context
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.PushersManager
import im.vector.app.fdroid.receiver.AlarmSyncBroadcastReceiver
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber

/**
 * This class has an alter ego in the gplay variant.
 */
object FcmHelper {

    fun isPushSupported(): Boolean = false

    /**
     * Retrieves the FCM registration token.
     *
     * @return the FCM token or null if not received from FCM
     */
    fun getFcmToken(context: Context): String? {
        return null
    }

    /**
     * Store FCM token to the SharedPrefs
     *
     * @param context android context
     * @param token   the token to store
     */
    fun storeFcmToken(context: Context, token: String?) {
        // No op
    }

    /**
     * onNewToken may not be called on application upgrade, so ensure my shared pref is set
     *
     * @param activity the first launch Activity
     */
    fun ensureFcmTokenIsRetrieved(activity: Activity, pushersManager: PushersManager, registerPusher: Boolean) {
        // No op
    }

    fun onEnterForeground(context: Context, activeSessionHolder: ActiveSessionHolder) {
        // try to stop all regardless of background mode
        activeSessionHolder.getSafeActiveSession()?.stopAnyBackgroundSync()
        AlarmSyncBroadcastReceiver.cancelAlarm(context)
    }

    fun onEnterBackground(context: Context, vectorPreferences: VectorPreferences, activeSessionHolder: ActiveSessionHolder) {
        // We need to use alarm in this mode
        if (vectorPreferences.areNotificationEnabledForDevice() && activeSessionHolder.hasActiveSession()) {
            when (vectorPreferences.getFdroidSyncBackgroundMode()) {
                BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY  -> {
                    // we rely on periodic worker
                    Timber.i("## Sync: Work scheduled to periodically sync in ${vectorPreferences.backgroundSyncDelay()} sec")
                    activeSessionHolder
                            .getSafeActiveSession()
                            ?.startAutomaticBackgroundSync(
                                    vectorPreferences.backgroundSyncTimeOut().toLong(),
                                    vectorPreferences.backgroundSyncDelay().toLong()
                            )
                }
                BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME -> {
                    val currentSession = activeSessionHolder.getActiveSession()
                    AlarmSyncBroadcastReceiver.scheduleAlarm(context, currentSession.sessionId, vectorPreferences.backgroundSyncDelay())
                    Timber.i("## Sync: Alarm scheduled to start syncing")
                }
                BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_DISABLED     -> {
                    // we do nothing
                }
            }
        }
    }
}
