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
import org.matrix.android.sdk.internal.crypto.tasks.RedactEventTask
import org.matrix.android.sdk.internal.crypto.tasks.SendEventTask
import org.matrix.android.sdk.internal.session.room.send.CancelSendTracker
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository
import javax.inject.Inject

internal class QueuedTaskFactory @Inject constructor(
        private val sendEventTask: SendEventTask,
        private val cryptoService: CryptoService,
        private val localEchoRepository: LocalEchoRepository,
        private val redactEventTask: RedactEventTask,
        private val cancelSendTracker: CancelSendTracker
) {

    fun createSendTask(event: Event, encrypt: Boolean): QueuedTask {
        return SendEventQueuedTask(
                event = event,
                encrypt = encrypt,
                cryptoService = cryptoService,
                localEchoRepository = localEchoRepository,
                sendEventTask = sendEventTask,
                cancelSendTracker = cancelSendTracker
        )
    }

    fun createRedactTask(redactionLocalEcho: String, eventId: String, roomId: String, reason: String?): QueuedTask {
        return RedactQueuedTask(
                redactionLocalEchoId = redactionLocalEcho,
                toRedactEventId = eventId,
                roomId = roomId,
                reason = reason,
                redactEventTask = redactEventTask,
                localEchoRepository = localEchoRepository,
                cancelSendTracker = cancelSendTracker
        )
    }
}
