/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.LiveEventListener
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
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

class UISIDetector(private val timeoutMillis: Long = 30_000L) : LiveEventListener {

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

    override fun onEventDecryptionError(event: Event, cryptoError: MXCryptoError) {
        val eventId = event.eventId
        val roomId = event.roomId
        if (!enabled || eventId == null || roomId == null) return

        val trackedId: String = trackedId(eventId, roomId)
        if (trackedEvents.containsKey(trackedId)) {
            Timber.v("## UISIDetector: Event $eventId is already tracked")
            return
        }
        // track it and start timer
        val timeoutTask = object : TimerTask() {
            override fun run() {
                executor.execute {
                    // we should check if it's still tracked (it might have been decrypted)
                    if (!trackedEvents.containsKey(trackedId)) {
                        Timber.v("## UISIDetector: E2E error for $eventId was resolved")
                        return@execute
                    }
                    unTrack(eventId, roomId)
                    Timber.v("## UISIDetector: Timeout on $eventId")
                    triggerUISI(E2EMessageDetected.fromEvent(event, roomId))
                }
            }
        }
        trackedEvents[trackedId] = timeoutTask
        timer.schedule(timeoutTask, timeoutMillis)
    }

    override fun onLiveEvent(roomId: String, event: Event) {}

    override fun onPaginatedEvent(roomId: String, event: Event) {}

    private fun trackedId(eventId: String, roomId: String): String = "$roomId-$eventId"

    private fun triggerUISI(source: E2EMessageDetected) {
        if (!enabled) return
        Timber.i("## UISIDetector: Unable To Decrypt $source")
        callback?.uisiDetected(source)
    }

    private fun unTrack(eventId: String, roomId: String) {
        trackedEvents.remove(trackedId(eventId, roomId))?.cancel()
    }
}
