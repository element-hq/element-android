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

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.internal.crypto.IncomingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.OutgoingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import timber.log.Timber

/**
 * Generic interactive key verification transaction
 */
internal abstract class DefaultVerificationTransaction(
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        private val crossSigningService: CrossSigningService,
        private val outgoingGossipingRequestManager: OutgoingGossipingRequestManager,
        private val incomingGossipingRequestManager: IncomingGossipingRequestManager,
        private val userId: String,
        override val transactionId: String,
        override val otherUserId: String,
        override var otherDeviceId: String? = null,
        override val isIncoming: Boolean) : VerificationTransaction {

    lateinit var transport: VerificationTransport

    interface Listener {
        fun transactionUpdated(tx: VerificationTransaction)
    }

    protected var listeners = ArrayList<Listener>()

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    protected fun trust(canTrustOtherUserMasterKey: Boolean,
                        toVerifyDeviceIds: List<String>,
                        eventuallyMarkMyMasterKeyAsTrusted: Boolean, autoDone: Boolean = true) {
        Timber.d("## Verification: trust ($otherUserId,$otherDeviceId) , verifiedDevices:$toVerifyDeviceIds")
        Timber.d("## Verification: trust Mark myMSK trusted $eventuallyMarkMyMasterKeyAsTrusted")

        // TODO what if the otherDevice is not in this list? and should we
        toVerifyDeviceIds.forEach {
            setDeviceVerified(otherUserId, it)
        }

        // If not me sign his MSK and upload the signature
        if (canTrustOtherUserMasterKey) {
            // we should trust this master key
            // And check verification MSK -> SSK?
            if (otherUserId != userId) {
                crossSigningService.trustUser(otherUserId, object : MatrixCallback<Unit> {
                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## Verification: Failed to trust User $otherUserId")
                    }
                })
            } else {
                // Notice other master key is mine because other is me
                if (eventuallyMarkMyMasterKeyAsTrusted) {
                    // Mark my keys as trusted locally
                    crossSigningService.markMyMasterKeyAsTrusted()
                }
            }
        }

        if (otherUserId == userId) {
            incomingGossipingRequestManager.onVerificationCompleteForDevice(otherDeviceId!!)

            // If me it's reasonable to sign and upload the device signature
            // Notice that i might not have the private keys, so may not be able to do it
            crossSigningService.trustDevice(otherDeviceId!!, object : MatrixCallback<Unit> {
                override fun onFailure(failure: Throwable) {
                    Timber.w("## Verification: Failed to sign new device $otherDeviceId, ${failure.localizedMessage}")
                }
            })
        }

        if (autoDone) {
            state = VerificationTxState.Verified
            transport.done(transactionId) {}
        }
    }

    private fun setDeviceVerified(userId: String, deviceId: String) {
        // TODO should not override cross sign status
        setDeviceVerificationAction.handle(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true),
                userId,
                deviceId)
    }
}
