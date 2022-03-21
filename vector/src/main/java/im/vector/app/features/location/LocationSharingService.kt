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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.services.VectorService
import im.vector.app.features.notifications.NotificationUtils
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocationSharingService : VectorService(), LocationTracker.Callback {

    @Inject lateinit var notificationUtils: NotificationUtils
    @Inject lateinit var locationTracker: LocationTracker

    private var sessionId: String? = null
    private var roomId: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        roomId = intent?.getStringExtra(EXTRA_ROOM_ID)

        if (sessionId == null || roomId == null) {
            stopForeground(true)
            stopSelf()
        }

        // Show a sticky notification
        val notification = notificationUtils.buildLiveLocationSharingNotification()
        startForeground(roomId!!.hashCode(), notification)

        // Start tracking location
        locationTracker.addCallback(this)
        locationTracker.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.removeCallback(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
    }

    override fun onLocationUpdate(locationData: LocationData) {
        Timber.d("### LocationSharingService.onLocationUpdate: ${locationData.latitude} - ${locationData.longitude}")
    }

    override fun onLocationProviderIsNotAvailable() {
        stopForeground(true)
        stopSelf()
    }
}
