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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import gobind.Conduit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class BLEClient(
        private val ourPublicKey: PublicKey,
        private val bleDevice: BluetoothDevice,
        private val context: Context,
        private val bleManagerChannel: SendChannel<BLEManagerCommand>,
        private val isKnownKey: suspend (PublicKey) -> Boolean,
        private val pineconeConnect: () -> Conduit?,
        private val pineconeDisconenct: (Conduit) -> Unit,
) {
    private val quitChannel = Channel<BLEClientQuit>()
    private var started = AtomicBoolean(false)
    private var shutdown = AtomicBoolean(false)

    private val commandChannel = Channel<BLEClientCommand>(Channel.BUFFERED)
    private var timeoutCommand: Job? = null
    private var pendingCommand: BLEClientCommand? = null
    private var pendingChannel: Channel<BLEClientResponse>? = null
    private var processingStopped: CompletableDeferred<Unit>? = CompletableDeferred<Unit>()

    private var gattClient: BluetoothGatt? = null
    private var psmCharacteristic: BluetoothGattCharacteristic? = null
    private var psmValue: Int? = null
    private var devicePublicKey: PublicKey? = null
    private var l2capSocket: BluetoothSocket? = null
    private var pineconePeering: BLEPineconePeer? = null

    private val tag = "BLEClient: ${bleDevice.address}"

    companion object {
        // General timeout for any bluetooth command
        private const val COMMAND_TIMEOUT_MS = 10000L
        // Delay to use before doing a gatt connect
        private const val DIRECT_CONNECTION_DELAY_MS = 100L
        // Delay to use after refreshing gatt cache
        private const val POST_GATT_REFRESH_DELAY_MS = 100L
    }

    inner class BLEClientQuit(val response: CompletableDeferred<Unit>)

    sealed class BLEClientCommand
    object BLEGattConnect : BLEClientCommand()
    object BLEGattDiscoverServices : BLEClientCommand()
    object BLEGattReadCharacteristic : BLEClientCommand()
    object BLEConnectL2CAP : BLEClientCommand()

    sealed class BLEClientResponse
    object GattConnectResponse : BLEClientResponse()
    class GattPSMServiceDiscovered(val characteristic: BluetoothGattCharacteristic) : BLEClientResponse()
    class GattReadCharacteristicResponse(val psm: Int, val publicKey: PublicKey) : BLEClientResponse()

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun start() = CoroutineScope(Dispatchers.Default).launch {
        if (!started.getAndSet(true)) {
            Timber.i("$tag: Starting")
            processCommands()
            commandChannel.send(BLEGattConnect)
            for (msg in quitChannel) {
                Timber.i("$tag: Received quit")
                stopProcessingCommands()
                cleanupBLEResources()
                quitChannel.close()
                msg.response.complete(Unit)
                break
            }
            Timber.i("$tag: Stopped")
            try {
                devicePublicKey?.let {
                    Timber.i("$tag: Unregister all device state with BLEManager")
                    bleManagerChannel.send(BLEManagerClearDeviceState(it))
                } ?: run {
                    Timber.i("$tag: Unregister this BLEClient with BLEManager")
                    bleManagerChannel.send(BLEManagerUnregisterClient(bleDevice))
                }
            } catch (_: Exception) {}
        } else {
            Timber.w("$tag: Already started")
        }
    }

    suspend fun stop(waitForShutdown: Boolean) {
        Timber.i("$tag: Stopping")
        if(shutdown.getAndSet(true)) {
            Timber.w("$tag: Already shutting down")
            return
        }

        Timber.i("$tag: Sending quit to channel")
        val response = CompletableDeferred<Unit>()
        try {
            quitChannel.send(BLEClientQuit(response))
        } catch (_: Exception) {
            response.complete(Unit)
        }
        Timber.i("$tag: Sent quit to channel")

        if (waitForShutdown) {
            response.await()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun cleanupBLEResources() {
        Timber.i("$tag: Cleaning up BLE resources")
        pineconePeering?.let {
            it.close()
        }

        try {
            l2capSocket?.close()
        } catch (_: Exception) {}
        l2capSocket = null

        try {
            runBlocking {
                clearServicesCache()
            }
            gattClient?.disconnect()
            gattClient?.close()
        } catch (_: Exception) {}
        gattClient = null
    }

    private suspend fun clearServicesCache() {
        if (gattClient == null) return
        try {
            val refreshMethod = gattClient?.javaClass?.getMethod("refresh")
            refreshMethod?.invoke(gattClient)
        } catch (e: Exception) {
            Timber.e("$tag: Refreshing services cache failed")
        }
        delay(POST_GATT_REFRESH_DELAY_MS)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun processCommands() = CoroutineScope(Dispatchers.Default).launch {
        for (command in commandChannel) {
            if (!shutdown.get()) {
                Timber.i("$tag: Processing internal command ${command::class.simpleName}")
                handleCommand(command)
            } else {
                Timber.i("$tag: Exiting command processing loop")
                break
            }
            Timber.i("$tag: Waiting for next command")
        }
        processingStopped?.complete(Unit)
        Timber.i("$tag: Command handler stopped")
    }

    private suspend fun stopProcessingCommands() {
        Timber.i("$tag: Command handler received quit")
        cancelCommandTimer()
        commandChannel.close()
        pendingChannel?.close()
        pendingChannel = null
        processingStopped?.await()
        processingStopped = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun handleCommand(command: BLEClientCommand) {
        when (command) {
            is BLEGattConnect -> {
                pendingCommand = BLEGattConnect
                pendingChannel = Channel<BLEClientResponse>()
                delay(DIRECT_CONNECTION_DELAY_MS)
                try {
                    gattClient = bleDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                    Timber.i("$tag: Waiting for connect response")
                    startCommandTimer(COMMAND_TIMEOUT_MS)
                    val response = pendingChannel?.receive()
                    Timber.i("$tag: Received connect response")
                    cancelCommandTimer()
                    pendingCommand = null
                    pendingChannel = null
                    if (response is GattConnectResponse) {
                        Timber.i("$tag: Connected to device")
                        commandChannel.send(BLEGattDiscoverServices)
                    } else {
                        Timber.e("$tag: Received unexpected connect response")
                        stop(false)
                    }
                } catch (_: Exception) {}
            }
            is BLEGattDiscoverServices -> {
                pendingCommand = BLEGattDiscoverServices
                pendingChannel = Channel<BLEClientResponse>()
                gattClient?.discoverServices() ?: run {
                    Timber.e("$tag: Gatt not assigned yet")
                    stop(false)
                }
                try {
                    Timber.i("$tag: Waiting for discover response")
                    startCommandTimer(COMMAND_TIMEOUT_MS)
                    val response = pendingChannel?.receive()
                    Timber.i("$tag: Received discover response")
                    cancelCommandTimer()
                    pendingCommand = null
                    pendingChannel = null
                    if (response is GattPSMServiceDiscovered) {
                        Timber.i("$tag: Discovered psm characteristic")
                        psmCharacteristic = response.characteristic
                        commandChannel.send(BLEGattReadCharacteristic)
                    } else {
                        Timber.e("$tag: Received unexpected discover response")
                        stop(false)
                    }
                } catch (_: Exception) {}
            }
            is BLEGattReadCharacteristic -> {
                pendingCommand = BLEGattReadCharacteristic
                pendingChannel = Channel<BLEClientResponse>()
                psmCharacteristic?.let {
                    gattClient?.readCharacteristic(psmCharacteristic) ?: run {
                        Timber.e("$tag: Gatt not assigned yet")
                        stop(false)
                    }
                } ?: run {
                    Timber.e("$tag: psm characteristic not assigned yet")
                    stop(false)
                }

                try {
                    Timber.i("$tag: Waiting for read characteristic response")
                    startCommandTimer(COMMAND_TIMEOUT_MS)
                    val response = pendingChannel?.receive()
                    Timber.i("$tag: Received read characteristic response")
                    cancelCommandTimer()
                    pendingCommand = null
                    pendingChannel = null
                    if (response is GattReadCharacteristicResponse) {
                        Timber.i("$tag: Received psm of: ${response.psm} for ${response.publicKey}")
                        psmValue = response.psm
                        val key = response.publicKey

                        if (isKnownKey(key)) {
                            Timber.w("$tag: already associated with connected key: $key")
                            // NOTE: Register the device after checking for known key
                            bleManagerChannel.send(BLEManagerRegisterDevice(key, bleDevice))
                            stop(false)
                            return
                        }

                        // NOTE: Register the device or it won't get cleaned up properly later
                        bleManagerChannel.send(BLEManagerRegisterDevice(key, bleDevice))

                        if (key.compareTo(ourPublicKey.toPublicKeyBytes().slice(0 until BLEConstants.PSM_CHARACTERISTIC_KEY_SIZE).toByteArray().toPublicKey(), true) <= 0) {
                            Timber.w("$tag: Not connecting to device with lower key: $key")
                            stop(false)
                            return
                        }

                        Timber.i("$tag: Initiating connection to device with key: $key")
                        // NOTE: Assign key after checks so correct command is sent to BLEManager
                        devicePublicKey = key
                        commandChannel.send(BLEConnectL2CAP)
                    } else {
                        Timber.e("$tag: Received unexpected read characteristic response")
                        stop(false)
                    }
                } catch (_: Exception) {}
            }
            is BLEConnectL2CAP -> {
                psmValue?.let {
                    try {
                        l2capSocket = gattClient?.device?.createInsecureL2capChannel(it)
                    } catch (e: Exception) {
                        Timber.e("$tag: Failed creating socket")
                        stop(false)
                    }

                    try {
                        l2capSocket?.connect()
                        Timber.i("$tag: Successfully connected outbound to PSM $psmValue")
                        pineconeConnect()?.let { newConduit ->
                            Timber.i("$tag: Successful outbound pinecone connection")
                            val stopCallback = { runBlocking { stop(false) } }
                            pineconePeering = BLEPineconePeer(bleDevice.address, newConduit, l2capSocket!!, pineconeDisconenct, stopCallback)
                        } ?: run {
                            Timber.e("$tag: Failed pinecone connection")
                            stop(false)
                        }
                    } catch (e: Exception) {
                        Timber.e("$tag: Failed to connect to PSM $psmValue: ${e.toString()}")
                        try {
                            l2capSocket?.close()
                        } catch (e: Exception) {
                            Timber.w("$tag: Exception closing socket to PSM $psmValue: ${e.toString()}")
                        }
                        stop(false)
                    }
                } ?: run {
                    Timber.e("$tag: Trying to connect without psm value assigned")
                }
            }
        }
    }

    private fun startCommandTimer(timeoutMS: Long) {
        cancelCommandTimer()

        timeoutCommand = CoroutineScope(Dispatchers.Default).launch {
            delay(timeoutMS)
            Timber.e("$tag: Command timeout reached")
            stop(false)
        }
    }

    private fun cancelCommandTimer() {
        timeoutCommand?.cancel()
        timeoutCommand = null
    }

    // NOTE: All callbacks are performed on `Binder` threads and will block further callbacks
    // from happening. To limit this, return from the callback and stop blocking as soon as possible.
    // We do this by sending messages to a channel so we don't block while performing the next action.
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.i("$tag: Received onConnectionStateChanged")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTING -> {
                        Timber.i("$tag: Connecting")
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        Timber.i("$tag: Disconnecting")
                    }
                    BluetoothProfile.STATE_CONNECTED -> {
                        Timber.i("$tag: Successfully Connected")
                        if (pendingCommand is BLEGattConnect) {
                            runBlocking {
                                pendingChannel?.send(GattConnectResponse)
                            }
                        }
                    }
                    BluetoothGatt.STATE_DISCONNECTED -> {
                        Timber.i("$tag: Successfully Disconnected")
                        runBlocking {
                            stop(false)
                        }
                    }
                }
            } else {
                Timber.e("$tag: onConnectionStateChanged Got GATT code $status")
                runBlocking {
                    stop(false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Timber.e("$tag: onServicesDiscovered not successful, status code: $status")
                runBlocking {
                    stop(false)
                }
                return
            }

            if (pendingCommand !is BLEGattDiscoverServices) {
                Timber.w("$tag: Received onServicesDiscovered while waiting for response to ${pendingCommand!!::class.simpleName}")
                return
            }

            for (service in gatt.services) {
                if (service.uuid == BLEConstants.SERVICE_UUID) {
                    for (characteristic in service.characteristics) {
                        if (characteristic.uuid == BLEConstants.PSM_UUID) {
                            Timber.i("$tag: Found psm characteristic")
                            runBlocking {
                                pendingChannel?.send(GattPSMServiceDiscovered(characteristic))
                            }
                            return
                        }
                    }
                }
            }

            Timber.e("$tag: Failed discovering psm service")
            runBlocking {
                stop(false)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Timber.e("$tag: onCharacteristicRead failed, got GATT code $status")
                // NOTE : Don't call stop or return here. This isn't always fatal...
            }

            if (pendingCommand !is BLEGattReadCharacteristic) {
                Timber.w("$tag: Received onCharacteristicRead while waiting for  response to ${pendingCommand!!::class.simpleName}")
                return
            }

            if (characteristic.value.size < 50) {
                Timber.i("$tag: Received characteristic value: ${characteristic.value.toByteString()}")
            } else {
                Timber.e("$tag: Received characteristic value is > 50 bytes...")
            }

            if (characteristic.value.size != BLEConstants.PSM_CHARACTERISTIC_SIZE) {
                Timber.e("$tag: Received incorrect number of bytes for psm & public key")
                runBlocking {
                    stop(false)
                }
                return
            }

            val psmBytes = characteristic.value.slice(0..1)
            val psm = bytesToShort(psmBytes.toByteArray())
            val keyBytes: PublicKeyBytes = characteristic.value.slice(2 until BLEConstants.PSM_CHARACTERISTIC_KEY_SIZE).toByteArray()
            val key = keyBytes.toPublicKey()

            runBlocking {
                pendingChannel?.send(GattReadCharacteristicResponse(psm.toInt(), key))
            }
        }
    }
}

private fun bytesToShort(bytes: ByteArray): Short {
    val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Short.BYTES)
    buffer.put(bytes.sliceArray(0 until java.lang.Short.BYTES))
    buffer.flip()
    return buffer.short
}
