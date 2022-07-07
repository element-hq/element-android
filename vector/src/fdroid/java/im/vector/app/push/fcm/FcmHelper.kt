/*
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
import im.vector.app.fdroid.BackgroundSyncStarter
import im.vector.app.fdroid.receiver.AlarmSyncBroadcastReceiver
import javax.inject.Inject

/**
 * This class has an alter ego in the gplay variant.
 */
class FcmHelper @Inject constructor(
        private val context: Context,
        private val backgroundSyncStarter: BackgroundSyncStarter,
) {

    fun isFirebaseAvailable(): Boolean = false

    /**
     * Retrieves the FCM registration token.
     *
     * @return the FCM token or null if not received from FCM
     */
    fun getFcmToken(): String? {
        return null
    }

    /**
     * Store FCM token to the SharedPrefs
     *
     * @param token the token to store
     */
    fun storeFcmToken(token: String?) {
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

    fun onEnterForeground(activeSessionHolder: ActiveSessionHolder) {
        // try to stop all regardless of background mode
        activeSessionHolder.getSafeActiveSession()?.syncService()?.stopAnyBackgroundSync()
        AlarmSyncBroadcastReceiver.cancelAlarm(context)
    }

    fun onEnterBackground(activeSessionHolder: ActiveSessionHolder) {
        backgroundSyncStarter.start(activeSessionHolder)
    }
}
