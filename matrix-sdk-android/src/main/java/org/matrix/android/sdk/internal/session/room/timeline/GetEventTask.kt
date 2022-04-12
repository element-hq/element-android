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

package org.matrix.android.sdk.internal.session.room.timeline

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.crypto.EventDecryptor
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
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
        private val eventDecryptor: EventDecryptor
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
                                forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                        )
                    }
        }

        event.ageLocalTs = event.unsignedData?.age?.let { System.currentTimeMillis() - it }

        return event
    }
}
