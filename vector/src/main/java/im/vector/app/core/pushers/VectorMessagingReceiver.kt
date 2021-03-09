/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.features.badge.BadgeProxy
import im.vector.app.features.notifications.NotifiableEventResolver
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.settings.VectorPreferences
import org.json.JSONException
import org.matrix.android.sdk.api.MatrixCallback
import org.unifiedpush.android.connector.MessagingReceiver
import timber.log.Timber
import org.unifiedpush.android.connector.MessagingReceiverHandler

/**
 * Class extending FirebaseMessagingService.
 */
val upHandler = object: MessagingReceiverHandler {

    private lateinit var notificationDrawerManager: NotificationDrawerManager
    private lateinit var notifiableEventResolver: NotifiableEventResolver
    private lateinit var pusherManager: PushersManager
    private lateinit var activeSessionHolder: ActiveSessionHolder
    private lateinit var vectorPreferences: VectorPreferences

    fun initVar(context: Context) {
        with(context.vectorComponent()) {
            notificationDrawerManager = notificationDrawerManager()
            notifiableEventResolver = notifiableEventResolver()
            pusherManager = pusherManager()
            activeSessionHolder = activeSessionHolder()
            vectorPreferences = vectorPreferences()
        }
    }

    override fun onMessage(context: Context?, message: String, instance: String) {
        initVar(context!!)
        Timber.i("onMessage received")
        lateinit var data: JSONObject
        lateinit var notification: JSONObject
        lateinit var eventId: String
        try {
            data = JSONObject(message)
            notification = data.getJSONObject("notification")
        } catch (e: JSONException) {
            e.printStackTrace()
            return
        }
        try {
            eventId = notification.getString("event_id")
        } catch (e: JSONException) {
            Timber.i("No event_id on notification")
            notification.put("event_id", "")
            eventId = ""
        }
        try {
            notification.getString("room_id")
        } catch (e: JSONException) {
            Timber.i("No room_id on notification")
            notification.put("room_id", "")
        }
        if (eventId == PushersManager.TEST_EVENT_ID) {
            val intent = Intent(NotificationUtils.PUSH_ACTION)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            return
        }

        if (!vectorPreferences.areNotificationEnabledForDevice()) {
            Timber.i("Notification are disabled for this device")
            return
        }

        mUIHandler.post {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // we are in foreground, let the sync do the things?
                Timber.v("PUSH received in a foreground state, ignore")
            } else {
                onMessageReceivedInternal(context, notification)
            }
        }
    }

    override fun onNewEndpoint(context: Context?, endpoint: String, instance: String) {
        initVar(context!!)
        Timber.i("onNewEndpoint: adding $endpoint")
        UPHelper.storeUpEndpoint(context, endpoint)
        if (vectorPreferences.areNotificationEnabledForDevice() && activeSessionHolder.hasActiveSession()) {
            val gateway = UPHelper.customOrDefaultGateway(context, endpoint)
            UPHelper.storePushGateway(context, gateway)
            UPHelper.storeUpEndpoint(context, endpoint)
            pusherManager.registerPusher(endpoint, gateway)
        }
    }

    override fun onRegistrationFailed(context: Context?, instance: String) {
        Toast.makeText(context, "Registration Failed", Toast.LENGTH_SHORT).show()
    }

    override fun onRegistrationRefused(context: Context?, instance: String) {
        Toast.makeText(context, "Registration Refused", Toast.LENGTH_SHORT).show()
    }

    override fun onUnregistered(context: Context?, instance: String) {
        initVar(context!!)
        pusherManager.unregisterPusher(
                context,
                object : MatrixCallback<Unit> {}
        )
    }

    // UI handler
    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    private fun onMessageReceivedInternal(context: Context, data: JSONObject) {
        try {
            // update the badge counter
            val unreadCount = data.getJSONObject("counts").getInt("unread")
            BadgeProxy.updateBadgeCount(context, unreadCount)

            val session = activeSessionHolder.getSafeActiveSession()

            if (session == null) {
                Timber.w("## Can't sync from push, no current session")
            } else {
                val eventId = data.getString("event_id")
                val roomId = data.getString("room_id")

                if (isEventAlreadyKnown(eventId, roomId)) {
                    Timber.i("Ignoring push, event already known")
                } else {
                    Timber.v("Requesting background sync")
                    session.requireBackgroundSync()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "## onMessageReceivedInternal() failed")
        }
    }

    private fun isEventAlreadyKnown(eventId: String?, roomId: String?): Boolean {
        if (null != eventId && null != roomId) {
            try {
                val session = activeSessionHolder.getSafeActiveSession() ?: return false
                val room = session.getRoom(roomId) ?: return false
                return room.getTimeLineEvent(eventId) != null
            } catch (e: Exception) {
                Timber.e(e, "## isEventAlreadyKnown() : failed to check if the event was already defined")
            }
        }
        return false
    }
}

class VectorMessagingReceiver : MessagingReceiver(upHandler)

