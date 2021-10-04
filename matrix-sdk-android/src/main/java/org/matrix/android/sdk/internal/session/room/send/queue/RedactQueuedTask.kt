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

import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.tasks.RedactEventTask
import org.matrix.android.sdk.internal.session.room.send.CancelSendTracker
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository

internal class RedactQueuedTask(
        private val toRedactEventId: String,
        val redactionLocalEchoId: String,
        private val roomId: String,
        private val reason: String?,
        private val redactEventTask: RedactEventTask,
        private val localEchoRepository: LocalEchoRepository,
        private val cancelSendTracker: CancelSendTracker
) : QueuedTask(queueIdentifier = roomId, taskIdentifier = redactionLocalEchoId) {

    override suspend fun doExecute() {
        redactEventTask.execute(RedactEventTask.Params(redactionLocalEchoId, roomId, toRedactEventId, reason))
    }

    override fun onTaskFailed() {
        localEchoRepository.updateSendState(redactionLocalEchoId, roomId, SendState.UNDELIVERED)
    }

    override fun isCancelled(): Boolean {
        return super.isCancelled() || cancelSendTracker.isCancelRequestedFor(redactionLocalEchoId, roomId)
    }
}
