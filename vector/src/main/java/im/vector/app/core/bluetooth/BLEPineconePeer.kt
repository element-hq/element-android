/*
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.bluetooth

import android.bluetooth.BluetoothSocket
import gobind.Conduit
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class BLEPineconePeer(
        private val deviceAddress: DeviceAddress,
        private val conduit: Conduit,
        private val socket: BluetoothSocket,
        private val pineconeDisconenct: (Conduit) -> Unit,
        private val stopCallback: () -> Unit,
) {
    private val isConnected: Boolean
        get() = socket.isConnected
    private var stopped = AtomicBoolean(false)

    private val bleInput: InputStream = socket.inputStream
    private val bleOutput: OutputStream = socket.outputStream
    private val readerThread = thread {
        reader()
    }
    private val writerThread = thread {
        writer()
    }

    private val TAG = "BLEPineconePeer: $deviceAddress"

    fun close() {
        if (stopped.getAndSet(true)) {
            return
        }

        Timber.i("$TAG: Closing")
        try {
            conduit.close()
        } catch (_: Exception) {
        }
        pineconeDisconenct(conduit)

        try {
            bleInput.close()
        } catch (_: Exception) {
        }
        try {
            bleOutput.close()
        } catch (_: Exception) {
        }
        try {
            socket.close()
        } catch (_: Exception) {
        }

        readerThread.interrupt()
        writerThread.interrupt()
        Timber.i("$TAG: Closed")
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
                Timber.e("$TAG: reader exception: $e")
                try {
                    stopCallback()
                } catch (_: Exception) {}
                break
            }
        }
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
                Timber.e("$TAG: writer exception: $e")
                try {
                    stopCallback()
                } catch (_: Exception) {}
                break
            }
        }
    }
}
