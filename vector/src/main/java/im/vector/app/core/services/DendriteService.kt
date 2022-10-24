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

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import gobind.Conduit
import gobind.DendriteMonolith
import gobind.Gobind
import gobind.InterfaceInfo
import im.vector.app.R
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.concurrent.thread

class DendriteService : VectorAndroidService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var vectorPreferences: VectorPreferences

    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null

    private val binder = DendriteLocalBinder()
    private var monolith: DendriteMonolith? = null
    private val serviceUUID = ParcelUuid(UUID.fromString("a2fda8dd-d250-4a64-8b9a-248f50b93c64"))
    private val psmUUID = UUID.fromString("15d4151b-1008-41c0-85f2-950facf8a3cd")

    private var manager: BluetoothManager? = null
    private lateinit var gattServer: BluetoothGattServer
    private lateinit var gattCharacteristic: BluetoothGattCharacteristic
    private var gattService = BluetoothGattService(serviceUUID.uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

    private lateinit var advertiser: BluetoothLeAdvertiser
    private lateinit var scanner: BluetoothLeScanner

    private lateinit var l2capServer: BluetoothServerSocket
    private lateinit var l2capPSM: ByteArray

    private var connecting: MutableMap<String, Boolean> = mutableMapOf<String, Boolean>()
    private var connections: MutableMap<String, DendriteBLEPeering> = mutableMapOf<String, DendriteBLEPeering>()
    private var conduits: MutableMap<String, Conduit> = mutableMapOf<String, Conduit>()

    private lateinit var multicastLock: WifiManager.MulticastLock
    private var networkCallback = NetworkCallback()

    private val bleReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return
            }
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> return
                    BluetoothAdapter.STATE_TURNING_OFF -> stopBluetooth()
                    BluetoothAdapter.STATE_ON -> startBluetooth()
                    BluetoothAdapter.STATE_TURNING_ON -> return
                }
            }
        }
    }

    private val gattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, l2capPSM);
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Timber.i("BLE: Discovering services via GATT ${gatt.toString()}")
                gatt?.discoverServices()
            } else {
                connecting.remove(gatt?.device?.address?.toString())
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connecting.remove(gatt?.device?.address?.toString())
                return
            }
            val services = gatt?.services ?: return
            if (services.size == 0) {
                connecting.remove(gatt.device?.address?.toString())
                return
            }
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

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (gatt == null || characteristic == null) {
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connecting.remove(gatt.device?.address?.toString())
                return
            }
            val device = gatt.device
            val psmBytes = characteristic.value
            val psm = bytesToInt(psmBytes)

            var connection = connections[device.address.toString()]
            if (connection != null) {
                if (connection.isConnected) {
                    connection.close()
                } else {
                    connections.remove(device.address.toString())
                    conduits.remove(device.address.toString())
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Timber.i("BLE: Connecting outbound $device PSM $psm")
                val socket = device.createInsecureL2capChannel(psm.toInt())
                try {
                    socket.connect()
                } catch (e: Exception) {
                    timber.log.Timber.i("BLE: Failed to connect to $device PSM $psm: ${e.toString()}")
                    connecting.remove(device.address)
                    return
                }
                if (!socket.isConnected) {
                    Timber.i("BLE: Expected to be connected but not")
                    connecting.remove(device.address)
                    return
                }

                Timber.i("BLE: Connected outbound $device PSM $psm")

                Timber.i("Creating DendriteBLEPeering")
                val conduit = monolith!!.conduit("ble", Gobind.PeerTypeBluetooth)
                conduits[device.address] = conduit
                connections[device.address] = DendriteBLEPeering(conduit, socket)
                Timber.i("Starting DendriteBLEPeering")
                connections[device.address]!!.start()
                connecting.remove(device.address)
            }
        }
    }

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            if (errorCode != ADVERTISE_FAILED_ALREADY_STARTED) {
                Toast.makeText(applicationContext, "BLE legacy advertise failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Toast.makeText(applicationContext, "BLE legacy advertise started", Toast.LENGTH_SHORT).show()
        }
    }

    private val advertiseSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status)
            if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                Toast.makeText(applicationContext, "BLE advertise set started", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            super.onAdvertisingSetStopped(advertisingSet)
            Toast.makeText(applicationContext, "BLE advertise set stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            if (errorCode != SCAN_FAILED_ALREADY_STARTED) {
                Timber.i("BLE: error %s", errorCode)
                Toast.makeText(applicationContext, "BLE: Error $errorCode scanning for devices", Toast.LENGTH_SHORT).show()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (!result.isConnectable || result.scanRecord?.serviceUuids?.contains(serviceUUID) != true) {
                return
            }

            val key = result.device.address.toString()
            Timber.i("BLE: Scan result found $key")

            if (connections.containsKey(key)) {
                val connection = connections[key]
                if (connection?.isConnected == true) {
                    Timber.i("BLE: Ignoring device $key that we are already connected to")
                    return
                }
            }

            if (connecting.containsKey(key)) {
                Timber.i("BLE: Ignoring device $key that we are already connecting to")
                return
            }

            Timber.i("BLE: Connecting to $key")
            connecting[key] = true
            result.device.connectGatt(applicationContext, false, gattCallback)
        }
    }

    companion object {
        const val ACTION = "ACTION"
        const val ACTION_START = "ACTION_START"
        private const val ID = 532345
        private const val CHANNEL_ID = "im.vector.p2p"
        private const val CHANNEL_NAME = "Element P2P"
    }

    inner class DendriteBLEPeering(private var conduit: Conduit, private var socket: BluetoothSocket) {
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

            // TODO
            //updateNotification()
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

            // TODO
            //updateNotification()
        }

        private fun reader() {
            val b = ByteArray(socket.maxReceivePacketSize)
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
            val b = ByteArray(socket.maxTransmitPacketSize)
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

    inner class DendriteLocalBinder : Binder() {
        fun getService() : DendriteService {
            return this@DendriteService
        }
    }

    private fun updateNotification() {
        if (notificationManager == null || monolith == null) {
            return
        }

        val remotePeers = monolith!!.peerCount(Gobind.PeerTypeRemote).toInt()
        val multicastPeers = monolith!!.peerCount(Gobind.PeerTypeMulticast).toInt()
        val bluetoothPeers = monolith!!.peerCount(Gobind.PeerTypeBluetooth).toInt()

        val title: String = "P2P Matrix service running"
        val text: String

        text = if (remotePeers+multicastPeers+bluetoothPeers == 0) {
            "No connectivity"
        } else {
            val texts: MutableList<String> = mutableListOf<String>()
            if (bluetoothPeers > 0) {
                texts.add(0, "$bluetoothPeers BLE")
            }
            if (multicastPeers > 0) {
                texts.add(0, "$multicastPeers LAN")
            }
            if (remotePeers > 0) {
                texts.add(0, "static")
            }
            "Connected to " + texts.joinToString(", ")
        }

        notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_ems_logo)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .build()
        notificationManager!!.notify(ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.getStringExtra(ACTION);
        Timber.i("Action: $action")
        when (action) {
            ACTION_START -> {
                startService()
            }
        }
        return START_NOT_STICKY
    }

    private fun startService() {
        if (monolith != null) {
            return
        }

        // post notification belonging to this service
        startForeground(ID, serviceNotification())

        Timber.i("Starting Dendrite")
        if (monolith == null) {
            monolith = gobind.DendriteMonolith()
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        this.registerReceiver(bleReceiver, intentFilter)

        vectorPreferences = singletonEntryPoint().vectorPreferences()
        vectorPreferences.subscribeToChanges(this)

        monolith?.storageDirectory = applicationContext.filesDir.toString()
        monolith?.cacheDirectory = applicationContext.cacheDir.toString()
        monolith?.start()

        if (vectorPreferences.p2pEnableStatic()) {
            monolith!!.setStaticPeer(vectorPreferences.p2pStaticURI())
        }

        monolith?.registerNetworkCallback(networkCallback)

        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("Element.P2P")
        val enabled = vectorPreferences.p2pEnableMulticast()
        if (enabled) {
            multicastLock.acquire()
        }
        monolith!!.setMulticastEnabled(enabled)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startBluetooth()
        }

        Timer().schedule(object : TimerTask() {
            override fun run() {
                updateNotification()
            }
        }, 0, 1000)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        if (monolith != null) {
            Timber.i("Stopping Dendrite")

            // Occurs when the element app is closed from the system tray
            stopBluetooth()
            this.unregisterReceiver(bleReceiver)

            monolith?.stop()
            monolith = null
        }

        notificationManager?.cancelAll()
        stopForeground(true)
        myStopSelf()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val m = monolith ?: return
        when (key){
            VectorPreferences.SETTINGS_P2P_ENABLE_MULTICAST -> {
                val enabled = vectorPreferences.p2pEnableMulticast()
                if (enabled) {
                    multicastLock.acquire()
                } else {
                    if (multicastLock.isHeld) {
                        multicastLock.release()
                    }
                }
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
    }

    fun registerUser(userID: String, password: String): String {
        return monolith?.registerUser(userID, password) ?: ""
    }

    fun registerDevice(userID: String): String {
        return monolith?.registerDevice(userID, "P2P") ?: ""
    }

    private fun serviceNotification(): Notification {
        createChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_ems_logo)
                .setContentTitle("Element p2p service running")
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun startBluetooth() {
        when {
            !vectorPreferences.p2pEnableBluetooth() -> {
                Timber.i("BLE: Bluetooth peerings not enabled")
                return
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                Timber.i("BLE: Bluetooth peerings not supported on this version of Android")
                return
            }
            ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED -> {
                Timber.i("BLE: BLUETOOTH_ADVERTISE permission not granted, Bluetooth will not be available")
                return
            }
            ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED -> {
                    Timber.i("BLE: BLUETOOTH_SCAN permission not granted, Bluetooth will not be available")
                    return
                }
            ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED -> {
                Timber.i("BLE: BLUETOOTH_CONNECT permission not granted, Bluetooth will not be available")
                return
            }
            else -> Timber.i("BLE: Bluetooth prerequisites satisfied")
        }

        manager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (manager?.adapter?.bluetoothLeAdvertiser == null) {
            Timber.i("BLE: Bluetooth adapter not available")
            return
        }
        advertiser = manager!!.adapter.bluetoothLeAdvertiser
        scanner = manager!!.adapter.bluetoothLeScanner

        val isCodedPHY = vectorPreferences.p2pBLECodedPhy() && manager!!.adapter.isLeCodedPhySupported

        advertiser.stopAdvertising(advertiseCallback)
        advertiser.stopAdvertisingSet(advertiseSetCallback)
        scanner.stopScan(scanCallback)

        connecting.clear()
        connections.clear()
        conduits.clear()

        val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(serviceUUID)
                .build()

        l2capServer = manager!!.adapter.listenUsingInsecureL2capChannel()
        l2capPSM = intToBytes(l2capServer.psm.toShort())

        if (isCodedPHY) {
            val parameters = AdvertisingSetParameters.Builder()
                    .setLegacyMode(false)
                    .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                    .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MAX)
                    .setConnectable(true)

            if (isCodedPHY) {
                parameters.setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
                parameters.setSecondaryPhy(BluetoothDevice.PHY_LE_1M)
                Toast.makeText(applicationContext, "Requesting Coded PHY + 1M PHY", Toast.LENGTH_SHORT).show()
            }

            advertiser.startAdvertisingSet(parameters.build(), advertiseData, null, null, null, advertiseSetCallback)
        } else {
            val advertiseSettings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setTimeout(0)
                    .setConnectable(true)
                    .build()

            advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }

        gattCharacteristic = BluetoothGattCharacteristic(psmUUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        gattService.addCharacteristic(gattCharacteristic)

        gattServer = manager!!.openGattServer(applicationContext, gattServerCallback)
        gattServer.addService(gattService)

        val scanFilter = ScanFilter.Builder()
                .setServiceUuid(serviceUUID)
                .build()

        val scanFilters: MutableList<ScanFilter> = ArrayList()
        scanFilters.add(scanFilter)

        val scanSettingsBuilder = ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)

        if (isCodedPHY) {
            scanSettingsBuilder.setPhy(BluetoothDevice.PHY_LE_CODED)
        } else {
            scanSettingsBuilder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        }

        val scanSettings = scanSettingsBuilder.build()

        scanner.startScan(scanFilters, scanSettings, scanCallback)

        GlobalScope.launch {
            while (true) {
                Timber.i("BLE: Waiting for connection on PSM ${l2capServer.psm}")
                try {
                    val remote = l2capServer.accept() ?: continue
                    val device = remote.remoteDevice.address.toString()

                    val connection = connections[device]
                    if (connection != null) {
                        if (connection.isConnected) {
                            connection.close()
                        } else {
                            connections.remove(device)
                            conduits.remove(device)
                        }
                    }

                    Timber.i("BLE: Connected inbound $device PSM $l2capPSM")

                    Timber.i("Creating DendriteBLEPeering")
                    val conduit = monolith!!.conduit("ble", Gobind.PeerTypeBluetooth)
                    conduits[device] = conduit
                    connections[device] = DendriteBLEPeering(conduit, remote)
                    Timber.i("Starting DendriteBLEPeering")
                    connections[device]!!.start()

                    Timber.i("BLE: Created BLE peering with $device PSM $l2capPSM")
                    connecting.remove(device)
                } catch (e: Exception) {
                    Timber.i("BLE: Accept exception: ${e.message}")
                }
            }
        }
    }

    private fun stopBluetooth() {
        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        if (manager?.adapter?.isEnabled == true) {
            advertiser.stopAdvertising(advertiseCallback)
            advertiser.stopAdvertisingSet(advertiseSetCallback)
            scanner.stopScan(scanCallback)
        }

        connections.forEach { (_, c) ->
            if (c.isConnected) {
                c.close()
            }
        }

        connecting.clear()
        connections.clear()
        conduits.clear()
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
}

class NetworkCallback : gobind.InterfaceRetriever {
    private var interfaceCache: MutableList<gobind.InterfaceInfo> = mutableListOf<gobind.InterfaceInfo>()

    override public fun cacheCurrentInterfaces(): Long {
        interfaceCache.clear()

        for (iface in NetworkInterface.getNetworkInterfaces()) {
            val addrs = StringBuilder("")
            for (ia in iface.interfaceAddresses) {
                val parts: List<String> = ia.toString().split("/")
                if (parts.size > 1) {
                    addrs.append(java.lang.String.format(Locale.ROOT, "%s/%d ", parts[1], ia.networkPrefixLength))
                }
            }

            var ifaceInfo = gobind.InterfaceInfo()
            ifaceInfo.setName(iface.name)
            ifaceInfo.setIndex(iface.index.toLong())
            ifaceInfo.setMtu(iface.mtu.toLong())
            ifaceInfo.setUp(iface.isUp)
            ifaceInfo.setBroadcast(iface.supportsMulticast())
            ifaceInfo.setLoopback(iface.isLoopback)
            ifaceInfo.setPointToPoint(iface.isPointToPoint)
            ifaceInfo.setMulticast(iface.supportsMulticast())
            ifaceInfo.setAddrs(addrs.toString())

            interfaceCache.add(ifaceInfo)
        }
        return interfaceCache.size.toLong()
    }

    override public fun getCachedInterface(index: Long): InterfaceInfo? {
        if (index >= interfaceCache.size) {
            return null
        }

        return interfaceCache[index.toInt()]
    }
}
