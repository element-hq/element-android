/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
@file:Suppress("UNUSED_PARAMETER")

package im.vector.app.push.fcm

import android.content.Context
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.PushersManager
import im.vector.app.fdroid.BackgroundSyncStarter
import im.vector.app.fdroid.receiver.AlarmSyncBroadcastReceiver
import javax.inject.Inject

/**
 * This class has an alter ego in the gplay variant.
 */
class FdroidFcmHelper @Inject constructor(
        private val context: Context,
        private val backgroundSyncStarter: BackgroundSyncStarter,
) : FcmHelper {

    override fun isFirebaseAvailable(): Boolean = false

    override fun getFcmToken(): String? {
        return null
    }

    override fun storeFcmToken(token: String?) {
        // No op
    }

    override fun ensureFcmTokenIsRetrieved(pushersManager: PushersManager, registerPusher: Boolean) {
        // No op
    }

    override fun onEnterForeground(activeSessionHolder: ActiveSessionHolder) {
        // try to stop all regardless of background mode
        activeSessionHolder.getSafeActiveSessionAsync {
            it?.syncService()?.stopAnyBackgroundSync()
        }
        AlarmSyncBroadcastReceiver.cancelAlarm(context)
    }

    override fun onEnterBackground(activeSessionHolder: ActiveSessionHolder) {
        backgroundSyncStarter.start(activeSessionHolder)
    }
}
