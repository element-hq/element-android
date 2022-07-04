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

package im.vector.app.features.widgets.ptt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import im.vector.app.core.services.VectorService
import androidx.core.content.getSystemService
import timber.log.Timber

class BluetoothLowEnergyService : VectorService() {

    private val bluetoothManager = getSystemService<BluetoothManager>()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTING -> Timber.d("### BluetoothLowEnergyService.newState: STATE_CONNECTING")
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("### BluetoothLowEnergyService.newState: STATE_CONNECTED")
                    bluetoothGatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTING -> Timber.d("### BluetoothLowEnergyService.newState: STATE_DISCONNECTING")
                BluetoothProfile.STATE_DISCONNECTED -> Timber.d("### BluetoothLowEnergyService.newState: STATE_DISCONNECTED")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        initializeBluetoothAdapter()
    }

    private fun initializeBluetoothAdapter() {
        bluetoothAdapter = bluetoothManager?.adapter
    }

    fun connect(address: String) {
        bluetoothGatt = bluetoothAdapter
                ?.getRemoteDevice(address)
                ?.connectGatt(applicationContext, false, gattCallback)
    }
}
