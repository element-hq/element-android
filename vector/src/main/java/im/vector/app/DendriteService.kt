/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.widget.Toast
import gobind.DendriteMonolith
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.UUID

class DendriteService : Service() {
    private val binder = DendriteLocalBinder()
    private var monolith: DendriteMonolith? = null

    private val serviceUUID = ParcelUuid(UUID.fromString("a2fda8dd-d250-4a64-8b9a-248f50b93c64"))
    private var adapter = BluetoothAdapter.getDefaultAdapter()
    private var advertiser = adapter.bluetoothLeAdvertiser
    private var scanner = adapter.bluetoothLeScanner
    private var server: BluetoothServerSocket = adapter.listenUsingInsecureL2capChannel()
    private var psm: ByteArray = intToBytes(server.psm)

    inner class DendriteLocalBinder : Binder() {
        fun getService() : DendriteService {
            return this@DendriteService
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    fun registerUser(userID: String, password: String): String {
        return monolith?.registerUser(userID, password) ?: ""
    }

    fun registerDevice(userID: String): String {
        return monolith?.registerDevice(userID, "P2P") ?: ""
    }

    override fun onCreate() {
        if (monolith == null) {
            monolith = gobind.DendriteMonolith()
        }

        Toast.makeText(applicationContext, "Starting Dendrite", Toast.LENGTH_SHORT).show()
        monolith?.storageDirectory = applicationContext.filesDir.toString()
        monolith?.start()

        if (adapter.isEnabled && adapter.isMultipleAdvertisementSupported) {
            val advertiseSettings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setTimeout(0)
                    .setConnectable(true)
                    .build()

            val advertiseData = AdvertiseData.Builder()
                    .addServiceData(serviceUUID, psm)
                    .build()

            val scanMask: ByteArray = ByteArray(4)
            val scanFilter = ScanFilter.Builder()
                    .setServiceData(serviceUUID, scanMask, scanMask)
                    .build()

            val scanFilters: MutableList<ScanFilter> = ArrayList()
            scanFilters.add(scanFilter)

            val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build()

            advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
            scanner.startScan(scanFilters, scanSettings, scanCallback)
        }

        super.onCreate()
    }

    override fun onDestroy() {
        if (monolith == null) {
            return
        }

        if (adapter.isEnabled && adapter.isMultipleAdvertisementSupported) {
            //server.close()
            advertiser.stopAdvertising(advertiseCallback)
            scanner.stopScan(scanCallback)
        }

        Toast.makeText(applicationContext, "Shutting down Dendrite", Toast.LENGTH_SHORT).show()
        monolith?.stop()
        monolith = null

        super.onDestroy()
    }

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Toast.makeText(applicationContext, "BLE advertise failed: $errorCode", Toast.LENGTH_SHORT).show()
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Toast.makeText(applicationContext, "BLE advertise started", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.i("BLE: error %s", errorCode)
            Toast.makeText(applicationContext, "BLE: Error code $errorCode", Toast.LENGTH_SHORT).show()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Timber.i("BLE: batch")
            Toast.makeText(applicationContext, "BLE: Batched", Toast.LENGTH_SHORT).show()
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device.address.toString()
            val record = result.scanRecord ?: return
            try {
                val service = record.serviceData.getValue(serviceUUID) ?: return
                val psm = bytesToInt(service)

                Toast.makeText(applicationContext, "BLE: Found $device PSM $psm", Toast.LENGTH_SHORT).show()

                //var socket = result.device.createInsecureL2capChannel(psm)
                //socket.close()
            } catch (e: Exception) {
                Timber.i("BLE: Device $device error %s", e.message)
            }
        }
    }

    private fun intToBytes(x: Int): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Integer.BYTES)
        buffer.putInt(x)
        return buffer.array()
    }

    fun bytesToInt(bytes: ByteArray): Int {
        val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Integer.BYTES)
        buffer.put(bytes)
        buffer.flip()
        return buffer.int
    }
}
