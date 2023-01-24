/*
 * Copyright (c) 2022 New Vector Ltd
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
