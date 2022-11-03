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

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import javax.inject.Inject

class BluetoothLowEnergyServiceConnection @Inject constructor(
        private val context: Context,
) : ServiceConnection, BluetoothLowEnergyService.Callback {

    interface Callback {
        fun onCharacteristicRead(data: ByteArray)
        fun onConnectedToDevice(device: BluetoothDevice)
    }

    private var isBound = false
    private var bluetoothLowEnergyService: BluetoothLowEnergyService? = null
    private var deviceAddress: String? = null

    var callback: Callback? = null

    fun bind(deviceAddress: String, callback: Callback) {
        this.deviceAddress = deviceAddress
        this.callback = callback

        if (!isBound) {
            Intent(context, BluetoothLowEnergyService::class.java).also { intent ->
                context.bindService(intent, this, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        bluetoothLowEnergyService = (binder as BluetoothLowEnergyService.LocalBinder).getService().also {
            it.callback = this
        }

        deviceAddress?.let {
            bluetoothLowEnergyService?.connect(it)
        }
        isBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isBound = false
        bluetoothLowEnergyService = null
    }

    override fun onConnectedToDevice(device: BluetoothDevice) {
        callback?.onConnectedToDevice(device)
    }

    override fun onCharacteristicRead(data: ByteArray) {
        callback?.onCharacteristicRead(data)
    }

    fun stopService() {
        bluetoothLowEnergyService?.stopService()
    }
}
