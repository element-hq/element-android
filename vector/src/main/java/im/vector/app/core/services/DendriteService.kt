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
import android.bluetooth.BluetoothManager
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import gobind.Conduit
import gobind.DendriteMonolith
import gobind.Gobind
import gobind.InterfaceInfo
import im.vector.app.R
import im.vector.app.core.bluetooth.BLEManager
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class DendriteService : VectorAndroidService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var vectorPreferences: VectorPreferences
    private var serviceStarted = AtomicBoolean(false)

    // Notifications
    private val disableNotifications = AtomicBoolean(false)
    private var notification: Notification? = null
    private val notificationManager: NotificationManager by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    // Dendrite
    private val binder = DendriteLocalBinder()
    private var monolith: DendriteMonolith? = null

    // LAN
    private lateinit var multicastLock: WifiManager.MulticastLock
    private var networkCallback = NetworkCallback()

    // Bluetooth
    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    private var bleManager: BLEManager? = null
    private val bleReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return
            }
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        stopBluetooth()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> return
                    BluetoothAdapter.STATE_ON -> {
                        startBluetooth()
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> return
                }
            }
        }
    }

    companion object {
        const val TAG = "DendriteService"
        const val BLE_TAG = "DendriteService - BLE"
        const val ACTION = "ACTION"
        const val ACTION_START = "ACTION_START"
        private const val ID = 532345
        private const val CHANNEL_ID = "im.vector.p2p"
        private const val CHANNEL_NAME = "Element P2P"
    }

    inner class DendriteLocalBinder : Binder() {
        fun getService() : DendriteService {
            return this@DendriteService
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.getStringExtra(ACTION);
        Timber.i("$TAG: Action: $action")
        when (action) {
            ACTION_START -> {
                startService()
            }
        }
        return START_NOT_STICKY
    }

    private fun startService() {
        if (serviceStarted.getAndSet(true)) {
            // NOTE: Retry starting bluetooth if not already started.
            Timber.i("$BLE_TAG: Retrying bluetooth restart")
            startBluetooth()
            return
        }

        // post notification belonging to this service
        startForeground(ID, serviceNotification())

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bleReceiver, intentFilter)

        vectorPreferences = singletonEntryPoint().vectorPreferences()
        vectorPreferences.subscribeToChanges(this)

        monolith = gobind.DendriteMonolith()
        monolith?.storageDirectory = applicationContext.filesDir.toString()
        monolith?.cacheDirectory = applicationContext.cacheDir.toString()
        monolith?.start()

        if (vectorPreferences.p2pEnableStatic()) {
            monolith?.setStaticPeer(vectorPreferences.p2pStaticURI())
        }
        monolith?.setRelayingEnabled(vectorPreferences.p2pRelayingEnabled())

        val selfRelayURI = monolith?.getRelayServers(monolith?.publicKey()) ?: ""
        vectorPreferences.p2pSetSelfRelayURI(selfRelayURI)

        monolith?.registerNetworkCallback(networkCallback)

        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("Element.P2P")
        val enabled = vectorPreferences.p2pEnableMulticast()
        if (enabled) {
            multicastLock.acquire()
        }
        monolith?.setMulticastEnabled(enabled)

        startBluetooth()

        Timer().schedule(object : TimerTask() {
            override fun run() {
                updateNotification()
            }
        }, 0, 1000)
    }

    override fun onDestroy() {
        Timber.i("$TAG: onDestroy")
        super.onDestroy()
        onTaskRemoved(null)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.i("$TAG: onTaskRemoved")
        Timber.i("$TAG: Stopping")
        super.onTaskRemoved(rootIntent)
        if (serviceStarted.getAndSet(false)) {
            disableNotifications.set(true)

            // Occurs when the element app is closed from the system tray
            unregisterReceiver(bleReceiver)
            stopBluetooth()

            monolith?.stop()
            monolith = null

            notificationManager.cancelAll()
            stopForeground(STOP_FOREGROUND_REMOVE)

            myStopSelf()
            Timber.i("$TAG: Stopped")

            // NOTE: If DendriteService stops, stop the entire process.
            exitProcess(0)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.i("$BLE_TAG: Preferences changed....")
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
                monolith?.setMulticastEnabled(enabled)
            }

            VectorPreferences.SETTINGS_P2P_ENABLE_BLUETOOTH -> {
                val enabled = vectorPreferences.p2pEnableBluetooth()
                if (enabled) {
                    startBluetooth()
                } else {
                    stopBluetooth()
                    monolith?.disconnectType(Gobind.PeerTypeBluetooth)
                }
            }

            VectorPreferences.SETTINGS_P2P_BLE_CODED_PHY -> {
                // TODO: does this work? What about if (!enabled)?
                val enabled = vectorPreferences.p2pEnableBluetooth()
                if (enabled) {
                    stopBluetooth()
                    monolith?.disconnectType(Gobind.PeerTypeBluetooth)
                    startBluetooth()
                }
            }

            VectorPreferences.SETTINGS_P2P_ENABLE_STATIC -> {
                val enabled = vectorPreferences.p2pEnableStatic()
                if (enabled) {
                    val uri = vectorPreferences.p2pStaticURI()
                    monolith?.setStaticPeer(uri)
                } else {
                    monolith?.setStaticPeer("")
                }
            }

            VectorPreferences.SETTINGS_P2P_STATIC_URI -> {
                if (vectorPreferences.p2pEnableStatic()) {
                    val uri = vectorPreferences.p2pStaticURI()
                    monolith?.setStaticPeer(uri)
                }
            }

            VectorPreferences.SETTINGS_P2P_ENABLE_RELAYING -> {
                monolith?.setRelayingEnabled(vectorPreferences.p2pRelayingEnabled())
            }

            VectorPreferences.SETTINGS_P2P_SELF_RELAY_URI -> {
                val uri = vectorPreferences.p2pSelfRelayURI()
                monolith?.setRelayServers(monolith?.publicKey(), uri)
            }
        }
    }

    fun registerUser(userID: String, password: String): String {
        return monolith?.registerUser(userID, password) ?: ""
    }

    fun registerDevice(userID: String): String {
        return monolith?.registerDevice(userID, "P2P") ?: ""
    }

    fun assignRelayServers(userID: String, relayServers: String) {
        monolith?.setRelayServers(userID, relayServers)
    }

    fun getRelayServers(userID: String): String {
        return monolith?.getRelayServers(userID) ?: ""
    }

    private fun pineconeConnect(): Conduit? {
        var conduit: Conduit? = null
        try {
            conduit = monolith?.conduit("ble", Gobind.PeerTypeBluetooth)
        } catch (_: Exception) {}
        return conduit
    }

    private fun pineconeDisconnect(conduit: Conduit) {
        val port = conduit.port()
        if (port > 0) {
            try {
                monolith?.disconnectPort(port)
            } catch (_: Exception) {}
        }
    }

    private fun updateNotification() {
        if (disableNotifications.get()) {
            return
        }
        val remotePeers = monolith?.peerCount(Gobind.PeerTypeRemote)?.toInt() ?: 0
        val multicastPeers = monolith?.peerCount(Gobind.PeerTypeMulticast)?.toInt() ?: 0
        val bluetoothPeers = monolith?.peerCount(Gobind.PeerTypeBluetooth)?.toInt() ?: 0

        val title = "P2P Matrix service running"

        val text: String = if (remotePeers+multicastPeers+bluetoothPeers == 0) {
            "No connectivity"
        } else {
            val texts = mutableListOf<String>()
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
        notificationManager.notify(ID, notification)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            )
        }
    }

    private fun startBluetooth() {
        when {
            !bluetoothAdapter.isEnabled -> {
                Timber.w("$BLE_TAG: Bluetooth disabled, not starting")
                return
            }
            !vectorPreferences.p2pEnableBluetooth() -> {
                Timber.w("$BLE_TAG: Bluetooth peerings not enabled")
                return
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                Timber.w("$BLE_TAG: Bluetooth peerings not supported on this version of Android")
                return
            }
            ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED -> {
                Timber.w("$BLE_TAG: BLUETOOTH_ADVERTISE permission not granted, Bluetooth will not be available")
                return
            }
            ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED -> {
                    Timber.w("$BLE_TAG: BLUETOOTH_SCAN permission not granted, Bluetooth will not be available")
                    return
                }
            ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED -> {
                Timber.w("$BLE_TAG: BLUETOOTH_CONNECT permission not granted, Bluetooth will not be available")
                return
            }
            bluetoothAdapter.bluetoothLeAdvertiser == null -> {
                Timber.w("$BLE_TAG: Bluetooth advertiser not available")
                return
            }
            else -> Timber.i("$BLE_TAG: Bluetooth prerequisites satisfied")
        }

        val publicKey: String
        if (monolith != null) {
            publicKey = monolith!!.publicKey()
        } else {
            Timber.e("$TAG: Dendrite isn't started, cannot start bluetooth")
            return
        }
        bleManager = BLEManager(publicKey, bluetoothManager, applicationContext, ::pineconeConnect, ::pineconeDisconnect)
        runBlocking {
            bleManager?.start(vectorPreferences.p2pBLECodedPhy() && bluetoothAdapter.isLeCodedPhySupported)
        }
    }

    private fun stopBluetooth() {
        bleManager?.stop()
        bleManager = null
    }
}

class NetworkCallback : gobind.InterfaceRetriever {
    private var interfaceCache: MutableList<gobind.InterfaceInfo> = mutableListOf<gobind.InterfaceInfo>()

    override fun cacheCurrentInterfaces(): Long {
        interfaceCache.clear()

        for (iface in NetworkInterface.getNetworkInterfaces()) {
            val addrs = StringBuilder("")
            for (ia in iface.interfaceAddresses) {
                val parts: List<String> = ia.toString().split("/")
                if (parts.size > 1) {
                    addrs.append(java.lang.String.format(Locale.ROOT, "%s/%d ", parts[1], ia.networkPrefixLength))
                }
            }

            val ifaceInfo = gobind.InterfaceInfo()
            ifaceInfo.name = iface.name
            ifaceInfo.index = iface.index.toLong()
            ifaceInfo.mtu = iface.mtu.toLong()
            ifaceInfo.up = iface.isUp
            ifaceInfo.broadcast = iface.supportsMulticast()
            ifaceInfo.loopback = iface.isLoopback
            ifaceInfo.pointToPoint = iface.isPointToPoint
            ifaceInfo.multicast = iface.supportsMulticast()
            ifaceInfo.addrs = addrs.toString()

            interfaceCache.add(ifaceInfo)
        }
        return interfaceCache.size.toLong()
    }

    override fun getCachedInterface(index: Long): InterfaceInfo? {
        if (index >= interfaceCache.size) {
            return null
        }

        return interfaceCache[index.toInt()]
    }
}
