/*
 * Copyright 2019 New Vector Ltd
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.BuildConfig
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.network.WifiDetector
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.features.badge.BadgeProxy
import im.vector.app.features.notifications.NotifiableEventResolver
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorDataStore
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.Session
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.MessagingReceiverHandler
import timber.log.Timber
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class UnifiedPushMessage(
        val notification: Notification = Notification()
        )

@JsonClass(generateAdapter = true)
data class Notification(
        @Json(name = "event_id") val eventId: String = "",
        @Json(name = "room_id") val roomId: String = "",
        var unread: Int = 0,
        val counts: Counts = Counts()
        )

@JsonClass(generateAdapter = true)
data class Counts(
        val unread: Int = 0
        )

private val loggerTag = LoggerTag("Push", LoggerTag.SYNC)

/**
 * Injected variables can't be into interfaces.
 * We need to create an interface with a function
 * to initialize these variables.
 */
interface VectorMessagingReceiverHandler : MessagingReceiverHandler {
    fun init(notificationDrawerManager: NotificationDrawerManager,
             notifiableEventResolver: NotifiableEventResolver,
             pusherManager: PushersManager,
             activeSessionHolder: ActiveSessionHolder,
             vectorPreferences: VectorPreferences,
             vectorDataStore: VectorDataStore,
             wifiDetector: WifiDetector
    )
}

/**
 * UnifiedPush handler.
 */
val upHandler = object: VectorMessagingReceiverHandler {
    lateinit var notificationDrawerManager: NotificationDrawerManager
    lateinit var notifiableEventResolver: NotifiableEventResolver
    lateinit var pusherManager: PushersManager
    lateinit var activeSessionHolder: ActiveSessionHolder
    lateinit var vectorPreferences: VectorPreferences
    lateinit var vectorDataStore: VectorDataStore
    lateinit var wifiDetector: WifiDetector

    private val coroutineScope = CoroutineScope(SupervisorJob())

    // UI handler
    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    /**
     * Called to init injected vars
     */
    override fun init(notificationDrawerManager: NotificationDrawerManager,
                      notifiableEventResolver: NotifiableEventResolver,
                      pusherManager: PushersManager,
                      activeSessionHolder: ActiveSessionHolder,
                      vectorPreferences: VectorPreferences,
                      vectorDataStore: VectorDataStore,
                      wifiDetector: WifiDetector) {
        Timber.tag(loggerTag.value).d("Init vars")
        this.notificationDrawerManager = notificationDrawerManager
        this.notifiableEventResolver = notifiableEventResolver
        this.pusherManager = pusherManager
        this.activeSessionHolder = activeSessionHolder
        this.vectorPreferences = vectorPreferences
        this.vectorDataStore = vectorDataStore
        this.wifiDetector = wifiDetector
    }

    /**
     * Called when message is received.
     *
     * @param message the message
     * @param instance connection, for multi-account
     */
    override fun onMessage(context: Context?, message: String, instance: String) {
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.tag(loggerTag.value).d("## onMessage() %s", message)
        } else {
            Timber.tag(loggerTag.value).d("## onMessage() received")
        }

        runBlocking {
            vectorDataStore.incrementPushCounter()
        }

        val moshi: Moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        lateinit var notification: Notification

        if (UPHelper.isEmbeddedDistributor(context!!)) {
            notification = moshi.adapter(Notification::class.java)
                    .fromJson(message) ?: return
        } else {
            val data = moshi.adapter(UnifiedPushMessage::class.java)
                    .fromJson(message) ?: return
            notification = data.notification
            notification.unread = notification.counts.unread
        }

        // Diagnostic Push
        if (notification.eventId == PushersManager.TEST_EVENT_ID) {
            val intent = Intent(NotificationUtils.PUSH_ACTION)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            return
        }

        if (!vectorPreferences.areNotificationEnabledForDevice()) {
            Timber.tag(loggerTag.value).i("Notification are disabled for this device")
            return
        }

        mUIHandler.post {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // we are in foreground, let the sync do the things?
                Timber.tag(loggerTag.value).d("PUSH received in a foreground state, ignore")
            } else {
                onMessageReceivedInternal(context, notification)
            }
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    override fun onNewEndpoint(context: Context?, endpoint: String, instance: String) {
        Timber.tag(loggerTag.value).i("onNewEndpoint: adding $endpoint")
        if (vectorPreferences.areNotificationEnabledForDevice() && activeSessionHolder.hasActiveSession()) {
            val gateway = UPHelper.customOrDefaultGateway(context!!, endpoint)
            if (UPHelper.getUpEndpoint(context) != endpoint
                    || UPHelper.getPushGateway(context) != gateway) {
                UPHelper.storePushGateway(context, gateway)
                UPHelper.storeUpEndpoint(context, endpoint)
                pusherManager.enqueueRegisterPusher(context, endpoint, gateway)
            } else {
                Timber.tag(loggerTag.value).i("onNewEndpoint: skipped")
            }
        }
        if (context == null || !UPHelper.allowBackgroundSync(context)) {
            val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_DISABLED
            vectorPreferences.setFdroidSyncBackgroundMode(mode)
            if (context != null) {
                GuardServiceStarter(vectorPreferences, context).stop()
            }
        }
    }

    override fun onRegistrationFailed(context: Context?, instance: String) {
        Toast.makeText(context, "Push service registration failed", Toast.LENGTH_SHORT).show()
    }

    override fun onRegistrationRefused(context: Context?, instance: String) {
        Toast.makeText(context, "Push service registration refused by server", Toast.LENGTH_LONG).show()
    }

    override fun onUnregistered(context: Context?, instance: String) {
        Timber.tag(loggerTag.value).d("Unifiedpush: Unregistered")
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        runBlocking {
            try {
                pusherManager.unregisterPusher(context!!, UPHelper.getUpEndpoint(context)!!)
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).d("Probably unregistering a non existant pusher")
            }
        }
    }

    /**
     * Internal receive method
     *
     * @param notification Notification containing message data.
     */
    private fun onMessageReceivedInternal(context: Context, notification: Notification) {
        try {
            if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                Timber.tag(loggerTag.value).d("## onMessageReceivedInternal() : $notification")
            } else {
                Timber.tag(loggerTag.value).d("## onMessageReceivedInternal()")
            }

            // update the badge counter
            BadgeProxy.updateBadgeCount(context, notification.unread)

            val session = activeSessionHolder.getSafeActiveSession()

            if (session == null) {
                Timber.tag(loggerTag.value).w("## Can't sync from push, no current session")
            } else {
                if (isEventAlreadyKnown(notification.eventId, notification.roomId)) {
                    Timber.tag(loggerTag.value).d("Ignoring push, event already known")
                } else {
                    // Try to get the Event content faster
                    Timber.tag(loggerTag.value).d("Requesting event in fast lane")
                    getEventFastLane(session, notification.roomId, notification.eventId)

                    Timber.tag(loggerTag.value).d("Requesting background sync")
                    session.requireBackgroundSync()
                }
            }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## onMessageReceivedInternal() failed")
        }
    }

    private fun getEventFastLane(session: Session, roomId: String?, eventId: String?) {
        roomId?.takeIf { it.isNotEmpty() } ?: return
        eventId?.takeIf { it.isNotEmpty() } ?: return

        // If the room is currently displayed, we will not show a notification, so no need to get the Event faster
        if (notificationDrawerManager.shouldIgnoreMessageEventInRoom(roomId)) {
            return
        }

        if (wifiDetector.isConnectedToWifi().not()) {
            Timber.tag(loggerTag.value).d("No WiFi network, do not get Event")
            return
        }

        coroutineScope.launch {
            Timber.tag(loggerTag.value).d("Fast lane: start request")
            val event = tryOrNull { session.getEvent(roomId, eventId) } ?: return@launch

            val resolvedEvent = notifiableEventResolver.resolveInMemoryEvent(session, event, canBeReplaced = true)

            resolvedEvent
                    ?.also { Timber.tag(loggerTag.value).d("Fast lane: notify drawer") }
                    ?.let {
                        notificationDrawerManager.updateEvents { it.onNotifiableEventReceived(resolvedEvent) }
                    }
        }
    }

    // check if the event was not yet received
    // a previous catchup might have already retrieved the notified event
    private fun isEventAlreadyKnown(eventId: String?, roomId: String?): Boolean {
        if (null != eventId && null != roomId) {
            try {
                val session = activeSessionHolder.getSafeActiveSession() ?: return false
                val room = session.getRoom(roomId) ?: return false
                return room.getTimeLineEvent(eventId) != null
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## isEventAlreadyKnown() : failed to check if the event was already defined")
            }
        }
        return false
    }
}

/**
 * Hilt injection happen at super.onReceive().
 * We must implement an intermediate receiver to
 * initialize vars of the handler before
 * super.onReceive().
 */
open class InjectedMessagingReceiver : MessagingReceiver(upHandler) {
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var notifiableEventResolver: NotifiableEventResolver
    @Inject lateinit var pusherManager: PushersManager
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var vectorDataStore: VectorDataStore
    @Inject lateinit var wifiDetector: WifiDetector

    override fun onReceive(context: Context?, intent: Intent?) {
        upHandler.init(notificationDrawerManager,
                notifiableEventResolver,
                pusherManager,
                activeSessionHolder,
                vectorPreferences,
                vectorDataStore,
                wifiDetector)
        super.onReceive(context, intent) // Injection would happen here
    }
}

@AndroidEntryPoint
class VectorMessagingReceiver : InjectedMessagingReceiver()
