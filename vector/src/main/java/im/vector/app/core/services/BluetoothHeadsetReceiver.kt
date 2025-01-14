/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import java.lang.ref.WeakReference

class BluetoothHeadsetReceiver : BroadcastReceiver() {

    interface EventListener {
        fun onBTHeadsetEvent(event: BTHeadsetPlugEvent)
    }

    var delegate: WeakReference<EventListener>? = null

    data class BTHeadsetPlugEvent(
            val plugged: Boolean,
            val headsetName: String?,
            /**
             * Can be:
             * - BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
             * - BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
             * - AUDIO_VIDEO_WEARABLE_HEADSET.
             */
            val deviceClass: Int
    )

    override fun onReceive(context: Context?, intent: Intent?) {
        // This intent will have 3 extras:
        //  EXTRA_CONNECTION_STATE - The current connection state
        //  EXTRA_PREVIOUS_CONNECTION_STATE}- The previous connection state.
        //  BluetoothDevice#EXTRA_DEVICE - The remote device.
        // EXTRA_CONNECTION_STATE  or EXTRA_PREVIOUS_CONNECTION_STATE can be any of
        // STATE_DISCONNECTED}, STATE_CONNECTING, STATE_CONNECTED, STATE_DISCONNECTING

        val headsetConnected = when (intent?.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)) {
            BluetoothAdapter.STATE_CONNECTED -> true
            BluetoothAdapter.STATE_DISCONNECTED -> false
            else -> return // ignore intermediate states
        }

        val device = intent.getParcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val deviceName = device?.name
        when (device?.bluetoothClass?.deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> {
                // filter only device that we care about for
                delegate?.get()?.onBTHeadsetEvent(
                        BTHeadsetPlugEvent(
                                plugged = headsetConnected,
                                headsetName = deviceName,
                                deviceClass = device.bluetoothClass.deviceClass
                        )
                )
            }
            else -> return
        }
    }

    companion object {
        fun createAndRegister(context: Context, listener: EventListener): BluetoothHeadsetReceiver {
            val receiver = BluetoothHeadsetReceiver()
            receiver.delegate = WeakReference(listener)
            ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            return receiver
        }

        fun unRegister(context: Context, receiver: BluetoothHeadsetReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}
