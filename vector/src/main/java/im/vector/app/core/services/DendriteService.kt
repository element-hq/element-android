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

package im.vector.app.core.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import gobind.DendriteMonolith
import gobind.Gobind
import im.vector.app.R
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

class DendriteService : VectorAndroidService(), SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject lateinit var vectorPreferences: VectorPreferences

    private var notificationManager: NotificationManager? = null

    private val binder = DendriteLocalBinder()
    private var monolith: DendriteMonolith? = null

    companion object {
        private const val ID = 532345
        private const val CHANNEL_ID = "im.vector.p2p"
        private const val CHANNEL_NAME = "Element P2P"
    }

    inner class DendriteLocalBinder : Binder() {
        fun getService() : DendriteService {
            return this@DendriteService
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // post notification belonging to this service
        startForeground(ID, serviceNotification())

        Timber.i("Starting Dendrite")
        if (monolith == null) {
            monolith = gobind.DendriteMonolith()
        }
        //monolith?.storageDirectory = applicationContext.filesDir.toString()
        //monolith?.start()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Occurs when the element app is closed from the system tray
        stopForeground(true)
        myStopSelf()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        //val m = monolith ?: return
        /*
        when (key){
            VectorPreferences.SETTINGS_P2P_ENABLE_MULTICAST -> {
                val enabled = vectorPreferences.p2pEnableMulticast()
                m.setMulticastEnabled(enabled)
            }

            VectorPreferences.SETTINGS_P2P_ENABLE_BLUETOOTH -> {
                val enabled = vectorPreferences.p2pEnableBluetooth()
                if (enabled) {
                    startBluetooth()
                } else {
                    stopBluetooth()
                    m.disconnectType(Gobind.PeerTypeBluetooth)
                }
            }

            VectorPreferences.SETTINGS_P2P_ENABLE_STATIC -> {
                val enabled = vectorPreferences.p2pEnableStatic()
                if (enabled) {
                    val uri = vectorPreferences.p2pStaticURI()
                    m.setStaticPeer(uri)
                } else {
                    m.setStaticPeer("")
                }
            }

            VectorPreferences.SETTINGS_P2P_STATIC_URI -> {
                if (vectorPreferences.p2pEnableStatic()) {
                    val uri = vectorPreferences.p2pStaticURI()
                    m.setStaticPeer(uri)
                }
            }

            VectorPreferences.SETTINGS_P2P_BLE_CODED_PHY -> {
                val enabled = vectorPreferences.p2pEnableBluetooth()
                if (enabled) {
                    stopBluetooth()
                    m.disconnectType(Gobind.PeerTypeBluetooth)
                    startBluetooth()
                }
            }
        }
        */
    }

    private fun serviceNotification(): Notification {
        createChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_smartphone)
                .setContentTitle("Peer-to-peer service running")
                .setContentText("It runs as a foreground service.")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .build()
    }

    private fun createChannel() {
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.createNotificationChannel(
                    NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            )
        }
    }
}
