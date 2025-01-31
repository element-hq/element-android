/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.timeline

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.crypto.EventDecryptor
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal interface GetEventTask : Task<GetEventTask.Params, Event> {
    data class Params(
            val roomId: String,
            val eventId: String
    )
}

internal class DefaultGetEventTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val eventDecryptor: EventDecryptor,
        private val clock: Clock,
) : GetEventTask {

    override suspend fun execute(params: GetEventTask.Params): Event {
        val event = executeRequest(globalErrorReceiver) {
            roomAPI.getEvent(params.roomId, params.eventId)
        }

        // Try to decrypt the Event
        if (event.isEncrypted()) {
            tryOrNull(message = "Unable to decrypt the event") {
                eventDecryptor.decryptEvent(event, "")
            }
                    ?.let { result ->
                        event.mxDecryptionResult = OlmDecryptionResult(
                                payload = result.clearEvent,
                                senderKey = result.senderCurve25519Key,
                                keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                                forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                                isSafe = result.isSafe
                        )
                    }
        }

        event.ageLocalTs = clock.epochMillis() - (event.unsignedData?.age ?: 0)

        return event
    }
}
