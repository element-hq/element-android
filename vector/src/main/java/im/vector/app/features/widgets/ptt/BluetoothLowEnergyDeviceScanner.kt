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
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import androidx.core.os.HandlerCompat.postDelayed
import javax.inject.Inject

val PTT_REGEX = Regex(".*ptt.*", RegexOption.IGNORE_CASE)

class BluetoothLowEnergyDeviceScanner @Inject constructor(
        context: Context
) {

    interface Callback {
        fun onPairedDeviceFound(device: BluetoothDevice)
        fun onScanResult(device: BluetoothDevice)
    }

    private val bluetoothManager = context.getSystemService<BluetoothManager>()

    var callback: Callback? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            callback?.onScanResult(result.device)
        }
    }

    fun startScanning() {
        stopScanning()
        bluetoothManager
                ?.adapter
                ?.bondedDevices
                ?.firstOrNull { it.name.matches(PTT_REGEX) }
                ?.let { bluetoothDevice ->
                    callback?.onPairedDeviceFound(bluetoothDevice)
                }
                ?: run {
                    bluetoothManager
                            ?.adapter
                            ?.bluetoothLeScanner
                            ?.startScan(scanCallback)

                    Handler(Looper.getMainLooper()).postDelayed({
                        stopScanning()
                    }, 10_000)
                }
    }

    private fun stopScanning() {
        bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }
}
