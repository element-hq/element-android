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
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import javax.inject.Inject

internal class DecryptRoomEventUseCase @Inject constructor(
        private val olmDevice: MXOlmDevice,
        private val cryptoStore: IMXCryptoStore,
        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
) {

    suspend operator fun invoke(event: Event, requestKeysOnFail: Boolean = true): MXEventDecryptionResult {
        if (event.roomId.isNullOrBlank()) {
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
        }

        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()
                ?: throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)

        if (encryptedEventContent.senderKey.isNullOrBlank() ||
                encryptedEventContent.sessionId.isNullOrBlank() ||
                encryptedEventContent.ciphertext.isNullOrBlank()) {
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
        }

        try {
            val olmDecryptionResult = olmDevice.decryptGroupMessage(
                    encryptedEventContent.ciphertext,
                    event.roomId,
                    "",
                    eventId = event.eventId.orEmpty(),
                    encryptedEventContent.sessionId,
                    encryptedEventContent.senderKey
            )
            if (olmDecryptionResult.payload != null) {
                return MXEventDecryptionResult(
                        clearEvent = olmDecryptionResult.payload,
                        senderCurve25519Key = olmDecryptionResult.senderKey,
                        claimedEd25519Key = olmDecryptionResult.keysClaimed?.get("ed25519"),
                        forwardingCurve25519KeyChain = olmDecryptionResult.forwardingCurve25519KeyChain
                                .orEmpty(),
                        messageVerificationState = olmDecryptionResult.verificationState
                )
            } else {
                throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
            }
        } catch (throwable: Throwable) {
            if (throwable is MXCryptoError.OlmError) {
                // TODO Check the value of .message
                if (throwable.olmException.message == "UNKNOWN_MESSAGE_INDEX") {
                    // So we know that session, but it's ratcheted and we can't decrypt at that index
                    // Check if partially withheld
                    val withHeldInfo = cryptoStore.getWithHeldMegolmSession(event.roomId, encryptedEventContent.sessionId)
                    if (withHeldInfo != null) {
                        // Encapsulate as withHeld exception
                        throw MXCryptoError.Base(
                                MXCryptoError.ErrorType.KEYS_WITHHELD,
                                withHeldInfo.code?.value ?: "",
                                withHeldInfo.reason
                        )
                    }

                    throw MXCryptoError.Base(
                            MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX,
                            "UNKNOWN_MESSAGE_INDEX",
                            null
                    )
                }

                val reason = String.format(MXCryptoError.OLM_REASON, throwable.olmException.message)
                val detailedReason = String.format(MXCryptoError.DETAILED_OLM_REASON, encryptedEventContent.ciphertext, reason)

                throw MXCryptoError.Base(
                        MXCryptoError.ErrorType.OLM,
                        reason,
                        detailedReason
                )
            }
            if (throwable is MXCryptoError.Base) {
                if (throwable.errorType == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) {
                    // Check if it was withheld by sender to enrich error code
                    val withHeldInfo = cryptoStore.getWithHeldMegolmSession(event.roomId, encryptedEventContent.sessionId)
                    if (withHeldInfo != null) {
                        if (requestKeysOnFail) {
                            requestKeysForEvent(event)
                        }
                        // Encapsulate as withHeld exception
                        throw MXCryptoError.Base(
                                MXCryptoError.ErrorType.KEYS_WITHHELD,
                                withHeldInfo.code?.value ?: "",
                                withHeldInfo.reason
                        )
                    }

                    if (requestKeysOnFail) {
                        requestKeysForEvent(event)
                    }
                }
            }
            throw throwable
        }
    }

    private fun requestKeysForEvent(event: Event) {
        outgoingKeyRequestManager.requestKeyForEvent(event, false)
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
