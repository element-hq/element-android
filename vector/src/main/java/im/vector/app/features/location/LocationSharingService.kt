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
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
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

    private val binder = LocalBinder()

    /**
     * Keep track of a map between beacon event Id starting the live and RoomArgs.
     */
    private val roomArgsMap = mutableMapOf<String, RoomArgs>()
    private val timers = mutableListOf<Timer>()
    var callback: Callback? = null

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
            launchInIO { session ->
                sendStartingLiveBeaconInfo(session, roomArgs)
            }
        }

        return START_STICKY
    }

    private suspend fun sendStartingLiveBeaconInfo(session: Session, roomArgs: RoomArgs) {
        val updateLiveResult = session
                .getRoom(roomArgs.roomId)
                ?.locationSharingService()
                ?.startLiveLocationShare(timeoutMillis = roomArgs.durationMillis)

        updateLiveResult
                ?.let { result ->
                    when (result) {
                        is UpdateLiveLocationShareResult.Success -> {
                            addRoomArgs(result.beaconEventId, roomArgs)
                            locationTracker.requestLastKnownLocation()
                        }
                        is UpdateLiveLocationShareResult.Failure -> {
                            callback?.onServiceError(result.error)
                            tryToDestroyMe()
                        }
                    }
                }
                ?: run {
                    Timber.w("### LocationSharingService.sendStartingLiveBeaconInfo error, no received beacon info id")
                    tryToDestroyMe()
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
        removeRoomArgs(roomId)
        tryToDestroyMe()
    }

    @Synchronized
    override fun onLocationUpdate(locationData: LocationData) {
        Timber.i("### LocationSharingService.onLocationUpdate. Uncertainty: ${locationData.uncertainty}")

        // Emit location update to all rooms in which live location sharing is active
        roomArgsMap.forEach { item ->
            sendLiveLocation(item.value.roomId, item.key, locationData)
        }
    }

    private fun sendLiveLocation(
            roomId: String,
            beaconInfoEventId: String,
            locationData: LocationData
    ) {
        launchInIO { session ->
            session.getRoom(roomId)
                    ?.locationSharingService()
                    ?.sendLiveLocation(
                            beaconInfoEventId = beaconInfoEventId,
                            latitude = locationData.latitude,
                            longitude = locationData.longitude,
                            uncertainty = locationData.uncertainty
                    )
        }
    }

    override fun onNoLocationProviderAvailable() {
        stopForeground(true)
        stopSelf()
    }

    @Synchronized
    private fun tryToDestroyMe() {
        if (roomArgsMap.isEmpty()) {
            Timber.i("### LocationSharingService. Destroying self, time is up for all rooms")
            destroyMe()
        }
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

    @Synchronized
    private fun addRoomArgs(beaconEventId: String, roomArgs: RoomArgs) {
        roomArgsMap[beaconEventId] = roomArgs
    }

    @Synchronized
    private fun removeRoomArgs(roomId: String) {
        val beaconIds = roomArgsMap
                .filter { it.value.roomId == roomId }
                .map { it.key }
        beaconIds.forEach { roomArgsMap.remove(it) }
    }

    private fun launchInIO(block: suspend CoroutineScope.(Session) -> Unit) =
            activeSessionHolder
                    .getSafeActiveSession()
                    ?.let { session ->
                        session.coroutineScope.launch(
                                context = session.coroutineDispatchers.io,
                                block = { block(session) }
                        )
                    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationSharingService = this@LocationSharingService
    }

    interface Callback {
        fun onServiceError(error: Throwable)
    }

    companion object {
        const val EXTRA_ROOM_ARGS = "EXTRA_ROOM_ARGS"
    }
}
