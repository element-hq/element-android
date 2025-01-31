/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.call

import io.realm.Realm
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("CallEventProcessor", LoggerTag.VOIP)

@SessionScope
internal class CallEventProcessor @Inject constructor(private val callSignalingHandler: CallSignalingHandler) :
        EventInsertLiveProcessor {

    private val allowedTypes = listOf(
            EventType.CALL_ANSWER,
            EventType.CALL_SELECT_ANSWER,
            EventType.CALL_REJECT,
            EventType.CALL_NEGOTIATE,
            EventType.CALL_CANDIDATES,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.ENCRYPTED,
            EventType.CALL_ASSERTED_IDENTITY,
            EventType.CALL_ASSERTED_IDENTITY_PREFIX
    )

    private val eventsToPostProcess = mutableListOf<Event>()

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        if (insertType != EventInsertType.INCREMENTAL_SYNC) {
            return false
        }
        return allowedTypes.contains(eventType)
    }

    override suspend fun process(realm: Realm, event: Event) {
        eventsToPostProcess.add(event)
    }

    fun shouldProcessFastLane(eventType: String): Boolean {
        return eventType == EventType.CALL_INVITE
    }

    fun processFastLane(event: Event) {
        dispatchToCallSignalingHandlerIfNeeded(event)
    }

    override suspend fun onPostProcess() {
        eventsToPostProcess.forEach {
            dispatchToCallSignalingHandlerIfNeeded(it)
        }
        eventsToPostProcess.clear()
    }

    private fun dispatchToCallSignalingHandlerIfNeeded(event: Event) {
        event.roomId ?: return Unit.also {
            Timber.tag(loggerTag.value).w("Event with no room id ${event.eventId}")
        }
        callSignalingHandler.onCallEvent(event)
    }
}
