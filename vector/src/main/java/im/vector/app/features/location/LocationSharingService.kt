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

package im.vector.app.features.location

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcelable
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.services.VectorService
import im.vector.app.core.time.Clock
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@AndroidEntryPoint
class LocationSharingService : VectorService(), LocationTracker.Callback {

    @Parcelize
    data class RoomArgs(
            val sessionId: String,
            val roomId: String,
            val durationMillis: Long
    ) : Parcelable

    @Inject lateinit var notificationUtils: NotificationUtils
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var clock: Clock

    private val binder = LocalBinder()

    /**
     * Keep track of a map between beacon event Id starting the live and RoomArgs.
     */
    private var roomArgsMap = mutableMapOf<String, RoomArgs>()
    private var timers = mutableListOf<Timer>()

    override fun onCreate() {
        super.onCreate()
        Timber.i("### LocationSharingService.onCreate")

        // Start tracking location
        locationTracker.addCallback(this)
        locationTracker.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val roomArgs = intent?.getParcelableExtra(EXTRA_ROOM_ARGS) as? RoomArgs

        Timber.i("### LocationSharingService.onStartCommand. sessionId - roomId ${roomArgs?.sessionId} - ${roomArgs?.roomId}")

        if (roomArgs != null) {
            // Show a sticky notification
            val notification = notificationUtils.buildLiveLocationSharingNotification()
            startForeground(roomArgs.roomId.hashCode(), notification)

            // Schedule a timer to stop sharing
            scheduleTimer(roomArgs.roomId, roomArgs.durationMillis)

            // Send beacon info state event
            activeSessionHolder
                    .getSafeActiveSession()
                    ?.let { session ->
                        session.coroutineScope.launch(session.coroutineDispatchers.io) {
                            sendStartingLiveBeaconInfo(session, roomArgs)
                        }
                    }
        }

        return START_STICKY
    }

    private suspend fun sendStartingLiveBeaconInfo(session: Session, roomArgs: RoomArgs) {
        val beaconContent = MessageBeaconInfoContent(
                timeout = roomArgs.durationMillis,
                isLive = true,
                unstableTimestampMillis = clock.epochMillis()
        ).toContent()

        val stateKey = session.myUserId
        val beaconEventId = session
                .getRoom(roomArgs.roomId)
                ?.stateService()
                ?.sendStateEvent(
                        eventType = EventType.STATE_ROOM_BEACON_INFO.first(),
                        stateKey = stateKey,
                        body = beaconContent
                )

        beaconEventId
                ?.takeUnless { it.isEmpty() }
                ?.let {
                    roomArgsMap[it] = roomArgs
                    locationTracker.requestLastKnownLocation()
                }
                ?: run {
                    Timber.w("### LocationSharingService.sendStartingLiveBeaconInfo error, no received beacon info id")
                }
    }

    private fun scheduleTimer(roomId: String, durationMillis: Long) {
        Timer()
                .apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            stopSharingLocation(roomId)
                            timers.remove(this@apply)
                        }
                    }, durationMillis)
                }
                .also {
                    timers.add(it)
                }
    }

    fun stopSharingLocation(roomId: String) {
        Timber.i("### LocationSharingService.stopSharingLocation for $roomId")

        // Send a new beacon info state by setting live field as false
        sendStoppedBeaconInfo(roomId)

        synchronized(roomArgsMap) {
            val beaconIds = roomArgsMap
                    .filter { it.value.roomId == roomId }
                    .map { it.key }
            beaconIds.forEach { roomArgsMap.remove(it) }

            if (roomArgsMap.isEmpty()) {
                Timber.i("### LocationSharingService. Destroying self, time is up for all rooms")
                destroyMe()
            }
        }
    }

    private fun sendStoppedBeaconInfo(roomId: String) {
        activeSessionHolder
                .getSafeActiveSession()
                ?.let { session ->
                    session.coroutineScope.launch(session.coroutineDispatchers.io) {
                        session.getRoom(roomId)?.stateService()?.stopLiveLocation(session.myUserId)
                    }
                }
    }

    override fun onLocationUpdate(locationData: LocationData) {
        Timber.i("### LocationSharingService.onLocationUpdate. Uncertainty: ${locationData.uncertainty}")

        // Emit location update to all rooms in which live location sharing is active
        roomArgsMap.toMap().forEach { item ->
            sendLiveLocation(item.value.roomId, item.key, locationData)
        }
    }

    private fun sendLiveLocation(
            roomId: String,
            beaconInfoEventId: String,
            locationData: LocationData
    ) {
        val session = activeSessionHolder.getSafeActiveSession()
        val room = session?.getRoom(roomId)
        val userId = session?.myUserId

        if (room == null || userId == null) {
            return
        }

        room.sendService().sendLiveLocation(
                beaconInfoEventId = beaconInfoEventId,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                uncertainty = locationData.uncertainty
        )
    }

    override fun onLocationProviderIsNotAvailable() {
        stopForeground(true)
        stopSelf()
    }

    private fun destroyMe() {
        locationTracker.removeCallback(this)
        timers.forEach { it.cancel() }
        timers.clear()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("### LocationSharingService.onDestroy")
        destroyMe()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationSharingService = this@LocationSharingService
    }

    companion object {
        const val EXTRA_ROOM_ARGS = "EXTRA_ROOM_ARGS"
    }
}
