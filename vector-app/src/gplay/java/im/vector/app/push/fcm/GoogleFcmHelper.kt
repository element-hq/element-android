/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.push.fcm

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.PushersManager
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * This class store the FCM token in SharedPrefs and ensure this token is retrieved.
 * It has an alter ego in the fdroid variant.
 */
class GoogleFcmHelper @Inject constructor(
        @ApplicationContext private val context: Context,
        @DefaultPreferences private val sharedPrefs: SharedPreferences,
        appScope: CoroutineScope,
        private val coroutineDispatchers: CoroutineDispatchers
) : FcmHelper {

    private val scope = CoroutineScope(appScope.coroutineContext + coroutineDispatchers.io)

    companion object {
        private const val PREFS_KEY_FCM_TOKEN = "FCM_TOKEN"
    }

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

    override fun ensureFcmTokenIsRetrieved(pushersManager: PushersManager, registerPusher: Boolean) {
        // 'app should always check the device for a compatible Google Play services APK before accessing Google Play services features'
        if (checkPlayServices(context)) {
            try {
                FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->
                            storeFcmToken(token)
                            if (registerPusher) {
                                scope.launch {
                                    pushersManager.enqueueRegisterPusherWithFcmKey(token)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Timber.e(e, "## ensureFcmTokenIsRetrieved() : failed")
                        }
            } catch (e: Throwable) {
                Timber.e(e, "## ensureFcmTokenIsRetrieved() : failed")
            }
        } else {
            Toast.makeText(context, CommonStrings.no_valid_google_play_services_apk, Toast.LENGTH_SHORT).show()
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
