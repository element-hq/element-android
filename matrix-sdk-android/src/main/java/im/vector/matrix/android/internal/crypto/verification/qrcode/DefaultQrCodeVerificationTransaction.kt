/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.verification.qrcode

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningService
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.QrCodeVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationTxState
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.actions.SetDeviceVerificationAction
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.verification.DefaultVerificationTransaction
import im.vector.matrix.android.internal.crypto.verification.VerificationInfo
import im.vector.matrix.android.internal.crypto.verification.VerificationInfoStart
import im.vector.matrix.android.internal.util.exhaustive
import timber.log.Timber

internal class DefaultQrCodeVerificationTransaction(
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        override val transactionId: String,
        override val otherUserId: String,
        override var otherDeviceId: String?,
        private val crossSigningService: CrossSigningService,
        private val cryptoStore: IMXCryptoStore,
        // Not null only if other user is able to scan QR code
        private val qrCodeData: QrCodeData?,
        val userId: String,
        val deviceId: String,
        override val isIncoming: Boolean
) : DefaultVerificationTransaction(transactionId, otherUserId, otherDeviceId, isIncoming), QrCodeVerificationTransaction {

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
        when (otherQrCodeData) {
            is QrCodeData.VerifyingAnotherUser             -> {
                if (otherQrCodeData.otherUserMasterCrossSigningPublicKey
                        != crossSigningService.getUserCrossSigningKeys(userId)?.masterKey()?.unpaddedBase64PublicKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.otherUserMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else Unit
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted    -> {
                if (otherQrCodeData.userMasterCrossSigningPublicKey
                        != crossSigningService.getUserCrossSigningKeys(userId)?.masterKey()?.unpaddedBase64PublicKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else Unit
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                if (otherQrCodeData.userMasterCrossSigningPublicKey
                        != crossSigningService.getUserCrossSigningKeys(userId)?.masterKey()?.unpaddedBase64PublicKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else Unit
            }
        }.exhaustive

        val toVerifyDeviceIds = mutableListOf<String>()
        var canTrustOtherUserMasterKey = false

        // Check device key if available
        when (otherQrCodeData) {
            is QrCodeData.VerifyingAnotherUser             -> {
                if (otherQrCodeData.userMasterCrossSigningPublicKey
                        != crossSigningService.getUserCrossSigningKeys(otherUserId)?.masterKey()?.unpaddedBase64PublicKey) {
                    Timber.d("## Verification QR: Invalid user master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else {
                    canTrustOtherUserMasterKey = true
                    Unit
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted    -> {
                if (otherQrCodeData.otherDeviceKey
                        != cryptoStore.getUserDevice(userId, deviceId)?.fingerprint()) {
                    Timber.d("## Verification QR: Invalid other device key ${otherQrCodeData.otherDeviceKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else Unit
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                if (otherQrCodeData.deviceKey
                        != cryptoStore.getUserDevice(otherUserId, otherDeviceId ?: "")?.fingerprint()) {
                    Timber.d("## Verification QR: Invalid device key ${otherQrCodeData.deviceKey}")
                    cancel(CancelCode.MismatchedKeys)
                    return
                } else {
                    toVerifyDeviceIds.add(otherQrCodeData.deviceKey)
                    Unit
                }
            }
        }.exhaustive

        if (!canTrustOtherUserMasterKey && toVerifyDeviceIds.isEmpty()) {
            // Nothing to verify
            cancel(CancelCode.MismatchedKeys)
            return
        }

        // All checks are correct
        // Send the shared secret so that sender can trust me
        // qrCodeData.sharedSecret will be used to send the start request
        start(otherQrCodeData.sharedSecret)

        // Trust the other user
        trust(canTrustOtherUserMasterKey, toVerifyDeviceIds.distinct())
    }

    fun start(remoteSecret: String) {
        if (state != VerificationTxState.None) {
            Timber.e("## Verification QR: start verification from invalid state")
            // should I cancel??
            throw IllegalStateException("Interactive Key verification already started")
        }

        val startMessage = transport.createStartForQrCode(
                deviceId,
                transactionId,
                remoteSecret
        )

        transport.sendToOther(
                EventType.KEY_VERIFICATION_START,
                startMessage,
                VerificationTxState.Started,
                CancelCode.User,
                null
        )
    }

    override fun acceptVerificationEvent(senderId: String, info: VerificationInfo) {
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
    fun onStartReceived(startReq: VerificationInfoStart) {
        if (qrCodeData == null) {
            // Should not happen
            cancel(CancelCode.UnexpectedMessage)
            return
        }

        if (startReq.sharedSecret == qrCodeData.sharedSecret) {
            // Ok, we can trust the other user
            // We can only trust the master key in this case
            // But first, ask the user for a confirmation
            state = VerificationTxState.QrScannedByOther
        } else {
            // Display a warning
            cancel(CancelCode.MismatchedKeys)
        }
    }

    override fun otherUserScannedMyQrCode() {
        trust(true, emptyList())
    }

    override fun otherUserDidNotScannedMyQrCode() {
        // What can I do then?
        // At least remove the transaction...
        state = VerificationTxState.Cancelled(CancelCode.MismatchedKeys, true)
    }

    private fun trust(canTrustOtherUserMasterKey: Boolean, toVerifyDeviceIds: List<String>) {
        // If not me sign his MSK and upload the signature
        if (canTrustOtherUserMasterKey) {
            if (otherUserId != userId ) {
                // we should trust this master key
                // And check verification MSK -> SSK?
                crossSigningService.trustUser(otherUserId, object : MatrixCallback<Unit> {
                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## QR Verification: Failed to trust User $otherUserId")
                    }
                })
            } else {
                //Mark my keys as trusted locally
                crossSigningService.markMyMasterKeyAsTrusted()
            }
        }

        if (otherUserId == userId) {
            // If me it's reasonable to sign and upload the device signature
            // Notice that i might not have the private keys, so may not be able to do it
            crossSigningService.signDevice(otherDeviceId!!, object : MatrixCallback<Unit> {
                override fun onFailure(failure: Throwable) {
                    Timber.w(failure, "## QR Verification: Failed to sign new device $otherDeviceId")
                }
            })
        }

        // TODO what if the otherDevice is not in this list? and should we
        toVerifyDeviceIds.forEach {
            setDeviceVerified(otherUserId, it)
        }
        transport.done(transactionId)
        state = VerificationTxState.Verified
    }

    private fun setDeviceVerified(userId: String, deviceId: String) {
        // TODO should not override cross sign status
        setDeviceVerificationAction.handle(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true),
                userId,
                deviceId)
    }
}
