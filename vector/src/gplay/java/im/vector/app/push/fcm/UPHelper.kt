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
package im.vector.app.push.fcm

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.DefaultSharedPreferences
import im.vector.app.core.pushers.PushersManager
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber

/**
 * This class store the FCM token in SharedPrefs and ensure this token is retrieved.
 * It has an alter ego in the fdroid variant.
 */
object UPHelper {
    private val PREFS_UP_ENDPOINT = "UP_ENDPOINT"

    /**
     * Retrieves the FCM registration token.
     *
     * @return the FCM token or null if not received from FCM
     */
    fun getUpEndpoint(context: Context): String? {
        return DefaultSharedPreferences.getInstance(context).getString(PREFS_UP_ENDPOINT, null)
    }

    /**
     * Store FCM token to the SharedPrefs
     * TODO Store in realm
     *
     * @param context android context
     * @param endpoint   the endpoint to store
     */
    fun storeUpEndpoint(context: Context,
                        endpoint: String?) {
        DefaultSharedPreferences.getInstance(context).edit {
            putString(PREFS_UP_ENDPOINT, endpoint)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onEnterForeground(context: Context, activeSessionHolder: ActiveSessionHolder) {
        // No op
    }

    @Suppress("UNUSED_PARAMETER")
    fun onEnterBackground(context: Context, vectorPreferences: VectorPreferences, activeSessionHolder: ActiveSessionHolder) {
        // TODO FCM fallback
    }
}
