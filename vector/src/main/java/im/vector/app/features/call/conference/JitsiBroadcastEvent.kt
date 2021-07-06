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

package im.vector.app.features.call.conference

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.react.bridge.JavaOnlyMap
import org.jitsi.meet.sdk.BroadcastEmitter
import org.jitsi.meet.sdk.BroadcastEvent
import org.jitsi.meet.sdk.JitsiMeet
import org.matrix.android.sdk.api.extensions.tryOrNull

private const val CONFERENCE_URL_DATA_KEY = "url"

fun BroadcastEvent.extractConferenceUrl(): String? {
    return when (type) {
        BroadcastEvent.Type.CONFERENCE_TERMINATED,
        BroadcastEvent.Type.CONFERENCE_WILL_JOIN,
        BroadcastEvent.Type.CONFERENCE_JOINED -> data[CONFERENCE_URL_DATA_KEY] as? String
        else                                  -> null
    }
}

class JitsiBroadcastEmitter(private val context: Context) {

    fun emitConferenceEnded() {
        val broadcastEventData = JavaOnlyMap.of(CONFERENCE_URL_DATA_KEY, JitsiMeet.getCurrentConference())
        BroadcastEmitter(context).sendBroadcast(BroadcastEvent.Type.CONFERENCE_TERMINATED.name, broadcastEventData)
    }
}

class JitsiBroadcastEventObserver(private val context: Context,
                                  private val onBroadcastEvent: (BroadcastEvent) -> Unit) : LifecycleObserver {

    // See https://jitsi.github.io/handbook/docs/dev-guide/dev-guide-android-sdk#listening-for-broadcasted-events
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { onBroadcastReceived(it) }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unregisterForBroadcastMessages() {
        tryOrNull("Unable to unregister receiver") {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun registerForBroadcastMessages() {
        val intentFilter = IntentFilter()
        for (type in BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.action)
        }
        tryOrNull("Unable to register receiver") {
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter)
        }
    }

    private fun onBroadcastReceived(intent: Intent) {
        val event = BroadcastEvent(intent)
        onBroadcastEvent(event)
    }
}
