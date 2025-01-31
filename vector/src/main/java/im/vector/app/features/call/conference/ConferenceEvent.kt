/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.react.bridge.JavaOnlyMap
import org.jitsi.meet.sdk.BroadcastEmitter
import org.jitsi.meet.sdk.BroadcastEvent
import org.jitsi.meet.sdk.JitsiMeet
import timber.log.Timber

private const val CONFERENCE_URL_DATA_KEY = "url"

sealed class ConferenceEvent(open val data: Map<String, Any>) {
    data class Terminated(override val data: Map<String, Any>) : ConferenceEvent(data)
    data class WillJoin(override val data: Map<String, Any>) : ConferenceEvent(data)
    data class Joined(override val data: Map<String, Any>) : ConferenceEvent(data)

    fun extractConferenceUrl(): String? {
        return data[CONFERENCE_URL_DATA_KEY] as? String
    }
}

class ConferenceEventEmitter(private val context: Context) {

    fun emitConferenceEnded() {
        val broadcastEventData = JavaOnlyMap.of(CONFERENCE_URL_DATA_KEY, JitsiMeet.getCurrentConference())
        BroadcastEmitter(context).sendBroadcast(BroadcastEvent.Type.CONFERENCE_TERMINATED.name, broadcastEventData)
    }
}

class ConferenceEventObserver(
        private val context: Context,
        private val onBroadcastEvent: (ConferenceEvent) -> Unit
) :
        DefaultLifecycleObserver {

    // See https://jitsi.github.io/handbook/docs/dev-guide/dev-guide-android-sdk#listening-for-broadcasted-events
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { onBroadcastReceived(it) }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        } catch (throwable: Throwable) {
            Timber.v("Unable to unregister receiver")
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        val intentFilter = IntentFilter()
        for (type in BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.action)
        }
        try {
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter)
        } catch (throwable: Throwable) {
            Timber.v("Unable to register receiver")
        }
    }

    private fun onBroadcastReceived(intent: Intent) {
        val event = BroadcastEvent(intent)
        val conferenceEvent = when (event.type) {
            BroadcastEvent.Type.CONFERENCE_JOINED -> ConferenceEvent.Joined(event.data)
            BroadcastEvent.Type.CONFERENCE_TERMINATED -> ConferenceEvent.Terminated(event.data)
            BroadcastEvent.Type.CONFERENCE_WILL_JOIN -> ConferenceEvent.WillJoin(event.data)
            else -> null
        }
        if (conferenceEvent != null) {
            onBroadcastEvent(conferenceEvent)
        }
    }
}
