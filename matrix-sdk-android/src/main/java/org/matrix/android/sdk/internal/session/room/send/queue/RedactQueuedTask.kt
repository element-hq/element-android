/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
