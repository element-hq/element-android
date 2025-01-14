/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.tracking

import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.startForegroundCompat
import im.vector.app.core.services.VectorAndroidService
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.LocationTracker
import im.vector.app.features.location.live.GetLiveLocationShareSummaryUseCase
import im.vector.app.features.redaction.CheckIfEventIsRedactedUseCase
import im.vector.app.features.session.coroutineScope
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    @Inject lateinit var liveLocationNotificationBuilder: LiveLocationNotificationBuilder
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var getLiveLocationShareSummaryUseCase: GetLiveLocationShareSummaryUseCase
    @Inject lateinit var checkIfEventIsRedactedUseCase: CheckIfEventIsRedactedUseCase

    private var binder: LocationSharingAndroidServiceBinder? = null

    private val liveInfoSet = linkedSetOf<LiveInfo>()
    var callback: Callback? = null
    private val jobs = mutableListOf<Job>()
    private var startInProgress = false
    private var foregroundModeStarted = false

    private val _roomIdsOfActiveLives = MutableSharedFlow<Set<String>>(replay = 1)
    val roomIdsOfActiveLives = _roomIdsOfActiveLives.asSharedFlow()

    override fun onCreate() {
        super.onCreate()
        Timber.i("onCreate")
        binder = LocationSharingAndroidServiceBinder().also { it.setup(this) }
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

        val roomArgs = intent?.getParcelableExtraCompat(EXTRA_ROOM_ARGS) as? RoomArgs

        Timber.i("onStartCommand. sessionId - roomId ${roomArgs?.sessionId} - ${roomArgs?.roomId}")

        if (roomArgs != null) {
            // Show a sticky notification
            val notification = liveLocationNotificationBuilder.buildLiveLocationSharingNotification(roomArgs.roomId)
            if (foregroundModeStarted) {
                NotificationManagerCompat.from(this).notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
            } else {
                startForegroundCompat(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
                foregroundModeStarted = true
            }

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
                ?.startLiveLocationShare(roomArgs.durationMillis)

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
        updateNotification()
        tryToDestroyMe()
    }

    private fun updateNotification() {
        if (liveInfoSet.isNotEmpty()) {
            val roomId = liveInfoSet.last().roomArgs.roomId
            val notification = liveLocationNotificationBuilder.buildLiveLocationSharingNotification(roomId)
            NotificationManagerCompat.from(this).notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        }
    }

    private fun onLocationUpdate(locationData: LocationData) {
        Timber.i("onLocationUpdate. Uncertainty: ${locationData.uncertainty}")

        // Emit location update to all rooms in which live location sharing is active
        liveInfoSet.toSet().forEach { liveInfo ->
            sendLiveLocation(liveInfo.roomArgs.roomId, liveInfo.beaconEventId, locationData)
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
        stopForegroundCompat()
        stopSelf()
    }

    private fun tryToDestroyMe() {
        if (startInProgress.not() && liveInfoSet.isEmpty()) {
            Timber.i("Destroying self, time is up for all rooms")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")
        binder?.cleanUp()
        binder = null
        jobs.forEach { it.cancel() }
        jobs.clear()
        locationTracker.removeCallback(this)
    }

    private fun addRoomArgs(beaconEventId: String, roomArgs: RoomArgs) {
        Timber.i("adding roomArgs for beaconEventId: $beaconEventId")
        liveInfoSet.removeAll { it.beaconEventId == beaconEventId }
        liveInfoSet.add(LiveInfo(beaconEventId, roomArgs))
        launchWithActiveSession { _roomIdsOfActiveLives.emit(getRoomIdsOfActiveLives()) }
    }

    private fun removeRoomArgs(beaconEventId: String) {
        Timber.i("removing roomArgs for beaconEventId: $beaconEventId")
        liveInfoSet.removeAll { it.beaconEventId == beaconEventId }
        launchWithActiveSession { _roomIdsOfActiveLives.emit(getRoomIdsOfActiveLives()) }
    }

    private fun listenForLiveSummaryChanges(roomId: String, beaconEventId: String) {
        launchWithActiveSession { session ->
            val job = getLiveLocationShareSummaryUseCase.execute(roomId, beaconEventId)
                    .distinctUntilChangedBy { it?.isActive }
                    .filter { it?.isActive == false || (it == null && isLiveRedacted(roomId, beaconEventId)) }
                    .onEach { stopSharingLocation(beaconEventId) }
                    .launchIn(session.coroutineScope)
            jobs.add(job)
        }
    }

    private suspend fun isLiveRedacted(roomId: String, beaconEventId: String): Boolean {
        return checkIfEventIsRedactedUseCase.execute(roomId = roomId, eventId = beaconEventId)
    }

    private fun launchWithActiveSession(block: suspend CoroutineScope.(Session) -> Unit) =
            activeSessionHolder
                    .getSafeActiveSession()
                    ?.let { session ->
                        session.coroutineScope.launch(
                                block = { block(session) }
                        )
                    }

    fun getRoomIdsOfActiveLives(): Set<String> {
        return liveInfoSet.map { it.roomArgs.roomId }.toSet()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    interface Callback {
        fun onServiceError(error: Throwable)
    }

    companion object {
        const val EXTRA_ROOM_ARGS = "EXTRA_ROOM_ARGS"
        private const val FOREGROUND_SERVICE_NOTIFICATION_ID = 300
    }

    private data class LiveInfo(
            val beaconEventId: String,
            val roomArgs: RoomArgs
    )
}
