/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.send.queue

import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.tasks.SendEventTask
import org.matrix.android.sdk.internal.session.room.send.CancelSendTracker
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository

internal class SendEventQueuedTask(
        val event: Event,
        val encrypt: Boolean,
        val sendEventTask: SendEventTask,
        val cryptoService: CryptoService,
        val localEchoRepository: LocalEchoRepository,
        val cancelSendTracker: CancelSendTracker
) : QueuedTask(queueIdentifier = event.roomId!!, taskIdentifier = event.eventId!!) {

    override suspend fun doExecute() {
        sendEventTask.execute(SendEventTask.Params(event, encrypt))
    }

    override fun onTaskFailed() {
        when (event.getClearType()) {
            EventType.REDACTION,
            EventType.REACTION -> {
                // we just delete? it will not be present on timeline and no ux to retry
                localEchoRepository.deleteFailedEchoAsync(eventId = event.eventId, roomId = event.roomId ?: "")
                // TODO update aggregation :/ or it will stay locally
            }
            else               -> {
                localEchoRepository.updateSendState(event.eventId!!, event.roomId, SendState.UNDELIVERED)
            }
        }
    }

    override fun isCancelled(): Boolean {
        return super.isCancelled() || cancelSendTracker.isCancelRequestedFor(event.eventId, event.roomId)
    }
}
