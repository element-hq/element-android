/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Dynamic broadcast receiver to detect headset plug/unplug.
 */
class WiredHeadsetStateReceiver : BroadcastReceiver() {

    interface HeadsetEventListener {
        fun onHeadsetEvent(event: HeadsetPlugEvent)
    }

    var delegate: WeakReference<HeadsetEventListener>? = null

    data class HeadsetPlugEvent(
            val plugged: Boolean,
            val headsetName: String?,
            val hasMicrophone: Boolean
    )

    override fun onReceive(context: Context?, intent: Intent?) {
        // The intent will have the following extra values:
        //  state  0 for unplugged, 1 for plugged
        //  name  Headset type, human readable string
        //  microphone 1 if headset has a microphone, 0 otherwise

        val isPlugged = when (intent?.getIntExtra("state", -1)) {
            0 -> false
            1 -> true
            else -> return Unit.also {
                Timber.v("## VOIP WiredHeadsetStateReceiver invalid state")
            }
        }
        val hasMicrophone = when (intent.getIntExtra("microphone", -1)) {
            1 -> true
            else -> false
        }

        delegate?.get()?.onHeadsetEvent(
                HeadsetPlugEvent(plugged = isPlugged, headsetName = intent.getStringExtra("name"), hasMicrophone = hasMicrophone)
        )
    }

    companion object {
        fun createAndRegister(context: Context, listener: HeadsetEventListener): WiredHeadsetStateReceiver {
            val receiver = WiredHeadsetStateReceiver()
            receiver.delegate = WeakReference(listener)
            val action = AudioManager.ACTION_HEADSET_PLUG
            ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(action),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            return receiver
        }

        fun unRegister(context: Context, receiver: WiredHeadsetStateReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}
