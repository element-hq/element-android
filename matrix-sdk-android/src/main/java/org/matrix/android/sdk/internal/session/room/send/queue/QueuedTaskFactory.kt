/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
