/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.call

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

internal class CallEventProcessor @Inject constructor(
        @UserId private val userId: String,
        private val callService: DefaultCallSignalingService
) : EventInsertLiveProcessor {

    private val allowedTypes = listOf(
            EventType.CALL_ANSWER,
            EventType.CALL_CANDIDATES,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.ENCRYPTED
    )

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        if (insertType != EventInsertType.INCREMENTAL_SYNC) {
            return false
        }
        return allowedTypes.contains(eventType)
    }

    override suspend fun process(realm: Realm, event: Event) {
        update(realm, event)
    }

    private fun update(realm: Realm, event: Event) {
        val now = System.currentTimeMillis()
        // TODO might check if an invite is not closed (hangup/answsered) in the same event batch?
        event.roomId ?: return Unit.also {
            Timber.w("Event with no room id ${event.eventId}")
        }
        val age = now - (event.ageLocalTs ?: now)
        if (age > 40_000) {
            // To old to ring?
            return
        }
        event.ageLocalTs
        if (EventType.isCallEvent(event.getClearType())) {
            callService.onCallEvent(event)
        }
        Timber.v("$realm : $userId")
    }
}
