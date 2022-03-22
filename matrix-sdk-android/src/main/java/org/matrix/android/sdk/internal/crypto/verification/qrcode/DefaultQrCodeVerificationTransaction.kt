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

package org.matrix.android.sdk.internal.crypto.verification.qrcode

import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.crypto.IncomingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.OutgoingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64Safe
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.verification.DefaultVerificationTransaction
import org.matrix.android.sdk.internal.crypto.verification.ValidVerificationInfoStart
import timber.log.Timber

internal class DefaultQrCodeVerificationTransaction(
        setDeviceVerificationAction: SetDeviceVerificationAction,
        override val transactionId: String,
        override val otherUserId: String,
        override var otherDeviceId: String?,
        private val crossSigningService: CrossSigningService,
        outgoingGossipingRequestManager: OutgoingGossipingRequestManager,
        incomingGossipingRequestManager: IncomingGossipingRequestManager,
        private val cryptoStore: IMXCryptoStore,
        // Not null only if other user is able to scan QR code
        private val qrCodeData: QrCodeData?,
        val userId: String,
        val deviceId: String,
        override val isIncoming: Boolean
) : DefaultVerificationTransaction(
        setDeviceVerificationAction,
        crossSigningService,
        outgoingGossipingRequestManager,
        incomingGossipingRequestManager,
        userId,
        transactionId,
        otherUserId,
        otherDeviceId,
        isIncoming),
        QrCodeVerificationTransaction {

    override val qrCodeText: String?
        get() = qrCodeData?.toEncodedString()

    override var state: VerificationTxState = VerificationTxState.None
        set(newState) {
            field = newState

            listeners.forEach {
                try {
                    it.transactionUpdated(this)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }

    override fun userHasScannedOtherQrCode(otherQrCodeText: String) {
        val otherQrCodeData = otherQrCodeText.toQrCodeData() ?: run {
            Timber.d("## Verification QR: Invalid QR Code Data")
            cancel(CancelCode.QrCodeInvalid)
            return
        }

        // Perform some checks
        if (otherQrCodeData.transactionId != transactionId) {
            Timber.d("## Verification QR: Invalid transaction actual ${otherQrCodeData.transactionId} expected:$transactionId")
            cancel(CancelCode.QrCodeInvalid)
            return
        }

        // check master key
        val myMasterKey = crossSigningService.getUserCrossSigningKeys(userId)?.masterKey()?.unpaddedBase64PublicKey
        var canTrustOtherUserMasterKey = false

        // Check the other device view of my MSK
        when (otherQrCodeData) {
            is QrCodeData.VerifyingAnotherUser             -> {
                // key2 (aka otherUserMasterCrossSigningPublicKey) is what the one displaying the QR code (other user) think my MSK is.
                // Let's check that it's correct
                // If not -> Cancel
                if (otherQrCodeData.otherUserMasterCrossSigningPublicKey != myMasterKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.otherUserMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else Unit
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted    -> {
                // key1 (aka userMasterCrossSigningPublicKey) is the session displaying the QR code view of our MSK.
                // Let's check that I see the same MSK
                // If not -> Cancel
                if (otherQrCodeData.userMasterCrossSigningPublicKey != myMasterKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else {
                    // I can trust the MSK then (i see the same one, and other session tell me it's trusted by him)
                    canTrustOtherUserMasterKey = true
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                // key2 (aka userMasterCrossSigningPublicKey) is the session displaying the QR code view of our MSK.
                // Let's check that it's the good one
                // If not -> Cancel
                if (otherQrCodeData.userMasterCrossSigningPublicKey != myMasterKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else {
                    // Nothing special here, we will send a reciprocate start event, and then the other session will trust it's view of the MSK
                }
            }
        }

        val toVerifyDeviceIds = mutableListOf<String>()

        // Let's now check the other user/device key material
        when (otherQrCodeData) {
            is QrCodeData.VerifyingAnotherUser             -> {
                // key1(aka userMasterCrossSigningPublicKey) is the MSK of the one displaying the QR code (i.e other user)
                // Let's check that it matches what I think it should be
                if (otherQrCodeData.userMasterCrossSigningPublicKey
                        != crossSigningService.getUserCrossSigningKeys(otherUserId)?.masterKey()?.unpaddedBase64PublicKey) {
                    Timber.d("## Verification QR: Invalid user master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else {
                    // It does so i should mark it as trusted
                    canTrustOtherUserMasterKey = true
                    Unit
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted    -> {
                // key2 (aka otherDeviceKey) is my current device key in POV of the one displaying the QR code (i.e other device)
                // Let's check that it's correct
                if (otherQrCodeData.otherDeviceKey
                        != cryptoStore.getUserDevice(userId, deviceId)?.fingerprint()) {
                    Timber.d("## Verification QR: Invalid other device key ${otherQrCodeData.otherDeviceKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else Unit // Nothing special here, we will send a reciprocate start event, and then the other session will trust my device
                // and thus allow me to request SSSS secret
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                // key1 (aka otherDeviceKey) is the device key of the one displaying the QR code (i.e other device)
                // Let's check that it matches what I have locally
                if (otherQrCodeData.deviceKey
                        != cryptoStore.getUserDevice(otherUserId, otherDeviceId ?: "")?.fingerprint()) {
                    Timber.d("## Verification QR: Invalid device key ${otherQrCodeData.deviceKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else {
                    // Yes it does -> i should trust it and sign then upload the signature
                    toVerifyDeviceIds.add(otherDeviceId ?: "")
                    Unit
                }
            }
        }

        if (!canTrustOtherUserMasterKey && toVerifyDeviceIds.isEmpty()) {
            // Nothing to verify
            cancel(CancelCode.MismatchedKeys)
            return
        }

        // All checks are correct
        // Send the shared secret so that sender can trust me
        // qrCodeData.sharedSecret will be used to send the start request
        start(otherQrCodeData.sharedSecret)

        trust(
                canTrustOtherUserMasterKey = canTrustOtherUserMasterKey,
                toVerifyDeviceIds = toVerifyDeviceIds.distinct(),
                eventuallyMarkMyMasterKeyAsTrusted = true,
                autoDone = false
        )
    }

    private fun start(remoteSecret: String, onDone: (() -> Unit)? = null) {
        if (state != VerificationTxState.None) {
            Timber.e("## Verification QR: start verification from invalid state")
            // should I cancel??
            throw IllegalStateException("Interactive Key verification already started")
        }

        state = VerificationTxState.Started
        val startMessage = transport.createStartForQrCode(
                deviceId,
                transactionId,
                remoteSecret
        )

        transport.sendToOther(
                EventType.KEY_VERIFICATION_START,
                startMessage,
                VerificationTxState.WaitingOtherReciprocateConfirm,
                CancelCode.User,
                onDone
        )
    }

    override fun cancel() {
        cancel(CancelCode.User)
    }

    override fun cancel(code: CancelCode) {
        state = VerificationTxState.Cancelled(code, true)
        transport.cancelTransaction(transactionId, otherUserId, otherDeviceId ?: "", code)
    }

    override fun isToDeviceTransport() = false

    // Other user has scanned our QR code. check that the secret matched, so we can trust him
    fun onStartReceived(startReq: ValidVerificationInfoStart.ReciprocateVerificationInfoStart) {
        if (qrCodeData == null) {
            // Should not happen
            cancel(CancelCode.UnexpectedMessage)
            return
        }

        if (startReq.sharedSecret.fromBase64Safe()?.contentEquals(qrCodeData.sharedSecret.fromBase64()) == true) {
            // Ok, we can trust the other user
            // We can only trust the master key in this case
            // But first, ask the user for a confirmation
            state = VerificationTxState.QrScannedByOther
        } else {
            // Display a warning
            cancel(CancelCode.MismatchedKeys)
        }
    }

    fun onDoneReceived() {
        if (state != VerificationTxState.WaitingOtherReciprocateConfirm) {
            cancel(CancelCode.UnexpectedMessage)
            return
        }
        state = VerificationTxState.Verified
        transport.done(transactionId) {}
    }

    override fun otherUserScannedMyQrCode() {
        when (qrCodeData) {
            is QrCodeData.VerifyingAnotherUser             -> {
                // Alice telling Bob that the code was scanned successfully is sufficient for Bob to trust Alice's key,
                trust(true, emptyList(), false)
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted    -> {
                // I now know that I have the correct device key for other session,
                // and can sign it with the self-signing key and upload the signature
                trust(false, listOf(otherDeviceId ?: ""), false)
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                // I now know that i can trust my MSK
                trust(true, emptyList(), true)
            }
            null                                           -> Unit
        }
    }

    override fun otherUserDidNotScannedMyQrCode() {
        // What can I do then?
        // At least remove the transaction...
        cancel(CancelCode.MismatchedKeys)
    }
}
