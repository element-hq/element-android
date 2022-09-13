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

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import gobind.DendriteMonolith
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

class DendriteService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject lateinit var vectorPreferences: VectorPreferences

    private val binder = DendriteLocalBinder()
    private var monolith: DendriteMonolith? = null

    inner class DendriteLocalBinder : Binder() {
        fun getService() : DendriteService {
            return this@DendriteService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
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
}
