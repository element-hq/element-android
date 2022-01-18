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

import org.matrix.android.sdk.api.session.LiveEventListener
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors

enum class UISIEventSource {
    INITIAL_SYNC,
    INCREMENTAL_SYNC,
    PAGINATION
}

data class E2EMessageDetected(
        val eventId: String,
        val roomId: String,
        val senderUserId: String,
        val senderDeviceId: String,
        val senderKey: String,
        val sessionId: String,
        val source: UISIEventSource) {

    companion object {
        fun fromEvent(event: Event, roomId: String, source: UISIEventSource): E2EMessageDetected {
            val encryptedContent = event.content.toModel<EncryptedEventContent>()

            return E2EMessageDetected(
                    eventId = event.eventId ?: "",
                    roomId = roomId,
                    senderUserId = event.senderId ?: "",
                    senderDeviceId = encryptedContent?.deviceId ?: "",
                    senderKey = encryptedContent?.senderKey ?: "",
                    sessionId = encryptedContent?.sessionId ?: "",
                    source = source
            )
        }
    }
}

class UISIDetector : LiveEventListener {

    interface UISIDetectorCallback {
        val reciprocateToDeviceEventType: String
        fun uisiDetected(source: E2EMessageDetected)
        fun uisiReciprocateRequest(source: Event)
    }

    var callback: UISIDetectorCallback? = null

    private val trackedEvents = mutableListOf<Pair<E2EMessageDetected, TimerTask>>()
    private val executor = Executors.newSingleThreadExecutor()
    private val timer = Timer()
    private val timeoutMillis = 30_000L
    var enabled = false

    override fun onLiveEvent(roomId: String, event: Event) {
        if (!enabled) return
        if (!event.isEncrypted()) return
        executor.execute {
            handleEventReceived(E2EMessageDetected.fromEvent(event, roomId, UISIEventSource.INCREMENTAL_SYNC))
        }
    }

    override fun onPaginatedEvent(roomId: String, event: Event) {
        if (!enabled) return
        if (!event.isEncrypted()) return
        executor.execute {
            handleEventReceived(E2EMessageDetected.fromEvent(event, roomId, UISIEventSource.PAGINATION))
        }
    }

    override fun onEventDecrypted(eventId: String, roomId: String, clearEvent: JsonDict) {
        if (!enabled) return
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

    override fun onEventDecryptionError(eventId: String, roomId: String, throwable: Throwable) {
        if (!enabled) return
        executor.execute {
            unTrack(eventId, roomId)?.let {
                triggerUISI(it)
            }
//            if (throwable is MXCryptoError.OlmError) {
//                if (throwable.olmException.message == "UNKNOWN_MESSAGE_INDEX") {
//                    unTrack(eventId, roomId)?.let {
//                        triggerUISI(it)
//                    }
//                }
//            }
        }
    }

    private fun handleEventReceived(detectorEvent: E2EMessageDetected) {
        if (!enabled) return
        if (trackedEvents.any { it.first == detectorEvent }) {
            Timber.w("## UISIDetector: Event ${detectorEvent.eventId} is already tracked")
        } else {
            // track it and start timer
            val timeoutTask = object : TimerTask() {
                override fun run() {
                    executor.execute {
                        unTrack(detectorEvent.eventId, detectorEvent.roomId)
                        Timber.v("## UISIDetector: Timeout on ${detectorEvent.eventId} ")
                        triggerUISI(detectorEvent)
                    }
                }
            }
            trackedEvents.add(detectorEvent to timeoutTask)
            timer.schedule(timeoutTask, timeoutMillis)
        }
    }

    private fun triggerUISI(source: E2EMessageDetected) {
        if (!enabled) return
        Timber.i("## UISIDetector: Unable To Decrypt $source")
        callback?.uisiDetected(source)
    }

    private fun unTrack(eventId: String, roomId: String): E2EMessageDetected? {
        val index = trackedEvents.indexOfFirst { it.first.eventId == eventId && it.first.roomId == roomId }
        return if (index != -1) {
            trackedEvents.removeAt(index).let {
                it.second.cancel()
                it.first
            }
        } else {
            null
        }
    }
}
