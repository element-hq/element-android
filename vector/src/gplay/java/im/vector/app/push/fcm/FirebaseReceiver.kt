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

package im.vector.app.push.fcm

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.PushersManager
import im.vector.app.features.settings.VectorPreferences
import org.json.JSONObject
import org.unifiedpush.android.connector.ACTION_MESSAGE
import org.unifiedpush.android.connector.EXTRA_BYTES_MESSAGE
import org.unifiedpush.android.connector.EXTRA_TOKEN
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseReceiver : FirebaseMessagingService() {
    @Inject lateinit var fcmHelper: FcmHelper
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var pushersManager: PushersManager

    override fun onNewToken(token: String) {
        Timber.d("New Firebase token")
        fcmHelper.storeFcmToken(token)
        if (vectorPreferences.areNotificationEnabledForDevice() && activeSessionHolder.hasActiveSession()) {
            pushersManager.enqueueRegisterPusher(token, getString(R.string.default_push_gateway_http_url))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("New Firebase message")
        val intent = Intent()
        intent.action = ACTION_MESSAGE
        intent.setPackage(baseContext.packageName)
        intent.putExtra(EXTRA_BYTES_MESSAGE, JSONObject(message.data as Map<*, *>).toString().toByteArray())
        intent.putExtra(EXTRA_TOKEN, fcmHelper.getFcmToken())
        baseContext.sendBroadcast(intent)
    }
}
