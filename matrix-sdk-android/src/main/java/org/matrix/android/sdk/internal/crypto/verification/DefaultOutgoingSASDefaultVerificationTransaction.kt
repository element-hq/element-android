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
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.OutgoingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.crypto.IncomingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.OutgoingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import timber.log.Timber

internal class DefaultOutgoingSASDefaultVerificationTransaction(
        setDeviceVerificationAction: SetDeviceVerificationAction,
        userId: String,
        deviceId: String?,
        cryptoStore: IMXCryptoStore,
        crossSigningService: CrossSigningService,
        outgoingGossipingRequestManager: OutgoingGossipingRequestManager,
        incomingGossipingRequestManager: IncomingGossipingRequestManager,
        deviceFingerprint: String,
        transactionId: String,
        otherUserId: String,
        otherDeviceId: String
) : SASDefaultVerificationTransaction(
        setDeviceVerificationAction,
        userId,
        deviceId,
        cryptoStore,
        crossSigningService,
        outgoingGossipingRequestManager,
        incomingGossipingRequestManager,
        deviceFingerprint,
        transactionId,
        otherUserId,
        otherDeviceId,
        isIncoming = false),
        OutgoingSasVerificationTransaction {

    override val uxState: OutgoingSasVerificationTransaction.UxState
        get() {
            return when (val immutableState = state) {
                is VerificationTxState.None           -> OutgoingSasVerificationTransaction.UxState.WAIT_FOR_START
                is VerificationTxState.SendingStart,
                is VerificationTxState.Started,
                is VerificationTxState.OnAccepted,
                is VerificationTxState.SendingKey,
                is VerificationTxState.KeySent,
                is VerificationTxState.OnKeyReceived  -> OutgoingSasVerificationTransaction.UxState.WAIT_FOR_KEY_AGREEMENT
                is VerificationTxState.ShortCodeReady -> OutgoingSasVerificationTransaction.UxState.SHOW_SAS
                is VerificationTxState.ShortCodeAccepted,
                is VerificationTxState.SendingMac,
                is VerificationTxState.MacSent,
                is VerificationTxState.Verifying      -> OutgoingSasVerificationTransaction.UxState.WAIT_FOR_VERIFICATION
                is VerificationTxState.Verified       -> OutgoingSasVerificationTransaction.UxState.VERIFIED
                is VerificationTxState.Cancelled      -> {
                    if (immutableState.byMe) {
                        OutgoingSasVerificationTransaction.UxState.CANCELLED_BY_OTHER
                    } else {
                        OutgoingSasVerificationTransaction.UxState.CANCELLED_BY_ME
                    }
                }
                else                                  -> OutgoingSasVerificationTransaction.UxState.UNKNOWN
            }
        }

    override fun onVerificationStart(startReq: ValidVerificationInfoStart.SasVerificationInfoStart) {
        Timber.e("## SAS O: onVerificationStart - unexpected id:$transactionId")
        cancel(CancelCode.UnexpectedMessage)
    }

    fun start() {
        if (state != VerificationTxState.None) {
            Timber.e("## SAS O: start verification from invalid state")
            // should I cancel??
            throw IllegalStateException("Interactive Key verification already started")
        }

        val startMessage = transport.createStartForSas(
                deviceId ?: "",
                transactionId,
                KNOWN_AGREEMENT_PROTOCOLS,
                KNOWN_HASHES,
                KNOWN_MACS,
                KNOWN_SHORT_CODES
        )

        startReq = startMessage.asValidObject() as? ValidVerificationInfoStart.SasVerificationInfoStart
        state = VerificationTxState.SendingStart

        sendToOther(
                EventType.KEY_VERIFICATION_START,
                startMessage,
                VerificationTxState.Started,
                CancelCode.User,
                null
        )
    }

//    fun request() {
//        if (state != VerificationTxState.None) {
//            Timber.e("## start verification from invalid state")
//            // should I cancel??
//            throw IllegalStateException("Interactive Key verification already started")
//        }
//
//        val requestMessage = KeyVerificationRequest(
//                fromDevice = session.sessionParams.deviceId ?: "",
//                methods = listOf(KeyVerificationStart.VERIF_METHOD_SAS),
//                timestamp = System.currentTimeMillis().toInt(),
//                transactionId = transactionId
//        )
//
//        sendToOther(
//                EventType.KEY_VERIFICATION_REQUEST,
//                requestMessage,
//                VerificationTxState.None,
//                CancelCode.User,
//                null
//        )
//    }

    override fun onVerificationAccept(accept: ValidVerificationInfoAccept) {
        Timber.v("## SAS O: onVerificationAccept id:$transactionId")
        if (state != VerificationTxState.Started && state != VerificationTxState.SendingStart) {
            Timber.e("## SAS O: received accept request from invalid state $state")
            cancel(CancelCode.UnexpectedMessage)
            return
        }
        // Check that the agreement is correct
        if (!KNOWN_AGREEMENT_PROTOCOLS.contains(accept.keyAgreementProtocol) ||
                !KNOWN_HASHES.contains(accept.hash) ||
                !KNOWN_MACS.contains(accept.messageAuthenticationCode) ||
                accept.shortAuthenticationStrings.intersect(KNOWN_SHORT_CODES).isEmpty()) {
            Timber.e("## SAS O: received invalid accept")
            cancel(CancelCode.UnknownMethod)
            return
        }

        // Upon receipt of the m.key.verification.accept message from Bob’s device,
        // Alice’s device stores the commitment value for later use.
        accepted = accept
        state = VerificationTxState.OnAccepted

        //  Alice’s device creates an ephemeral Curve25519 key pair (dA,QA),
        // and replies with a to_device message with type set to “m.key.verification.key”, sending Alice’s public key QA
        val pubKey = getSAS().publicKey

        val keyToDevice = transport.createKey(transactionId, pubKey)
        // we need to send this to other device now
        state = VerificationTxState.SendingKey
        sendToOther(EventType.KEY_VERIFICATION_KEY, keyToDevice, VerificationTxState.KeySent, CancelCode.User) {
            // It is possible that we receive the next event before this one :/, in this case we should keep state
            if (state == VerificationTxState.SendingKey) {
                state = VerificationTxState.KeySent
            }
        }
    }

    override fun onKeyVerificationKey(vKey: ValidVerificationInfoKey) {
        Timber.v("## SAS O: onKeyVerificationKey id:$transactionId")
        if (state != VerificationTxState.SendingKey && state != VerificationTxState.KeySent) {
            Timber.e("## received key from invalid state $state")
            cancel(CancelCode.UnexpectedMessage)
            return
        }

        otherKey = vKey.key
        // Upon receipt of the m.key.verification.key message from Bob’s device,
        // Alice’s device checks that the commitment property from the Bob’s m.key.verification.accept
        // message is the same as the expected value based on the value of the key property received
        // in Bob’s m.key.verification.key and the content of Alice’s m.key.verification.start message.

        // check commitment
        val concat = vKey.key + startReq!!.canonicalJson
        val otherCommitment = hashUsingAgreedHashMethod(concat) ?: ""

        if (accepted!!.commitment.equals(otherCommitment)) {
            getSAS().setTheirPublicKey(otherKey)
            shortCodeBytes = calculateSASBytes()
            state = VerificationTxState.ShortCodeReady
        } else {
            // bad commitment
            cancel(CancelCode.MismatchedCommitment)
        }
    }

    private fun calculateSASBytes(): ByteArray {
        when (accepted?.keyAgreementProtocol) {
            KEY_AGREEMENT_V1 -> {
                // (Note: In all of the following HKDF is as defined in RFC 5869, and uses the previously agreed-on hash function as the hash function,
                // the shared secret as the input keying material, no salt, and with the input parameter set to the concatenation of:
                // - the string “MATRIX_KEY_VERIFICATION_SAS”,
                // - the Matrix ID of the user who sent the m.key.verification.start message,
                // - the device ID of the device that sent the m.key.verification.start message,
                // - the Matrix ID of the user who sent the m.key.verification.accept message,
                // - he device ID of the device that sent the m.key.verification.accept message
                // - the transaction ID.
                val sasInfo = "MATRIX_KEY_VERIFICATION_SAS$userId$deviceId$otherUserId$otherDeviceId$transactionId"

                // decimal: generate five bytes by using HKDF.
                // emoji: generate six bytes by using HKDF.
                return getSAS().generateShortCode(sasInfo, 6)
            }
            KEY_AGREEMENT_V2 -> {
                // Adds the SAS public key, and separate by |
                val sasInfo = "MATRIX_KEY_VERIFICATION_SAS|$userId|$deviceId|${getSAS().publicKey}|$otherUserId|$otherDeviceId|$otherKey|$transactionId"
                return getSAS().generateShortCode(sasInfo, 6)
            }
            else             -> {
                // Protocol has been checked earlier
                throw IllegalArgumentException()
            }
        }
    }

    override fun onKeyVerificationMac(vMac: ValidVerificationInfoMac) {
        Timber.v("## SAS O: onKeyVerificationMac id:$transactionId")
        // There is starting to be a huge amount of state / race here :/
        if (state != VerificationTxState.OnKeyReceived &&
                state != VerificationTxState.ShortCodeReady &&
                state != VerificationTxState.ShortCodeAccepted &&
                state != VerificationTxState.KeySent &&
                state != VerificationTxState.SendingMac &&
                state != VerificationTxState.MacSent) {
            Timber.e("## SAS O: received mac from invalid state $state")
            cancel(CancelCode.UnexpectedMessage)
            return
        }

        theirMac = vMac

        // Do I have my Mac?
        if (myMac != null) {
            // I can check
            verifyMacs(vMac)
        }
        // Wait for ShortCode Accepted
    }
}
