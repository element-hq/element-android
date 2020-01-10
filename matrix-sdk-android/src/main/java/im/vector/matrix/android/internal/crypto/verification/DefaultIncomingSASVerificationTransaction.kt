/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.verification

import android.util.Base64
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.IncomingSasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasMode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.actions.SetDeviceVerificationAction
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import timber.log.Timber

internal class DefaultIncomingSASVerificationTransaction(
        setDeviceVerificationAction: SetDeviceVerificationAction,
        override val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        deviceFingerprint: String,
        transactionId: String,
        otherUserID: String,
        val autoAccept: Boolean = false
) : SASVerificationTransaction(
        setDeviceVerificationAction,
        credentials,
        cryptoStore,
        deviceFingerprint,
        transactionId,
        otherUserID,
        null,
        true),
        IncomingSasVerificationTransaction {

    override val uxState: IncomingSasVerificationTransaction.UxState
        get() {
            return when (state) {
                SasVerificationTxState.OnStarted      -> IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT
                SasVerificationTxState.SendingAccept,
                SasVerificationTxState.Accepted,
                SasVerificationTxState.OnKeyReceived,
                SasVerificationTxState.SendingKey,
                SasVerificationTxState.KeySent        -> IncomingSasVerificationTransaction.UxState.WAIT_FOR_KEY_AGREEMENT
                SasVerificationTxState.ShortCodeReady -> IncomingSasVerificationTransaction.UxState.SHOW_SAS
                SasVerificationTxState.ShortCodeAccepted,
                SasVerificationTxState.SendingMac,
                SasVerificationTxState.MacSent,
                SasVerificationTxState.Verifying      -> IncomingSasVerificationTransaction.UxState.WAIT_FOR_VERIFICATION
                SasVerificationTxState.Verified       -> IncomingSasVerificationTransaction.UxState.VERIFIED
                SasVerificationTxState.Cancelled      -> IncomingSasVerificationTransaction.UxState.CANCELLED_BY_ME
                SasVerificationTxState.OnCancelled    -> IncomingSasVerificationTransaction.UxState.CANCELLED_BY_OTHER
                else                                  -> IncomingSasVerificationTransaction.UxState.UNKNOWN
            }
        }

    override fun onVerificationStart(startReq: VerificationInfoStart) {
        Timber.v("## SAS I: received verification request from state $state")
        if (state != SasVerificationTxState.None) {
            Timber.e("## SAS I: received verification request from invalid state")
            // should I cancel??
            throw IllegalStateException("Interactive Key verification already started")
        }
        this.startReq = startReq
        state = SasVerificationTxState.OnStarted
        this.otherDeviceId = startReq.fromDevice

        if (autoAccept) {
            performAccept()
        }
    }

    override fun performAccept() {
        if (state != SasVerificationTxState.OnStarted) {
            Timber.e("## SAS Cannot perform accept from state $state")
            return
        }

        // Select a key agreement protocol, a hash algorithm, a message authentication code,
        // and short authentication string methods out of the lists given in requester's message.
        val agreedProtocol = startReq!!.keyAgreementProtocols?.firstOrNull { KNOWN_AGREEMENT_PROTOCOLS.contains(it) }
        val agreedHash = startReq!!.hashes?.firstOrNull { KNOWN_HASHES.contains(it) }
        val agreedMac = startReq!!.messageAuthenticationCodes?.firstOrNull { KNOWN_MACS.contains(it) }
        val agreedShortCode = startReq!!.shortAuthenticationStrings?.filter { KNOWN_SHORT_CODES.contains(it) }

        // No common key sharing/hashing/hmac/SAS methods.
        // If a device is unable to complete the verification because the devices are unable to find a common key sharing,
        // hashing, hmac, or SAS method, then it should send a m.key.verification.cancel message
        if (listOf(agreedProtocol, agreedHash, agreedMac).any { it.isNullOrBlank() }
                || agreedShortCode.isNullOrEmpty()) {
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
        this.accepted = accept
        Timber.v("## SAS incoming accept request id:$transactionId")

        // The hash commitment is the hash (using the selected hash algorithm) of the unpadded base64 representation of QB,
        // concatenated with the canonical JSON representation of the content of the m.key.verification.start message
        val concat = getSAS().publicKey + startReq!!.toCanonicalJson()
        accept.commitment = hashUsingAgreedHashMethod(concat) ?: ""
        // we need to send this to other device now
        state = SasVerificationTxState.SendingAccept
        sendToOther(EventType.KEY_VERIFICATION_ACCEPT, accept, SasVerificationTxState.Accepted, CancelCode.User) {
            if (state == SasVerificationTxState.SendingAccept) {
                // It is possible that we receive the next event before this one :/, in this case we should keep state
                state = SasVerificationTxState.Accepted
            }
        }
    }

    override fun onVerificationAccept(accept: VerificationInfoAccept) {
        Timber.v("## SAS invalid message for incoming request id:$transactionId")
        cancel(CancelCode.UnexpectedMessage)
    }

    override fun onKeyVerificationKey(userId: String, vKey: VerificationInfoKey) {
        Timber.v("## SAS received key for request id:$transactionId")
        if (state != SasVerificationTxState.SendingAccept && state != SasVerificationTxState.Accepted) {
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
        state = SasVerificationTxState.SendingKey
        this.sendToOther(EventType.KEY_VERIFICATION_KEY, keyToDevice, SasVerificationTxState.KeySent, CancelCode.User) {
            if (state == SasVerificationTxState.SendingKey) {
                // It is possible that we receive the next event before this one :/, in this case we should keep state
                state = SasVerificationTxState.KeySent
            }
        }

        // Alice’s and Bob’s devices perform an Elliptic-curve Diffie-Hellman
        // (calculate the point (x,y)=dAQB=dBQA and use x as the result of the ECDH),
        // using the result as the shared secret.

        getSAS().setTheirPublicKey(otherKey)
        // (Note: In all of the following HKDF is as defined in RFC 5869, and uses the previously agreed-on hash function as the hash function,
        // the shared secret as the input keying material, no salt, and with the input parameter set to the concatenation of:
        // - the string “MATRIX_KEY_VERIFICATION_SAS”,
        // - the Matrix ID of the user who sent the m.key.verification.start message,
        // - the device ID of the device that sent the m.key.verification.start message,
        // - the Matrix ID of the user who sent the m.key.verification.accept message,
        // - he device ID of the device that sent the m.key.verification.accept message
        // - the transaction ID.
        val sasInfo = "MATRIX_KEY_VERIFICATION_SAS" +
                "$otherUserId$otherDeviceId" +
                "${credentials.userId}${credentials.deviceId}" +
                transactionId
        // decimal: generate five bytes by using HKDF.
        // emoji: generate six bytes by using HKDF.
        shortCodeBytes = getSAS().generateShortCode(sasInfo, 6)

        if (BuildConfig.LOG_PRIVATE_DATA) {
            Timber.v("************  BOB CODE ${getDecimalCodeRepresentation(shortCodeBytes!!)}")
            Timber.v("************  BOB EMOJI CODE ${getShortCodeRepresentation(SasMode.EMOJI)}")
        }

        state = SasVerificationTxState.ShortCodeReady
    }

    override fun onKeyVerificationMac(vKey: VerificationInfoMac) {
        Timber.v("## SAS I: received mac for request id:$transactionId")
        // Check for state?
        if (state != SasVerificationTxState.SendingKey
                && state != SasVerificationTxState.KeySent
                && state != SasVerificationTxState.ShortCodeReady
                && state != SasVerificationTxState.ShortCodeAccepted
                && state != SasVerificationTxState.SendingMac
                && state != SasVerificationTxState.MacSent) {
            Timber.e("## SAS I: received key from invalid state $state")
            cancel(CancelCode.UnexpectedMessage)
            return
        }
        theirMac = vKey

        // Do I have my Mac?
        if (myMac != null) {
            // I can check
            verifyMacs()
        }
        // Wait for ShortCode Accepted
    }
}
