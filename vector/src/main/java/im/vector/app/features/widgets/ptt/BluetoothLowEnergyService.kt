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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.services.VectorAndroidService
import im.vector.app.features.notifications.NotificationUtils
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class BluetoothLowEnergyService : VectorAndroidService() {

    interface Callback {
        fun onCharacteristicRead(data: ByteArray)
    }

    @Inject lateinit var notificationUtils: NotificationUtils

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val binder = LocalBinder()

    var callback: Callback? = null

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

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            gatt.services.forEach { service ->
                service.characteristics.forEach { characteristic ->
                    if (characteristic.uuid.equals(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))) {
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onCharacteristicRead(characteristic)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            onCharacteristicRead(characteristic)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeBluetoothAdapter()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationUtils.buildBluetoothLowEnergyNotification()
        startForeground(Random.nextInt(), notification)
        return START_STICKY
    }

    fun stopService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyMe()
    }

    private fun destroyMe() {
        callback = null
        bluetoothGatt?.disconnect()
        bluetoothAdapter = null
        bluetoothGatt = null
    }

    private fun initializeBluetoothAdapter() {
        val bluetoothManager = getSystemService<BluetoothManager>()
        bluetoothAdapter = bluetoothManager?.adapter
    }

    fun connect(address: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = bluetoothAdapter
                    ?.getRemoteDevice(address)
                    ?.connectGatt(applicationContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    private fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic) {
        @Suppress("DEPRECATION") val data = characteristic.value
        Timber.d("### BluetoothLowEnergyService. $data")
        if (data.isNotEmpty()) {
            callback?.onCharacteristicRead(data)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLowEnergyService = this@BluetoothLowEnergyService
    }
}
