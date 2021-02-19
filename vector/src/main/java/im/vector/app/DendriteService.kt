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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.widget.Toast
import androidx.core.app.NotificationCompat
import gobind.Conduit
import gobind.DendriteMonolith
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import javax.inject.Inject
import kotlin.concurrent.thread

class DendriteService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var vectorPreferences = VectorPreferences(this)

    private var notificationChannel = NotificationChannel("im.vector.p2p", "Element P2P", NotificationManager.IMPORTANCE_DEFAULT)
    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null

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
    private var l2capPSM: ByteArray = intToBytes(l2capServer.psm.toShort())

    private var connecting: MutableMap<String, Boolean> = mutableMapOf<String, Boolean>()
    private var connections: MutableMap<String, DendriteBLEPeering> = mutableMapOf<String, DendriteBLEPeering>()
    private var conduits: MutableMap<String, Conduit> = mutableMapOf<String, Conduit>()

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

            updateNotification()
        }

        public fun close() {
            val device = socket.remoteDevice.address.toString()
            Timber.i("BLE: Closing connection to $device")

            conduit.close()
            socket.close()

            val conduit = conduits[device]
            if (conduit != null) {
                val port = conduit.port()
                if (port > 0) {
                    try {
                        monolith?.disconnectPort(port)
                    } catch (e: Exception) {
                        // no biggie
                    }
                }
            }

            connecting.remove(device)
            connections.remove(device)
            conduits.remove(device)

            updateNotification()
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

    fun updateNotification() {
        if (notificationManager == null || monolith == null) {
            return
        }

        val peers = monolith!!.peerCount().toInt()
        val sessions = monolith!!.sessionCount().toInt()

        var title: String
        var text: String

        if (peers == 0) {
            title = "No connectivity"
            text = "There are no nearby devices connected"
        } else {
            text = when (sessions) {
                0    -> "No active connections"
                1    -> "$sessions active connection"
                else -> "$sessions active connections"
            }
            title = when (peers) {
                1    -> "$peers nearby device"
                else -> "$peers nearby devices"
            }
        }

        notification = NotificationCompat.Builder(applicationContext, "im.vector.p2p")
                .setContentTitle(title)
                .setContentText(text)
                .setNotificationSilent()
                .setSmallIcon(R.drawable.ic_smartphone)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setOnlyAlertOnce(true)
                .build()
        notificationManager!!.notify(545, notification)
    }

    override fun onCreate() {
        if (monolith == null) {
            monolith = gobind.DendriteMonolith()
        }

        vectorPreferences.subscribeToChanges(this)

        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager!!.createNotificationChannel(notificationChannel)

        monolith!!.storageDirectory = applicationContext.filesDir.toString()
        monolith!!.start()

        if (vectorPreferences.p2pEnableStatic()) {
            monolith!!.setStaticPeer(vectorPreferences.p2pStaticURI())
        }
        monolith!!.setMulticastEnabled(vectorPreferences.p2pEnableNearby())

        Timer().schedule(object : TimerTask() {
            override fun run() {
                updateNotification()
            }
        }, 0, 1000)

        if (adapter.isEnabled && adapter.isMultipleAdvertisementSupported) {
            manager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

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
                        val conduit = monolith!!.conduit("BLE-"+device)
                        conduits[device] = conduit
                        connections[device] = DendriteBLEPeering(conduit, remote)
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
            advertiser.stopAdvertising(advertiseCallback)
            scanner.stopScan(scanCallback)
        }

        if (notificationManager != null) {
            notificationManager!!.cancel(545)
        }

        monolith?.stop()
        monolith = null

        super.onDestroy()
    }

    private val gattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, l2capPSM);
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Timber.i("BLE: Discovering services via GATT ${gatt.toString()}")
                gatt?.discoverServices()
            } else {
                connecting.remove(gatt?.device?.address?.toString())
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connecting.remove(gatt?.device?.address?.toString())
                return
            }
            val services = gatt?.services ?: return
            Timber.i("BLE: Found services via GATT ${gatt.toString()}")
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
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connecting.remove(gatt?.device?.address?.toString())
                return
            }
            val device = gatt?.device ?: return
            val psmBytes = characteristic?.value ?: return
            val psm = bytesToInt(psmBytes)

            var connection = connections[device.address.toString()]
            if (connection != null && connection.isConnected) {
                connection.close()
            }

            Timber.i("BLE: Connecting outbound $device PSM $psm")

            val socket = device.createInsecureL2capChannel(psm.toInt())
            try {
                socket.connect()
            } catch (e: Exception) {
                timber.log.Timber.i("BLE: Failed to connect to $device PSM $psm: ${e.toString()}")
                return
            }
            if (!socket.isConnected) {
                Timber.i("BLE: Expected to be connected but not")
                return
            }

            Timber.i("BLE: Connected outbound $device PSM $psm")

            Timber.i("Creating DendriteBLEPeering")
            val conduit = monolith!!.conduit("BLE")
            conduits[device.address] = conduit
            connections[device.address] = DendriteBLEPeering(conduit, socket)
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
            if (!result.isConnectable || result.scanRecord?.serviceUuids?.contains(serviceUUID) != true) {
                return
            }

            val key = result.device.address.toString()
            // Timber.i("BLE: Scan result found $key")

            if (connections.containsKey(key)) {
                val connection = connections[key]
                if (connection?.isConnected == true) {
                    // Timber.i("BLE: Ignoring device $key that we are already connected to")
                    return
                }
            }

            if (connecting.containsKey(key)) {
                // Timber.i("BLE: Ignoring device $key that we are already connecting to")
                return
            }

            Timber.i("BLE: Connecting to $key")
            connecting[key] = true
            result.device.connectGatt(applicationContext, false, gattCallback)
        }
    }

    private fun intToBytes(x: Short): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Short.BYTES)
        buffer.putShort(x)
        return buffer.array()
    }

    fun bytesToInt(bytes: ByteArray): Short {
        val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Short.BYTES)
        buffer.put(bytes.sliceArray(0 until java.lang.Short.BYTES))
        buffer.flip()
        return buffer.short
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val m = monolith ?: return
        when (key){
            VectorPreferences.SETTINGS_P2P_ENABLE_NEARBY -> {
                val enabled = vectorPreferences.p2pEnableNearby()
                m.setMulticastEnabled(enabled)
                if (!enabled) {
                    m.disconnectMulticastPeers()
                }
                Toast.makeText(applicationContext, "Toggled enable nearby: $enabled", Toast.LENGTH_SHORT).show()
            }

            VectorPreferences.SETTINGS_P2P_ENABLE_STATIC -> {
                val enabled = vectorPreferences.p2pEnableStatic()
                if (enabled) {
                    val uri = vectorPreferences.p2pStaticURI()
                    m.setStaticPeer(uri)
                } else {
                    m.disconnectNonMulticastPeers()
                }
                Toast.makeText(applicationContext, "Toggled enable static: $enabled", Toast.LENGTH_SHORT).show()
            }

            VectorPreferences.SETTINGS_P2P_STATIC_URI -> {
                if (vectorPreferences.p2pEnableStatic()) {
                    val uri = vectorPreferences.p2pStaticURI()
                    m.setStaticPeer(uri)
                    Toast.makeText(applicationContext, "Updated static peer URI: $uri", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
