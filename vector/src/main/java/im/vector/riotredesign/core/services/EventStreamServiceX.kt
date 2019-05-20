/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotredesign.core.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.riotredesign.R
import im.vector.riotredesign.features.notifications.NotifiableEventResolver
import im.vector.riotredesign.features.notifications.NotificationUtils
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * A service in charge of controlling whether the event stream is running or not.
 *
 * It manages messages notifications displayed to the end user.
 */
class EventStreamServiceX : VectorService() {

    /**
     * Managed session (no multi session for Riot)
     */
    private val mSession by inject<Session>()

    /**
     * Set to true to simulate a push immediately when service is destroyed
     */
    private var mSimulatePushImmediate = false

    /**
     * The current state.
     */
    private var serviceState = ServiceState.INIT
        set(newServiceState) {
            Timber.i("setServiceState from $field to $newServiceState")
            field = newServiceState
        }

    /**
     * Push manager
     */
    // TODO private var mPushManager: PushManager? = null

    private var mNotifiableEventResolver: NotifiableEventResolver? = null

    /**
     * Live events listener
     */
    /* TODO
    private val mEventsListener = object : MXEventListener() {
        override fun onBingEvent(event: Event, roomState: RoomState, bingRule: BingRule) {
            if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                Timber.i("%%%%%%%%  MXEventListener: the event $event")
            }

            Timber.i("prepareNotification : " + event.eventId + " in " + roomState.roomId)
            val session = Matrix.getMXSession(applicationContext, event.matrixId)

            // invalid session ?
            // should never happen.
            // But it could be triggered because of multi accounts management.
            // The dedicated account is removing but some pushes are still received.
            if (null == session || !session.isAlive) {
                Timber.i("prepareNotification : don't bing - no session")
                return
            }

            if (EventType.CALL_INVITE == event.type) {
                handleCallInviteEvent(event)
                return
            }


            val notifiableEvent = mNotifiableEventResolver!!.resolveEvent(event, roomState, bingRule, session)
            if (notifiableEvent != null) {
                VectorApp.getInstance().notificationDrawerManager.onNotifiableEventReceived(notifiableEvent)
            }
        }

        override fun onLiveEventsChunkProcessed(fromToken: String, toToken: String) {
            Timber.i("%%%%%%%%  MXEventListener: onLiveEventsChunkProcessed[$fromToken->$toToken]")

            VectorApp.getInstance().notificationDrawerManager.refreshNotificationDrawer(OutdatedEventDetector(this@EventStreamServiceX))

            // do not suspend the application if there is some active calls
            if (ServiceState.CATCHUP == serviceState) {
                val hasActiveCalls = mSession?.mCallsManager?.hasActiveCalls() == true

                // if there are some active calls, the catchup should not be stopped.
                // because an user could answer to a call from another device.
                // there will no push because it is his own message.
                // so, the client has no choice to catchup until the ring is shutdown
                if (hasActiveCalls) {
                    Timber.i("onLiveEventsChunkProcessed : Catchup again because there are active calls")
                    catchup(false)
                } else if (ServiceState.CATCHUP == serviceState) {
                    Timber.i("onLiveEventsChunkProcessed : no Active call")
                    CallsManager.getSharedInstance().checkDeadCalls()
                    stop()
                }
            }
        }
    }    */

    /**
     * Service internal state
     */
    private enum class ServiceState {
        // Initial state
        INIT,
        // Service is started for a Catchup. Once the catchup is finished the service will be stopped
        CATCHUP,
        // Service is started, and session is monitored
        STARTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Cancel any previous worker
        cancelAnySimulatedPushSchedule()

        // no intent : restarted by Android
        if (null == intent) {
            // Cannot happen anymore
            Timber.e("onStartCommand : null intent")
            myStopSelf()
            return START_NOT_STICKY
        }

        val action = intent.action

        Timber.i("onStartCommand with action : $action (current state $serviceState)")

        // Manage foreground notification
        when (action) {
            ACTION_BOOT_COMPLETE,
            ACTION_APPLICATION_UPGRADE,
            ACTION_SIMULATED_PUSH_RECEIVED -> {
                // Display foreground notification
                Timber.i("startForeground")
                val notification = NotificationUtils.buildForegroundServiceNotification(this, R.string.notification_sync_in_progress)
                startForeground(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
            }
            ACTION_GO_TO_FOREGROUND -> {
                // Stop foreground notification display
                Timber.i("stopForeground")
                stopForeground(true)
            }
        }

        if (null == mSession) {
            Timber.e("onStartCommand : no sessions")
            myStopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_START,
            ACTION_GO_TO_FOREGROUND ->
                when (serviceState) {
                    ServiceState.INIT ->
                        start(false)
                    ServiceState.CATCHUP ->
                        // A push has been received before, just change state, to avoid stopping the service when catchup is over
                        serviceState = ServiceState.STARTED
                    ServiceState.STARTED -> {
                        // Nothing to do
                    }
                }
            ACTION_STOP,
            ACTION_GO_TO_BACKGROUND,
            ACTION_LOGOUT ->
                stop()
            ACTION_PUSH_RECEIVED,
            ACTION_SIMULATED_PUSH_RECEIVED ->
                when (serviceState) {
                    ServiceState.INIT ->
                        start(true)
                    ServiceState.CATCHUP ->
                        catchup(true)
                    ServiceState.STARTED ->
                        // Nothing to do
                        Unit
                }
            ACTION_PUSH_UPDATE -> pushStatusUpdate()
            ACTION_BOOT_COMPLETE -> {
                // No FCM only
                mSimulatePushImmediate = true
                stop()
            }
            ACTION_APPLICATION_UPGRADE -> {
                // FDroid only
                catchup(true)
            }
            else -> {
                // Should not happen
            }
        }

        // We don't want the service to be restarted automatically by the System
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Schedule worker?
        scheduleSimulatedPushIfNeeded()
    }

    /**
     * Tell the WorkManager to cancel any schedule of push simulation
     */
    private fun cancelAnySimulatedPushSchedule() {
        WorkManager.getInstance().cancelAllWorkByTag(PUSH_SIMULATOR_REQUEST_TAG)
    }

    /**
     * Configure the WorkManager to schedule a simulated push, if necessary
     */
    private fun scheduleSimulatedPushIfNeeded() {
        if (shouldISimulatePush()) {
            val delay = if (mSimulatePushImmediate) 0 else 60_000 // TODO mPushManager?.backgroundSyncDelay ?: let { 60_000 }
            Timber.i("## service is schedule to restart in $delay millis, if network is connected")

            val pushSimulatorRequest = OneTimeWorkRequestBuilder<PushSimulatorWorker>()
                    .setInitialDelay(delay.toLong(), TimeUnit.MILLISECONDS)
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .addTag(PUSH_SIMULATOR_REQUEST_TAG)
                    .build()

            WorkManager.getInstance().let {
                // Cancel any previous worker
                it.cancelAllWorkByTag(PUSH_SIMULATOR_REQUEST_TAG)
                it.enqueue(pushSimulatorRequest)
            }
        }
    }

    /**
     * Start the even stream.
     *
     * @param session the session
     */
    private fun startEventStream(session: Session) {
        /* TODO
        // resume if it was only suspended
        if (null != session.currentSyncToken) {
            session.resumeEventStream()
        } else {
            session.startEventStream(store?.eventStreamToken)
        }
        */
    }

    /**
     * Monitor the provided session.
     *
     * @param session the session
     */
    private fun monitorSession(session: Session) {
        /* TODO
        session.dataHandler.addListener(mEventsListener)
        CallsManager.getSharedInstance().addSession(session)

        val store = session.dataHandler.store

        // the store is ready (no data loading in progress...)
        if (store!!.isReady) {
            startEventStream(session, store)
        } else {
            // wait that the store is ready  before starting the events stream
            store.addMXStoreListener(object : MXStoreListener() {
                override fun onStoreReady(accountId: String) {
                    startEventStream(session, store)

                    store.removeMXStoreListener(this)
                }

                override fun onStoreCorrupted(accountId: String, description: String) {
                    // start a new initial sync
                    if (null == store.eventStreamToken) {
                        startEventStream(session, store)
                    } else {
                        // the data are out of sync
                        Matrix.getInstance(applicationContext)!!.reloadSessions(applicationContext)
                    }

                    store.removeMXStoreListener(this)
                }

                override fun onStoreOOM(accountId: String, description: String) {
                    val uiHandler = Handler(mainLooper)

                    uiHandler.post {
                        Toast.makeText(applicationContext, "$accountId : $description", Toast.LENGTH_LONG).show()
                        Matrix.getInstance(applicationContext)!!.reloadSessions(applicationContext)
                    }
                }
            })

            store.open()
        }
        */
    }

    /**
     * internal start.
     */
    private fun start(forPush: Boolean) {
        val applicationContext = applicationContext
        // TODO mPushManager = Matrix.getInstance(applicationContext)!!.pushManager
        mNotifiableEventResolver = NotifiableEventResolver(applicationContext)

        monitorSession(mSession!!)

        serviceState = if (forPush) {
            ServiceState.CATCHUP
        } else {
            ServiceState.STARTED
        }
    }

    /**
     * internal stop.
     */
    private fun stop() {
        Timber.i("## stop(): the service is stopped")

        /* TODO
        if (null != mSession && mSession!!.isAlive) {
            mSession!!.stopEventStream()
            mSession!!.dataHandler.removeListener(mEventsListener)
            CallsManager.getSharedInstance().removeSession(mSession)
        }
        mSession = null
        */

        // Stop the service
        myStopSelf()
    }

    /**
     * internal catchup method.
     *
     * @param checkState true to check if the current state allow to perform a catchup
     */
    private fun catchup(checkState: Boolean) {
        var canCatchup = true

        if (!checkState) {
            Timber.i("catchup  without checking serviceState ")
        } else {
            Timber.i("catchup with serviceState " + serviceState + " CurrentActivity ") // TODO + VectorApp.getCurrentActivity())

            /* TODO
            // the catchup should only be done
            // 1- the serviceState is in catchup : the event stream might have gone to sleep between two catchups
            // 2- the thread is suspended
            // 3- the application has been launched by a push so there is no displayed activity
            canCatchup = (serviceState == ServiceState.CATCHUP
                    //|| (serviceState == ServiceState.PAUSE)
                    || ServiceState.STARTED == serviceState && null == VectorApp.getCurrentActivity())
                    */
        }

        if (canCatchup) {
            if (mSession != null) {
                // TODO mSession!!.catchupEventStream()
            } else {
                Timber.i("catchup no session")
            }

            serviceState = ServiceState.CATCHUP
        } else {
            Timber.i("No catchup is triggered because there is already a running event thread")
        }
    }

    /**
     * The push status has been updated (i.e disabled or enabled).
     * TODO Useless now?
     */
    private fun pushStatusUpdate() {
        Timber.i("## pushStatusUpdate")
    }

    /* ==========================================================================================
     * Push simulator
     * ========================================================================================== */

    /**
     * @return true if the FCM is disable or not setup, user allowed background sync, user wants notification
     */
    private fun shouldISimulatePush(): Boolean {
        return false

        /* TODO

        if (Matrix.getInstance(applicationContext)?.defaultSession == null) {
            Timber.i("## shouldISimulatePush: NO: no session")

            return false
        }

        mPushManager?.let { pushManager ->
            if (pushManager.useFcm()
                    && !TextUtils.isEmpty(pushManager.currentRegistrationToken)
                    && pushManager.isServerRegistered) {
                // FCM is ok
                Timber.i("## shouldISimulatePush: NO: FCM is up")
                return false
            }

            if (!pushManager.isBackgroundSyncAllowed) {
                // User has disabled background sync
                Timber.i("## shouldISimulatePush: NO: background sync not allowed")
                return false
            }

            if (!pushManager.areDeviceNotificationsAllowed()) {
                // User does not want notifications
                Timber.i("## shouldISimulatePush: NO: user does not want notification")
                return false
            }
        }

        // Lets simulate push
        Timber.i("## shouldISimulatePush: YES")
        return true
        */
    }


    //================================================================================
    // Call management
    //================================================================================

    private fun handleCallInviteEvent(event: Event) {
        /*
        TODO
        val session = Matrix.getMXSession(applicationContext, event.matrixId)

        // invalid session ?
        // should never happen.
        // But it could be triggered because of multi accounts management.
        // The dedicated account is removing but some pushes are still received.
        if (null == session || !session.isAlive) {
            Timber.v("prepareCallNotification : don't bing - no session")
            return
        }

        val room: Room? = session.dataHandler.getRoom(event.roomId)

        // invalid room ?
        if (null == room) {
            Timber.i("prepareCallNotification : don't bing - the room does not exist")
            return
        }

        var callId: String? = null
        var isVideo = false

        try {
            callId = event.contentAsJsonObject?.get("call_id")?.asString

            // Check if it is a video call
            val offer = event.contentAsJsonObject?.get("offer")?.asJsonObject
            val sdp = offer?.get("sdp")
            val sdpValue = sdp?.asString

            isVideo = sdpValue?.contains("m=video") == true
        } catch (e: Exception) {
            Timber.e("prepareNotification : getContentAsJsonObject " + e.message, e)
        }

        if (!TextUtils.isEmpty(callId)) {
            CallService.onIncomingCall(this,
                    isVideo,
                    room.getRoomDisplayName(this),
                    room.roomId,
                    session.myUserId!!,
                    callId!!)
        }
         */
    }

    companion object {
        private const val PUSH_SIMULATOR_REQUEST_TAG = "PUSH_SIMULATOR_REQUEST_TAG"

        private const val ACTION_START = "im.vector.riotredesign.core.services.EventStreamServiceX.START"
        private const val ACTION_LOGOUT = "im.vector.riotredesign.core.services.EventStreamServiceX.LOGOUT"
        private const val ACTION_GO_TO_FOREGROUND = "im.vector.riotredesign.core.services.EventStreamServiceX.GO_TO_FOREGROUND"
        private const val ACTION_GO_TO_BACKGROUND = "im.vector.riotredesign.core.services.EventStreamServiceX.GO_TO_BACKGROUND"
        private const val ACTION_PUSH_UPDATE = "im.vector.riotredesign.core.services.EventStreamServiceX.PUSH_UPDATE"
        private const val ACTION_PUSH_RECEIVED = "im.vector.riotredesign.core.services.EventStreamServiceX.PUSH_RECEIVED"
        private const val ACTION_SIMULATED_PUSH_RECEIVED = "im.vector.riotredesign.core.services.EventStreamServiceX.SIMULATED_PUSH_RECEIVED"
        private const val ACTION_STOP = "im.vector.riotredesign.core.services.EventStreamServiceX.STOP"
        private const val ACTION_BOOT_COMPLETE = "im.vector.riotredesign.core.services.EventStreamServiceX.BOOT_COMPLETE"
        private const val ACTION_APPLICATION_UPGRADE = "im.vector.riotredesign.core.services.EventStreamServiceX.APPLICATION_UPGRADE"

        /* ==========================================================================================
         * Events sent to the service
         * ========================================================================================== */

        fun onApplicationStarted(context: Context) {
            sendAction(context, ACTION_START)
        }

        fun onLogout(context: Context) {
            sendAction(context, ACTION_LOGOUT)
        }

        fun onAppGoingToForeground(context: Context) {
            sendAction(context, ACTION_GO_TO_FOREGROUND)
        }

        fun onAppGoingToBackground(context: Context) {
            sendAction(context, ACTION_GO_TO_BACKGROUND)
        }

        fun onPushUpdate(context: Context) {
            sendAction(context, ACTION_PUSH_UPDATE)
        }

        fun onPushReceived(context: Context) {
            sendAction(context, ACTION_PUSH_RECEIVED)
        }

        fun onSimulatedPushReceived(context: Context) {
            sendAction(context, ACTION_SIMULATED_PUSH_RECEIVED, true)
        }

        fun onApplicationStopped(context: Context) {
            sendAction(context, ACTION_STOP)
        }

        fun onBootComplete(context: Context) {
            sendAction(context, ACTION_BOOT_COMPLETE, true)
        }

        fun onApplicationUpgrade(context: Context) {
            sendAction(context, ACTION_APPLICATION_UPGRADE, true)
        }

        private fun sendAction(context: Context, action: String, foreground: Boolean = false) {
            Timber.i("sendAction $action")

            val intent = Intent(context, EventStreamServiceX::class.java)
            intent.action = action

            if (foreground) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
