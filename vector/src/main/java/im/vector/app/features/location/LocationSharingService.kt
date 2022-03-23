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
import android.os.IBinder
import android.os.Parcelable
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.services.VectorService
import im.vector.app.features.notifications.NotificationUtils
import kotlinx.parcelize.Parcelize
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

    private var roomArgsList = mutableListOf<RoomArgs>()

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
            roomArgsList.add(roomArgs)

            // Show a sticky notification
            val notification = notificationUtils.buildLiveLocationSharingNotification()
            startForeground(roomArgs.roomId.hashCode(), notification)

            // Schedule a timer to stop sharing
            scheduleTimer(roomArgs.roomId, roomArgs.durationMillis)
        }

        return START_STICKY
    }

    private fun scheduleTimer(roomId: String, durationMillis: Long) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                stopSharingLocation(roomId)
            }
        }, durationMillis)
    }

    private fun stopSharingLocation(roomId: String) {
        Timber.i("### LocationSharingService.stopSharingLocation for $roomId")
        synchronized(roomArgsList) {
            roomArgsList.removeAll { it.roomId == roomId }
            if (roomArgsList.isEmpty()) {
                Timber.i("### LocationSharingService. Destroying self, time is up for all rooms")
                destroyMe()
            }
        }
    }

    private fun destroyMe() {
        locationTracker.removeCallback(this)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("### LocationSharingService.onDestroy")
        destroyMe()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_ROOM_ARGS = "EXTRA_ROOM_ARGS"
    }

    override fun onLocationUpdate(locationData: LocationData) {
        Timber.i("### LocationSharingService.onLocationUpdate. Uncertainty: ${locationData.uncertainty}")
    }

    override fun onLocationProviderIsNotAvailable() {
        stopForeground(true)
        stopSelf()
    }
}
