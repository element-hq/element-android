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

import android.util.Base64
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasMode
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.crypto.IncomingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.OutgoingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import timber.log.Timber

internal class DefaultIncomingSASDefaultVerificationTransaction(
        setDeviceVerificationAction: SetDeviceVerificationAction,
        override val userId: String,
        override val deviceId: String?,
        private val cryptoStore: IMXCryptoStore,
        crossSigningService: CrossSigningService,
        outgoingGossipingRequestManager: OutgoingGossipingRequestManager,
        incomingGossipingRequestManager: IncomingGossipingRequestManager,
        deviceFingerprint: String,
        transactionId: String,
        otherUserID: String,
        private val autoAccept: Boolean = false
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
        otherUserID,
        null,
        isIncoming = true),
        IncomingSasVerificationTransaction {

    override val uxState: IncomingSasVerificationTransaction.UxState
        get() {
            return when (val immutableState = state) {
                is VerificationTxState.OnStarted      -> IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT
                is VerificationTxState.SendingAccept,
                is VerificationTxState.Accepted,
                is VerificationTxState.OnKeyReceived,
                is VerificationTxState.SendingKey,
                is VerificationTxState.KeySent        -> IncomingSasVerificationTransaction.UxState.WAIT_FOR_KEY_AGREEMENT
                is VerificationTxState.ShortCodeReady -> IncomingSasVerificationTransaction.UxState.SHOW_SAS
                is VerificationTxState.ShortCodeAccepted,
                is VerificationTxState.SendingMac,
                is VerificationTxState.MacSent,
                is VerificationTxState.Verifying      -> IncomingSasVerificationTransaction.UxState.WAIT_FOR_VERIFICATION
                is VerificationTxState.Verified       -> IncomingSasVerificationTransaction.UxState.VERIFIED
                is VerificationTxState.Cancelled      -> {
                    if (immutableState.byMe) {
                        IncomingSasVerificationTransaction.UxState.CANCELLED_BY_ME
                    } else {
                        IncomingSasVerificationTransaction.UxState.CANCELLED_BY_OTHER
                    }
                }
                else                                  -> IncomingSasVerificationTransaction.UxState.UNKNOWN
            }
        }

    override fun onVerificationStart(startReq: ValidVerificationInfoStart.SasVerificationInfoStart) {
        Timber.v("## SAS I: received verification request from state $state")
        if (state != VerificationTxState.None) {
            Timber.e("## SAS I: received verification request from invalid state")
            // should I cancel??
            throw IllegalStateException("Interactive Key verification already started")
        }
        this.startReq = startReq
        state = VerificationTxState.OnStarted
        this.otherDeviceId = startReq.fromDevice

        if (autoAccept) {
            performAccept()
        }
    }

    override fun performAccept() {
        if (state != VerificationTxState.OnStarted) {
            Timber.e("## SAS Cannot perform accept from state $state")
            return
        }

        // Select a key agreement protocol, a hash algorithm, a message authentication code,
        // and short authentication string methods out of the lists given in requester's message.
        val agreedProtocol = startReq!!.keyAgreementProtocols.firstOrNull { KNOWN_AGREEMENT_PROTOCOLS.contains(it) }
        val agreedHash = startReq!!.hashes.firstOrNull { KNOWN_HASHES.contains(it) }
        val agreedMac = startReq!!.messageAuthenticationCodes.firstOrNull { KNOWN_MACS.contains(it) }
        val agreedShortCode = startReq!!.shortAuthenticationStrings.filter { KNOWN_SHORT_CODES.contains(it) }

        // No common key sharing/hashing/hmac/SAS methods.
        // If a device is unable to complete the verification because the devices are unable to find a common key sharing,
        // hashing, hmac, or SAS method, then it should send a m.key.verification.cancel message
        if (listOf(agreedProtocol, agreedHash, agreedMac).any { it.isNullOrBlank() } ||
                agreedShortCode.isNullOrEmpty()) {
            // Failed to find agreement
            Timber.e("## SAS Failed to find agreement ")
            cancel(CancelCode.UnknownMethod)
            return
        }

        // Bob’s device ensures that it has a copy of Alice’s device key.
        val mxDeviceInfo = cryptoStore.getUserDevice(userId = otherUserId, deviceId = otherDeviceId!!)

        if (mxDeviceInfo?.fingerprint() == null) {
            Timber.e("## SAS Failed to find device key ")
            // TODO force download keys!!
            // would be probably better to download the keys
            // for now I cancel
            cancel(CancelCode.User)
        } else {
            // val otherKey = info.identityKey()
            // need to jump back to correct thread
            val accept = transport.createAccept(
                    tid = transactionId,
                    keyAgreementProtocol = agreedProtocol!!,
                    hash = agreedHash!!,
                    messageAuthenticationCode = agreedMac!!,
                    shortAuthenticationStrings = agreedShortCode,
                    commitment = Base64.encodeToString("temporary commitment".toByteArray(), Base64.DEFAULT)
            )
            doAccept(accept)
        }
    }

    private fun doAccept(accept: VerificationInfoAccept) {
        this.accepted = accept.asValidObject()
        Timber.v("## SAS incoming accept request id:$transactionId")

        // The hash commitment is the hash (using the selected hash algorithm) of the unpadded base64 representation of QB,
        // concatenated with the canonical JSON representation of the content of the m.key.verification.start message
        val concat = getSAS().publicKey + startReq!!.canonicalJson
        accept.commitment = hashUsingAgreedHashMethod(concat) ?: ""
        // we need to send this to other device now
        state = VerificationTxState.SendingAccept
        sendToOther(EventType.KEY_VERIFICATION_ACCEPT, accept, VerificationTxState.Accepted, CancelCode.User) {
            if (state == VerificationTxState.SendingAccept) {
                // It is possible that we receive the next event before this one :/, in this case we should keep state
                state = VerificationTxState.Accepted
            }
        }
    }

    override fun onVerificationAccept(accept: ValidVerificationInfoAccept) {
        Timber.v("## SAS invalid message for incoming request id:$transactionId")
        cancel(CancelCode.UnexpectedMessage)
    }

    override fun onKeyVerificationKey(vKey: ValidVerificationInfoKey) {
        Timber.v("## SAS received key for request id:$transactionId")
        if (state != VerificationTxState.SendingAccept && state != VerificationTxState.Accepted) {
            Timber.e("## SAS received key from invalid state $state")
            cancel(CancelCode.UnexpectedMessage)
            return
        }

        otherKey = vKey.key
        // Upon receipt of the m.key.verification.key message from Alice’s device,
        // Bob’s device replies with a to_device message with type set to m.key.verification.key,
        // sending Bob’s public key QB
        val pubKey = getSAS().publicKey

        val keyToDevice = transport.createKey(transactionId, pubKey)
        // we need to send this to other device now
        state = VerificationTxState.SendingKey
        this.sendToOther(EventType.KEY_VERIFICATION_KEY, keyToDevice, VerificationTxState.KeySent, CancelCode.User) {
            if (state == VerificationTxState.SendingKey) {
                // It is possible that we receive the next event before this one :/, in this case we should keep state
                state = VerificationTxState.KeySent
            }
        }

        // Alice’s and Bob’s devices perform an Elliptic-curve Diffie-Hellman
        // (calculate the point (x,y)=dAQB=dBQA and use x as the result of the ECDH),
        // using the result as the shared secret.

        getSAS().setTheirPublicKey(otherKey)

        shortCodeBytes = calculateSASBytes()

        if (BuildConfig.LOG_PRIVATE_DATA) {
            Timber.v("************  BOB CODE ${getDecimalCodeRepresentation(shortCodeBytes!!)}")
            Timber.v("************  BOB EMOJI CODE ${getShortCodeRepresentation(SasMode.EMOJI)}")
        }

        state = VerificationTxState.ShortCodeReady
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
                val sasInfo = "MATRIX_KEY_VERIFICATION_SAS$otherUserId$otherDeviceId$userId$deviceId$transactionId"

                // decimal: generate five bytes by using HKDF.
                // emoji: generate six bytes by using HKDF.
                return getSAS().generateShortCode(sasInfo, 6)
            }
            KEY_AGREEMENT_V2 -> {
                // Adds the SAS public key, and separate by |
                val sasInfo = "MATRIX_KEY_VERIFICATION_SAS|$otherUserId|$otherDeviceId|$otherKey|$userId|$deviceId|${getSAS().publicKey}|$transactionId"
                return getSAS().generateShortCode(sasInfo, 6)
            }
            else             -> {
                // Protocol has been checked earlier
                throw IllegalArgumentException()
            }
        }
    }

    override fun onKeyVerificationMac(vMac: ValidVerificationInfoMac) {
        Timber.v("## SAS I: received mac for request id:$transactionId")
        // Check for state?
        if (state != VerificationTxState.SendingKey &&
                state != VerificationTxState.KeySent &&
                state != VerificationTxState.ShortCodeReady &&
                state != VerificationTxState.ShortCodeAccepted &&
                state != VerificationTxState.SendingMac &&
                state != VerificationTxState.MacSent) {
            Timber.e("## SAS I: received key from invalid state $state")
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
