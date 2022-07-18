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
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.services.VectorAndroidService
import im.vector.app.features.location.live.GetLiveLocationShareSummaryUseCase
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocationSharingAndroidService : VectorAndroidService(), LocationTracker.Callback {

    @Parcelize
    data class RoomArgs(
            val sessionId: String,
            val roomId: String,
            val durationMillis: Long
    ) : Parcelable

    @Inject lateinit var notificationUtils: NotificationUtils
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var getLiveLocationShareSummaryUseCase: GetLiveLocationShareSummaryUseCase

    private val binder = LocalBinder()

    /**
     * Keep track of a map between beacon event Id starting the live and RoomArgs.
     */
    private val roomArgsMap = mutableMapOf<String, RoomArgs>()
    var callback: Callback? = null
    private val jobs = mutableListOf<Job>()
    private var startInProgress = false

    override fun onCreate() {
        super.onCreate()
        Timber.i("onCreate")

        initLocationTracking()
    }

    private fun initLocationTracking() {
        // Start tracking location
        locationTracker.addCallback(this)
        locationTracker.start()

        launchWithActiveSession { session ->
            val job = locationTracker.locations
                    .onEach(this@LocationSharingAndroidService::onLocationUpdate)
                    .launchIn(session.coroutineScope)
            jobs.add(job)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInProgress = true

        val roomArgs = intent?.getParcelableExtra(EXTRA_ROOM_ARGS) as? RoomArgs

        Timber.i("onStartCommand. sessionId - roomId ${roomArgs?.sessionId} - ${roomArgs?.roomId}")

        if (roomArgs != null) {
            // Show a sticky notification
            val notification = notificationUtils.buildLiveLocationSharingNotification()
            startForeground(roomArgs.roomId.hashCode(), notification)

            // Send beacon info state event
            launchWithActiveSession { session ->
                sendStartingLiveBeaconInfo(session, roomArgs)
            }
        }

        startInProgress = false

        return START_STICKY
    }

    private suspend fun sendStartingLiveBeaconInfo(session: Session, roomArgs: RoomArgs) {
        val updateLiveResult = session
                .getRoom(roomArgs.roomId)
                ?.locationSharingService()
                ?.startLiveLocationShare(
                        timeoutMillis = roomArgs.durationMillis,
                        description = getString(R.string.sent_live_location)
                )

        updateLiveResult
                ?.let { result ->
                    when (result) {
                        is UpdateLiveLocationShareResult.Success -> {
                            addRoomArgs(result.beaconEventId, roomArgs)
                            listenForLiveSummaryChanges(roomArgs.roomId, result.beaconEventId)
                            locationTracker.requestLastKnownLocation()
                        }
                        is UpdateLiveLocationShareResult.Failure -> {
                            callback?.onServiceError(result.error)
                            tryToDestroyMe()
                        }
                    }
                }
                ?: run {
                    Timber.w("sendStartingLiveBeaconInfo error, no received beacon info id")
                    tryToDestroyMe()
                }
    }

    private fun stopSharingLocation(beaconEventId: String) {
        Timber.i("stopSharingLocation for beacon $beaconEventId")
        removeRoomArgs(beaconEventId)
        tryToDestroyMe()
    }

    private fun onLocationUpdate(locationData: LocationData) {
        Timber.i("onLocationUpdate. Uncertainty: ${locationData.uncertainty}")

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
        launchWithActiveSession { session ->
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

    private fun tryToDestroyMe() {
        if (startInProgress.not() && roomArgsMap.isEmpty()) {
            Timber.i("Destroying self, time is up for all rooms")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")
        jobs.forEach { it.cancel() }
        jobs.clear()
        locationTracker.removeCallback(this)
    }

    private fun addRoomArgs(beaconEventId: String, roomArgs: RoomArgs) {
        Timber.i("adding roomArgs for beaconEventId: $beaconEventId")
        roomArgsMap[beaconEventId] = roomArgs
    }

    private fun removeRoomArgs(beaconEventId: String) {
        Timber.i("removing roomArgs for beaconEventId: $beaconEventId")
        roomArgsMap.remove(beaconEventId)
    }

    private fun listenForLiveSummaryChanges(roomId: String, beaconEventId: String) {
        launchWithActiveSession { session ->
            val job = getLiveLocationShareSummaryUseCase.execute(roomId, beaconEventId)
                    .distinctUntilChangedBy { it.isActive }
                    .filter { it.isActive == false }
                    .onEach { stopSharingLocation(beaconEventId) }
                    .launchIn(session.coroutineScope)
            jobs.add(job)
        }
    }

    private fun launchWithActiveSession(block: suspend CoroutineScope.(Session) -> Unit) =
            activeSessionHolder
                    .getSafeActiveSession()
                    ?.let { session ->
                        session.coroutineScope.launch(
                                block = { block(session) }
                        )
                    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationSharingAndroidService = this@LocationSharingAndroidService
    }

    interface Callback {
        fun onServiceError(error: Throwable)
    }

    companion object {
        const val EXTRA_ROOM_ARGS = "EXTRA_ROOM_ARGS"
    }
}
