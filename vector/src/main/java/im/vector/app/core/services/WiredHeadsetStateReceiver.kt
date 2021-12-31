/*
 * Copyright (c) 2020 New Vector Ltd
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Dynamic broadcast receiver to detect headset plug/unplug
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
            0    -> false
            1    -> true
            else -> return Unit.also {
                Timber.v("## VOIP WiredHeadsetStateReceiver invalid state")
            }
        }
        val hasMicrophone = when (intent.getIntExtra("microphone", -1)) {
            1    -> true
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
            context.registerReceiver(receiver, IntentFilter(action))
            return receiver
        }

        fun unRegister(context: Context, receiver: WiredHeadsetStateReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}
