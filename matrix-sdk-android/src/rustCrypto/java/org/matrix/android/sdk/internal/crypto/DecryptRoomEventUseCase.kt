/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import javax.inject.Inject

internal class DecryptRoomEventUseCase @Inject constructor(private val olmMachine: OlmMachine) {

    suspend operator fun invoke(event: Event): MXEventDecryptionResult {
        return olmMachine.decryptRoomEvent(event)
    }

    suspend fun decryptAndSaveResult(event: Event) {
        tryOrNull(message = "Unable to decrypt the event") {
            invoke(event)
        }
                ?.let { result ->
                    event.mxDecryptionResult = OlmDecryptionResult(
                            payload = result.clearEvent,
                            senderKey = result.senderCurve25519Key,
                            keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                            forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                            verificationState = result.messageVerificationState
                    )
                }
    }
}
