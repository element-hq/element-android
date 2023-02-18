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
import android.bluetooth.BluetoothGatt
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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import gobind.Conduit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class BLEServer(
        private val context: Context,
        private val bluetoothManager: BluetoothManager,
        private val bluetoothAdapter: BluetoothAdapter,
        private val publicKey: PublicKey,
        private val pineconeConnect: () -> Conduit?,
        private val pineconeDisconenct: (Conduit) -> Unit,
) {
    private val bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
    companion object {
        private const val TAG = "BLEServer"
    }

    private var pineconePeers = ConcurrentHashMap<DeviceAddress, BLEPineconePeer>()
    private var connectedDevices = ConcurrentHashMap<DeviceAddress, BluetoothDevice>()

    private lateinit var gattServer: BluetoothGattServer
    private lateinit var gattCharacteristic: BluetoothGattCharacteristic
    private val gattService = BluetoothGattService(BLEConstants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
    private lateinit var l2capServer: BluetoothServerSocket
    private lateinit var l2capSocketServer: Job
    private lateinit var psmAndPublicKey: ByteArray

    private val gattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            Timber.i("$TAG: Received characteristic read request from ${device.address}")
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, psmAndPublicKey);
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Timber.i("$TAG: Device connection state changed ${device.address}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Timber.i("$TAG: Device connected: ${device.address}")
                    handleDeviceConnected(device)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Timber.i("$TAG: Device disconnected: ${device.address}")
                    // TODO: I get these every ~7s from iOS... why?
                    // Only if I have android scanning enabled.
                    // The l2cap socket works just fine...
                    handleDeviceDisconnected(device)
                }
            } else {
                Timber.i("$TAG: Device disconnected: ${device.address} with status: $status")
                handleDeviceDisconnected(device)
            }
        }
    }

    @Synchronized
    fun handleDeviceConnected(device: BluetoothDevice) {
        if (connectedDevices.containsKey(device.address)) {
            return
        }

        Timber.i("$TAG: Handling connected pinecone peer for ${device.address}")
        // Calling connect here apparently tells android that we are
        // using this connection. Whatever that means?
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer.connect(device, false)
        }
        connectedDevices[device.address] = device
    }

    fun handleDeviceDisconnected(device: BluetoothDevice) {
        Timber.i("$TAG: Closing pinecone peer for ${device.address}")
        pineconePeers[device.address]?.let {
            it.close()
        }
        pineconePeers.remove(device.address)
        connectedDevices[device.address]?.let {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gattServer.cancelConnection(it)
            }
        }
        connectedDevices.remove(device.address)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun start(isCodedPHY: Boolean) {
        Timber.i("$TAG: Starting")

        l2capServer = bluetoothAdapter.listenUsingInsecureL2capChannel()
        Timber.w("$TAG: Server PSM: ${l2capServer.psm}")
        psmAndPublicKey = shortToBytes(l2capServer.psm.toShort()) + publicKey.toPublicKeyBytes().slice(0 until BLEConstants.PSM_CHARACTERISTIC_KEY_SIZE).toByteArray()

        gattCharacteristic = BluetoothGattCharacteristic(BLEConstants.PSM_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        gattService.addCharacteristic(gattCharacteristic)

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        gattServer.addService(gattService)

        // Start Advertising
        // NOTE: Start advertising after gattServer and l2capSocket are listening
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Timber.e("$TAG: Insufficient permissions to start advertising, aborting start")
            return
        }
        startAdvertising(isCodedPHY)

        l2capSocketServer = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                Timber.i("$TAG: Waiting for connection on PSM ${l2capServer.psm}")
                try {
                    val remote = try {
                        l2capServer.accept()
                    } catch (e: CancellationException) {
                        Timber.i("$TAG: Socket server cancelled, exiting")
                        return@launch
                    } catch (e: Exception) {
                        Timber.i("$TAG: Socket server cancelled, exiting: ${e.message}")
                        //Timber.i("$TAG: Socket server exception: ${e.message}, continuing")
                        return@launch
                    }
                    val device = remote.remoteDevice.address.toString()

                    Timber.i("$TAG: Connected inbound $device PSM ${l2capServer.psm}")
                    gattServerCallback.onConnectionStateChange(remote.remoteDevice, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)

                    Timber.i("$TAG: Creating BLEPineconePeer")
                    pineconeConnect()?.let { newConduit ->
                        Timber.i("$TAG: Successful inbound pinecone peering with $device PSM ${l2capServer.psm}")
                        val stopCallback =  { gattServerCallback.onConnectionStateChange(remote.remoteDevice, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED) }
                        pineconePeers[remote.remoteDevice.address] = BLEPineconePeer(remote.remoteDevice.address, newConduit, remote, pineconeDisconenct, stopCallback)
                    } ?: run {
                        Timber.e("$TAG: Failed pinecone peering with $device PSM ${l2capServer.psm}")
                    }

                } catch (e: Exception) {
                    Timber.i("$TAG: Accept exception: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        Timber.i("$TAG: Stopping")

        l2capServer.close()
        runBlocking {
            try {
                l2capSocketServer.cancelAndJoin()
                Timber.i("$TAG: L2CAP socket server stopped")
            } catch (_: Exception) {}
        }

        for (peer in pineconePeers) {
            peer.value.close()
        }
        pineconePeers.clear()
        connectedDevices.clear()

        // Stop Advertising
        // NOTE: Stop advertising before stopping gattServer/l2capSockets to minimize new
        // nodes trying to connect to gattServer or l2capSocket after this point.
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            stopAdvertising()
        }

        gattServer.clearServices()
        gattServer.close()
        Timber.i("$TAG: Stopped")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startAdvertising(isCodedPHY: Boolean) {
        Timber.i("$TAG: Starting advertise")
        val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(BLEConstants.SERVICE_UUID))
                .build()

        if (isCodedPHY) {
            val parameters = AdvertisingSetParameters.Builder()
                    .setLegacyMode(false)
                    .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                    .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MAX)
                    .setConnectable(true)

            parameters.setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
            parameters.setSecondaryPhy(BluetoothDevice.PHY_LE_1M)

            bleAdvertiser.startAdvertisingSet(parameters.build(), advertiseData, null, null, null, advertiseSetCallback)
        } else {
            val advertiseSettings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setTimeout(0)
                    .setConnectable(true)
                    .build()

            bleAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun stopAdvertising() {
        Timber.i("$TAG: Stopping advertise")
        bleAdvertiser.stopAdvertising(advertiseCallback)
        bleAdvertiser.stopAdvertisingSet(advertiseSetCallback)
    }

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            if (errorCode != ADVERTISE_FAILED_ALREADY_STARTED) {
                Timber.e("$TAG: Advertise failed, already started: $errorCode")
            } else {
                Timber.e("$TAG: Advertise failed, code: $errorCode")
            }
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.i("$TAG: Advertise started")
        }
    }

    private val advertiseSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == ADVERTISE_SUCCESS) {
                Timber.i("$TAG: AdvertiseSet started")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            Timber.i("$TAG: AdvertiseSet stopped")
        }
    }
}

private fun shortToBytes(x: Short): ByteArray {
    val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Short.BYTES)
    buffer.putShort(x)
    return buffer.array()
}
