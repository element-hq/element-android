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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.widget.Toast
import gobind.Conduit
import gobind.DendriteMonolith
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.concurrent.thread

class DendriteService : Service() {
    private val binder = DendriteLocalBinder()
    private var monolith: DendriteMonolith? = null
    private val serviceUUID = ParcelUuid(UUID.fromString("a2fda8dd-d250-4a64-8b9a-248f50b93c64"))
    private val psmUUID = UUID.fromString("15d4151b-1008-41c0-85f2-950facf8a3cd")

    private lateinit var manager: BluetoothManager
    private lateinit var gattServer: BluetoothGattServer
    private lateinit var gattCharacteristic: BluetoothGattCharacteristic
    private var gattService = BluetoothGattService(serviceUUID.uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

    private var adapter = BluetoothAdapter.getDefaultAdapter()
    private var advertiser = adapter.bluetoothLeAdvertiser
    private var scanner = adapter.bluetoothLeScanner

    private var l2capServer: BluetoothServerSocket = adapter.listenUsingInsecureL2capChannel()
    private var l2capPSM: ByteArray = intToBytes(l2capServer.psm)

    private var connections: MutableMap<String, DendriteBLEPeering> = mutableMapOf<String, DendriteBLEPeering>()

    inner class DendriteLocalBinder : Binder() {
        fun getService() : DendriteService {
            return this@DendriteService
        }
    }

    inner class DendriteBLEPeering(conduit: Conduit, socket: BluetoothSocket) {
        private var conduit: Conduit = conduit
        private var socket: BluetoothSocket = socket

        public val isConnected: Boolean
            get() = socket.isConnected

        private var bleInput: InputStream = socket.inputStream
        private var bleOutput: OutputStream = socket.outputStream

        public fun start() {
            thread {
                reader()
            }
            thread {
                writer()
            }
        }

        public fun close() {
            socket.close()
        }

        private fun reader() {
            var b = ByteArray(socket.maxReceivePacketSize)
            while (isConnected) {
                try {
                    val rn = bleInput.read(b)
                    if (rn < 0) {
                        continue
                    }
                    val r = b.sliceArray(0 until rn).clone()
                    conduit.write(r)
                } catch (e: Exception) {
                    Timber.e(e)
                    break
                }
            }
            close()
        }

        private fun writer() {
            var b = ByteArray(socket.maxTransmitPacketSize)
            while (isConnected) {
                try {
                    val rn = conduit.read(b).toInt()
                    if (rn < 0) {
                        continue
                    }
                    val w = b.sliceArray(0 until rn).clone()
                    bleOutput.write(w)
                } catch (e: Exception) {
                    Timber.e(e)
                    break
                }
            }
            close()
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
                    .addServiceUuid(serviceUUID)
                    .build()

            val scanFilter = ScanFilter.Builder()
                    .setServiceUuid(serviceUUID)
                    .build()

            val scanFilters: MutableList<ScanFilter> = ArrayList()
            scanFilters.add(scanFilter)

            val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build()

            advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
            scanner.startScan(scanFilters, scanSettings, scanCallback)

            gattCharacteristic = BluetoothGattCharacteristic(psmUUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
            gattService.addCharacteristic(gattCharacteristic)

            manager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            gattServer = manager.openGattServer(applicationContext, gattServerCallback)
            gattServer.addService(gattService)

            GlobalScope.launch {
                while (true) {
                    Timber.i("BLE: Waiting for connection on PSM ${l2capServer.psm}")
                    try {
                        var remote = l2capServer.accept() ?: continue
                        val device = remote.remoteDevice.address.toString()

                        var connection = connections[device]
                        if (connection != null && connection.isConnected) {
                            connection.close()
                            connections.remove(device)
                        }

                        Timber.i("BLE: Connected inbound $device PSM $l2capPSM")

                        Timber.i("Creating DendriteBLEPeering")
                        connections[device] = DendriteBLEPeering(monolith!!.conduit("BLE"), remote)
                        Timber.i("Starting DendriteBLEPeering")
                        connections[device]!!.start()

                        Timber.i("BLE: Created BLE peering with $device PSM $l2capPSM")
                    } catch (e: Exception) {
                        Timber.i("BLE: Accept exception: ${e.message}")
                    }
                }
            }
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

    private val gattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, l2capPSM);
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return
            }
            val services = gatt?.services ?: return
            services.forEach { service ->
                if (service.uuid == serviceUUID.uuid) {
                    service.characteristics.forEach { characteristic ->
                        if (characteristic.uuid == psmUUID) {
                            Timber.i("BLE: Requesting PSM characteristic")
                            gatt.readCharacteristic(characteristic)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)

            val device = gatt?.device ?: return
            val psmBytes = characteristic?.value ?: return
            val psm = bytesToInt(psmBytes)

            var connection = connections[device.address]
            if (connection != null && connection.isConnected) {
                return
            }

            Timber.i("BLE: Connecting outbound $device PSM $psm")

            val socket = device.createInsecureL2capChannel(psm)

            try {
                socket.connect()
            } catch (e: Exception) {
                timber.log.Timber.i("BLE: Failed to connect to $device PSM $psm: ${e.toString()}")
                return
            }

            Timber.i("BLE: Connected outbound $device PSM $psm")

            Timber.i("Creating DendriteBLEPeering")
            connections[device.address] = DendriteBLEPeering(monolith!!.conduit("BLE"), socket)
            Timber.i("Starting DendriteBLEPeering")
            connections[device.address]!!.start()
        }
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

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            result.device.connectGatt(applicationContext, false, gattCallback)
        }
    }

    private fun intToBytes(x: Int): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Integer.BYTES)
        buffer.putInt(x)
        return buffer.array()
    }

    fun bytesToInt(bytes: ByteArray): Int {
        val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Integer.BYTES)
        buffer.put(bytes.sliceArray(0 until java.lang.Integer.BYTES))
        buffer.flip()
        return buffer.int
    }
}
