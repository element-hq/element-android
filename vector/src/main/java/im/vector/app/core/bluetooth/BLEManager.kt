/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcel
import android.os.ParcelUuid
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import gobind.Conduit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

typealias LastConnectionTimeMS = Long

sealed class BLEManagerCommand
// Registers this public key : device association, and removes the BLEClient associated with this device.
// Called when either a new or duplicate public key : device association is discovered.
class BLEManagerRegisterDevice(val publicKey: PublicKey, val device: BluetoothDevice) : BLEManagerCommand()

// Unregisters the BLEClient.
// Called after a duplicate public key : device association is registered.
class BLEManagerUnregisterClient(val device: BluetoothDevice) : BLEManagerCommand()

// Unregisters all device state associated with this public key, including the BLEClient.
// Called after the original BLEClient for a device stops.
class BLEManagerClearDeviceState(val publicKey: PublicKey) : BLEManagerCommand()

class BLEManager(
        private val publicKey: PublicKey,
        private val bluetoothManager: BluetoothManager,
        private val context: Context,
        private val pineconeConnect: () -> Conduit?,
        private val pineconeDisconenct: (Conduit) -> Unit,
) {
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    private lateinit var scanSettings: ScanSettings
    private val scanFilters: MutableList<ScanFilter> by lazy {
        val filters = ArrayList<ScanFilter>()
        val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BLEConstants.SERVICE_UUID))
                .build()
        filters.add(scanFilter)
        filters
    }

    private var started = AtomicBoolean(false)
    private var scanning = AtomicBoolean(false)
    private var codedPHY = false

    private var deviceBackoff = ConcurrentHashMap<DeviceAddress, LastConnectionTimeMS>()

    // NOTE: This channel is used to provide a communication means from the BLEClients/BLEServer
    // back to the BLEManager.
    private var clientChannel = Channel<BLEManagerCommand>(Channel.BUFFERED)

    private var bleServer: BLEServer? = null

    // NOTE: All 3 maps below are protected by this mutex
    private val bleClientsMutex = Mutex()
    private val bleClients = HashMap<BluetoothDevice, BLEClient>()
    private val blePublicKeys = HashMap<PublicKey, MutableList<BluetoothDevice>>()
    private val bleConnectedDevices = HashMap<BluetoothDevice, PublicKey>()

    private suspend fun registerClient(client: BLEClient, device: BluetoothDevice) {
        bleClientsMutex.withLock {
            bleClients[device] = client
        }
    }

    private suspend fun unregisterClient(device: BluetoothDevice) {
        bleClientsMutex.withLock {
            bleClients.remove(device)
        }
    }

    private suspend fun clearDeviceState(publicKey: PublicKey) {
        bleClientsMutex.withLock {
            val devices = blePublicKeys[publicKey]
            devices?.forEach {
                bleClients.remove(it)
                bleConnectedDevices.remove(it)
            }

            blePublicKeys.remove(publicKey)
        }
    }

    private suspend fun clearAllDeviceState() {
        bleClientsMutex.withLock {
            bleClients.clear()
            blePublicKeys.clear()
            bleConnectedDevices.clear()
            deviceBackoff.clear()
        }
    }

    private suspend fun isKnownDevice(device: BluetoothDevice): Boolean {
        bleClientsMutex.withLock {
            if (bleClients.containsKey(device) || bleConnectedDevices.containsKey(device)) {
                return true
            }
            return false
        }
    }

    private suspend fun registerDevice(publicKey: PublicKey, device: BluetoothDevice) {
        bleClientsMutex.withLock {
            if (!blePublicKeys.containsKey(publicKey)) {
                blePublicKeys[publicKey] = mutableListOf<BluetoothDevice>()
            }
            blePublicKeys[publicKey]!!.add(device)
            bleConnectedDevices[device] = publicKey
        }
    }

    suspend fun isKnownKey(publicKey: PublicKey): Boolean {
        bleClientsMutex.withLock {
            if (blePublicKeys.containsKey(publicKey)) {
                return true
            }
            return false
        }
    }

    companion object {
        private const val TAG = "BLEManager"
        private const val CONNECTION_RETRY_DELAY_MS = 2000L
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun start(isCodedPHY: Boolean) = CoroutineScope(Dispatchers.Default).launch {
        if (!started.getAndSet(true)) {
            Timber.i("$TAG: Starting")
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("$TAG: Insufficient permissions to start scanning, aborting start")
                return@launch
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("$TAG: Insufficient permissions to start advertising, aborting start")
                return@launch
            }

            codedPHY = isCodedPHY

            // Start BLEServer
            bleServer = BLEServer(context, bluetoothManager, bluetoothAdapter, publicKey, pineconeConnect, pineconeDisconenct)
            bleServer?.start(codedPHY)

            // Start Scanning
            updateBleScanSettings()
            startBleScan()

            // Block handling BLEClient/s
            for (command in clientChannel) {
                when(command) {
                    is BLEManagerClearDeviceState -> {
                        Timber.i("$TAG: Clearing device state for ${command.publicKey}")
                        clearDeviceState(command.publicKey)
                    }
                    is BLEManagerRegisterDevice -> {
                        Timber.i("$TAG: Registering device ${command.device} for ${command.publicKey}")
                        registerDevice(command.publicKey, command.device)
                    }
                    is BLEManagerUnregisterClient -> {
                        Timber.i("$TAG: Unregistering client for ${command.device}")
                        unregisterClient(command.device)
                    }
                }
            }
            Timber.i("$TAG: Stopped processing client stop events")
        } else {
            Timber.w("$TAG: Already started")
        }
    }

    fun stop() {
        Timber.i("$TAG: Stopping")
        // Stop Scanning
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            stopBleScan()
        }

        // Stop BLEServer
        bleServer?.stop()
        bleServer = null

        // Stop BLEClient/s
        // NOTE: Stop BLEClients after stopping scanning to ensure no new clients are created later.
        clientChannel.close()
        for (bleClient in bleClients) {
            Timber.i("$TAG: Stopping device ${bleClient.key.address}")
            runBlocking {
                bleClient.value.stop(true)
            }
        }
        runBlocking {
            clearAllDeviceState()
        }
        clientChannel = Channel(Channel.BUFFERED)
        started.set(false)
        Timber.i("$TAG: Stopped")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startBleScan() {
        if (!scanning.getAndSet(true)) {
            Timber.i("$TAG: Starting scan")
            bleScanner.startScan(scanFilters, scanSettings, scanCallback)
            // TODO: restart before 30min timeout
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopBleScan() {
        Timber.i("$TAG: Stopping scan")
        scanning.set(false)
        bleScanner.stopScan(scanCallback)
        // NOTE: Stopping the scan can still result in callbacks happening later
        // So we flush pending results with `scanning` set to false to ensure they
        // are all cleared.
        bleScanner.flushPendingScanResults(scanCallback)
    }

    private fun updateBleScanSettings() {
        val scanSettingsBuilder = ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0)

        if (codedPHY) {
            scanSettingsBuilder.setPhy(BluetoothDevice.PHY_LE_CODED)
        } else {
            scanSettingsBuilder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        }
        scanSettings = scanSettingsBuilder.build()
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            if (errorCode != SCAN_FAILED_ALREADY_STARTED) {
                Timber.i("$TAG: Scanning failed, already started: $errorCode")
            } else {
                Timber.e("$TAG: Scanning failed, code: $errorCode")
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!scanning.get()) {
                return
            }
            if (!result.isConnectable || result.scanRecord?.serviceUuids?.contains(ParcelUuid(BLEConstants.SERVICE_UUID)) != true) {
                return
            }
            deviceBackoff[result.device.address]?.let {
                if (SystemClock.elapsedRealtime() - it < CONNECTION_RETRY_DELAY_MS) {
                    return
                }
            }

            var isKnown = false
            runBlocking {
                if (isKnownDevice(result.device)) {
                    isKnown = true
                }
            }
            if (isKnown) {
                return
            }

            val key = result.device.address.toString()
            Timber.i("$TAG: Scan result found $key")

            deviceBackoff[result.device.address] = SystemClock.elapsedRealtime()

            val shortKey = publicKey.toPublicKeyBytes().slice(0..7).toByteArray().toPublicKey()
            Timber.e("$TAG: Our Key: $shortKey")
            val client = BLEClient(
                    publicKey,
                    result.device,
                    context,
                    clientChannel,
                    ::isKnownKey,
                    pineconeConnect,
                    pineconeDisconenct,
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            runBlocking {
                registerClient(client, result.device)
                client.start()
            }
        }
    }
}
