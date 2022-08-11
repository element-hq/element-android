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
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.PushersManager
import timber.log.Timber
import javax.inject.Inject

/**
 * This class store the FCM token in SharedPrefs and ensure this token is retrieved.
 * It has an alter ego in the fdroid variant.
 */
class GoogleFcmHelper @Inject constructor(
        context: Context,
) : FcmHelper {
    companion object {
        private const val PREFS_KEY_FCM_TOKEN = "FCM_TOKEN"
    }

    private val sharedPrefs = DefaultSharedPreferences.getInstance(context)

    override fun isFirebaseAvailable(): Boolean = true

    override fun getFcmToken(): String? {
        return sharedPrefs.getString(PREFS_KEY_FCM_TOKEN, null)
    }

    override fun storeFcmToken(token: String?) {
        // TODO Store in realm
        sharedPrefs.edit {
            putString(PREFS_KEY_FCM_TOKEN, token)
        }
    }

    override fun ensureFcmTokenIsRetrieved(activity: Activity, pushersManager: PushersManager, registerPusher: Boolean) {
        //        if (TextUtils.isEmpty(getFcmToken(activity))) {
        // 'app should always check the device for a compatible Google Play services APK before accessing Google Play services features'
        if (checkPlayServices(activity)) {
            try {
                FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->
                            storeFcmToken(token)
                            if (registerPusher) {
                                pushersManager.enqueueRegisterPusherWithFcmKey(token)
                            }
                        }
                        .addOnFailureListener { e ->
                            Timber.e(e, "## ensureFcmTokenIsRetrieved() : failed")
                        }
            } catch (e: Throwable) {
                Timber.e(e, "## ensureFcmTokenIsRetrieved() : failed")
            }
        } else {
            Toast.makeText(activity, R.string.no_valid_google_play_services_apk, Toast.LENGTH_SHORT).show()
            Timber.e("No valid Google Play Services found. Cannot use FCM.")
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private fun checkPlayServices(context: Context): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    override fun onEnterForeground(activeSessionHolder: ActiveSessionHolder) {
        // No op
    }

    override fun onEnterBackground(activeSessionHolder: ActiveSessionHolder) {
        // No op
    }
}
