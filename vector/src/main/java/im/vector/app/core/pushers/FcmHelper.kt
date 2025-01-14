/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers

import im.vector.app.core.di.ActiveSessionHolder

interface FcmHelper {
    fun isFirebaseAvailable(): Boolean

    /**
     * Retrieves the FCM registration token.
     *
     * @return the FCM token or null if not received from FCM.
     */
    fun getFcmToken(): String?

    /**
     * Store FCM token to the SharedPrefs.
     *
     * @param token the token to store.
     */
    fun storeFcmToken(token: String?)

    /**
     * onNewToken may not be called on application upgrade, so ensure my shared pref is set.
     *
     * @param pushersManager the instance to register the pusher on.
     * @param registerPusher whether the pusher should be registered.
     */
    fun ensureFcmTokenIsRetrieved(pushersManager: PushersManager, registerPusher: Boolean)

    fun onEnterForeground(activeSessionHolder: ActiveSessionHolder)

    fun onEnterBackground(activeSessionHolder: ActiveSessionHolder)
}
