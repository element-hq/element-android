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

package im.vector.app

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.LiveEventListener
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.util.JsonDict
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors

data class E2EMessageDetected(
        val eventId: String,
        val roomId: String,
        val senderUserId: String,
        val senderDeviceId: String,
        val senderKey: String,
        val sessionId: String
        ) {

    companion object {
        fun fromEvent(event: Event, roomId: String): E2EMessageDetected {
            val encryptedContent = event.content.toModel<EncryptedEventContent>()

            return E2EMessageDetected(
                    eventId = event.eventId ?: "",
                    roomId = roomId,
                    senderUserId = event.senderId ?: "",
                    senderDeviceId = encryptedContent?.deviceId ?: "",
                    senderKey = encryptedContent?.senderKey ?: "",
                    sessionId = encryptedContent?.sessionId ?: ""
            )
        }
    }
}

class UISIDetector : LiveEventListener {

    interface UISIDetectorCallback {
        val enabled: Boolean
        val reciprocateToDeviceEventType: String
        fun uisiDetected(source: E2EMessageDetected)
        fun uisiReciprocateRequest(source: Event)
    }

    var callback: UISIDetectorCallback? = null

    private val trackedEvents = mutableMapOf<String, TimerTask>()
    private val executor = Executors.newSingleThreadExecutor()
    private val timer = Timer()
    private val timeoutMillis = 30_000L
    private val enabled: Boolean get() = callback?.enabled.orFalse()

    override fun onEventDecrypted(event: Event, clearEvent: JsonDict) {
        val eventId = event.eventId
        val roomId = event.roomId
        if (!enabled || eventId == null || roomId == null) return
        executor.execute {
            unTrack(eventId, roomId)
        }
    }

    override fun onLiveToDeviceEvent(event: Event) {
        if (!enabled) return
        if (event.type == callback?.reciprocateToDeviceEventType) {
            callback?.uisiReciprocateRequest(event)
        }
    }

    override fun onEventDecryptionError(event: Event, throwable: Throwable) {
        val eventId = event.eventId
        val roomId = event.roomId
        if (!enabled || eventId == null || roomId == null) return

        val trackerId: String = trackerId(eventId, roomId)
        if (trackedEvents.containsKey(trackerId)) {
            Timber.w("## UISIDetector: Event $eventId is already tracked")
            return
        }
        // track it and start timer
        val timeoutTask = object : TimerTask() {
            override fun run() {
                executor.execute {
                    unTrack(eventId, roomId)
                    Timber.v("## UISIDetector: Timeout on $eventId")
                    triggerUISI(E2EMessageDetected.fromEvent(event, roomId))
                }
            }
        }
        trackedEvents[trackerId] = timeoutTask
        timer.schedule(timeoutTask, timeoutMillis)
    }

    override fun onLiveEvent(roomId: String, event: Event) { }

    override fun onPaginatedEvent(roomId: String, event: Event) { }

    private fun trackerId(eventId: String, roomId: String): String = "$roomId-$eventId"

    private fun triggerUISI(source: E2EMessageDetected) {
        if (!enabled) return
        Timber.i("## UISIDetector: Unable To Decrypt $source")
        callback?.uisiDetected(source)
    }

    private fun unTrack(eventId: String, roomId: String) {
        trackedEvents.remove(trackerId(eventId, roomId))?.cancel()
    }
}
