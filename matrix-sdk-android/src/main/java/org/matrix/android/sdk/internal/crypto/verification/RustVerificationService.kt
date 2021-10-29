/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.internal.crypto.OlmMachineProvider
import org.matrix.android.sdk.internal.crypto.OwnUserIdentity
import org.matrix.android.sdk.internal.crypto.SasVerification
import org.matrix.android.sdk.internal.crypto.UserIdentity
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.toValue
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

/** A helper class to deserialize to-device `m.key.verification.*` events to fetch the transaction id out */
@JsonClass(generateAdapter = true)
internal data class ToDeviceVerificationEvent(
        @Json(name = "sender") val sender: String?,
        @Json(name = "transaction_id") val transactionId: String
)

/** Helper method to fetch the unique ID of the verification event */
private fun getFlowId(event: Event): String? {
    return if (event.eventId != null) {
        val relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
        relatesTo?.eventId
    } else {
        val content = event.getClearContent().toModel<ToDeviceVerificationEvent>() ?: return null
        content.transactionId
    }
}

/** Convert a list of VerificationMethod into a list of strings that can be passed to the Rust side */
internal fun prepareMethods(methods: List<VerificationMethod>): List<String> {
    val stringMethods: MutableList<String> = methods.map { it.toValue() }.toMutableList()

    if (stringMethods.contains(VERIFICATION_METHOD_QR_CODE_SHOW) ||
            stringMethods.contains(VERIFICATION_METHOD_QR_CODE_SCAN)) {
        stringMethods.add(VERIFICATION_METHOD_RECIPROCATE)
    }

    return stringMethods
}

@SessionScope
internal class RustVerificationService @Inject constructor(private val olmMachineProvider: OlmMachineProvider) : VerificationService {

    val olmMachine by lazy {
        olmMachineProvider.olmMachine
    }

    private val dispatcher = UpdateDispatcher(this.olmMachine.verificationListeners)

    /** The main entry point for the verification service
     *
     * All verification related events should be forwarded through this method to
     * the verification service.
     *
     * Since events are at this point already handled by the rust-sdk through the receival
     * of the to-device events and the decryption of room events, this method mainly just
     * fetches the appropriate rust object that will be created or updated by the event and
     * dispatches updates to our listeners.
     */
    internal suspend fun onEvent(event: Event) = when (event.getClearType()) {
        // I'm not entirely sure why getClearType() returns a msgtype in one case
        // and a event type in the other case, but this is how the old verification
        // service did things and it does seem to work.
        MessageType.MSGTYPE_VERIFICATION_REQUEST -> onRequest(event)
        EventType.KEY_VERIFICATION_START         -> onStart(event)
        EventType.KEY_VERIFICATION_READY,
        EventType.KEY_VERIFICATION_ACCEPT,
        EventType.KEY_VERIFICATION_KEY,
        EventType.KEY_VERIFICATION_MAC,
        EventType.KEY_VERIFICATION_CANCEL,
        EventType.KEY_VERIFICATION_DONE          -> onUpdate(event)
        else                                     -> {
        }
    }

    /** Dispatch updates after a verification event has been received */
    private fun onUpdate(event: Event) {
        val sender = event.senderId ?: return
        val flowId = getFlowId(event) ?: return

        this.olmMachine.getVerificationRequest(sender, flowId)?.dispatchRequestUpdated()
        val verification = this.getExistingTransaction(sender, flowId) ?: return
        this.dispatcher.dispatchTxUpdated(verification)
    }

    /** Check if the start event created new verification objects and dispatch updates */
    private suspend fun onStart(event: Event) {
        val sender = event.senderId ?: return
        val flowId = getFlowId(event) ?: return

        val verification = this.getExistingTransaction(sender, flowId) ?: return
        val request = this.olmMachine.getVerificationRequest(sender, flowId)

        if (request != null && request.isReady()) {
            // If this is a SAS verification originating from a `m.key.verification.request`
            // event, we auto-accept here considering that we either initiated the request or
            // accepted the request. If it's a QR code verification, just dispatch an update.
            if (verification is SasVerification) {
                // accept() will dispatch an update, no need to do it twice.
                Timber.d("## Verification: Auto accepting SAS verification with $sender")
                verification.accept()
            } else {
                this.dispatcher.dispatchTxUpdated(verification)
            }
        } else {
            // This didn't originate from a request, so tell our listeners that
            // this is a new verification.
            this.dispatcher.dispatchTxAdded(verification)
            // The IncomingVerificationRequestHandler seems to only listen to updates
            // so let's trigger an update after the addition as well.
            this.dispatcher.dispatchTxUpdated(verification)
        }
    }

    /** Check if the request event created a nev verification request object and dispatch that it dis so */
    private fun onRequest(event: Event) {
        val flowId = getFlowId(event) ?: return
        val sender = event.senderId ?: return

        val request = this.getExistingVerificationRequest(sender, flowId) ?: return

        this.dispatcher.dispatchRequestAdded(request)
    }

    override fun addListener(listener: VerificationService.Listener) {
        this.dispatcher.addListener(listener)
    }

    override fun removeListener(listener: VerificationService.Listener) {
        this.dispatcher.removeListener(listener)
    }

    override fun markedLocallyAsManuallyVerified(userId: String, deviceID: String) {
        // TODO this doesn't seem to be used anymore?
        runBlocking {
            val device = olmMachine.getDevice(userId, deviceID)
            device?.markAsTrusted()
        }
    }

    override fun onPotentiallyInterestingEventRoomFailToDecrypt(event: Event) {
        // TODO This should be handled inside the rust-sdk decryption method
    }

    override fun getExistingTransaction(
            otherUserId: String,
            tid: String,
    ): VerificationTransaction? {
        return this.olmMachine.getVerification(otherUserId, tid)
    }

    override fun getExistingVerificationRequests(
            otherUserId: String
    ): List<PendingVerificationRequest> {
        return this.olmMachine.getVerificationRequests(otherUserId).map {
            it.toPendingVerificationRequest()
        }
    }

    override fun getExistingVerificationRequest(
            otherUserId: String,
            tid: String?
    ): PendingVerificationRequest? {
        return if (tid != null) {
            this.olmMachine.getVerificationRequest(otherUserId, tid)?.toPendingVerificationRequest()
        } else {
            null
        }
    }

    override fun getExistingVerificationRequestInRoom(
            roomId: String,
            tid: String?
    ): PendingVerificationRequest? {
        // This is only used in `RoomDetailViewModel` to resume the verification.
        //
        // Is this actually useful? SAS and QR code verifications ephemeral nature
        // due to the usage of ephemeral secrets. In the case of SAS verification, the
        // ephemeral key can't be stored due to libolm missing support for it, I would
        // argue that the ephemeral secret for QR verifications shouldn't be persisted either.
        //
        // This means that once we transition from a verification request into an actual
        // verification flow (SAS/QR) we won't be able to resume. In other words resumption
        // is only supported before both sides agree to verify.
        //
        // We would either need to remember if the request transitioned into a flow and only
        // support resumption if we didn't, otherwise we would risk getting different emojis
        // or secrets in the QR code, not to mention that the flows could be interrupted in
        // any non-starting state.
        //
        // In any case, we don't support resuming in the rust-sdk, so let's return null here.
        return null
    }

    override fun requestKeyVerification(
            methods: List<VerificationMethod>,
            otherUserId: String,
            otherDevices: List<String>?
    ): PendingVerificationRequest {
        val verification = when (val identity = runBlocking { olmMachine.getIdentity(otherUserId) }) {
            is OwnUserIdentity -> runBlocking { identity.requestVerification(methods) }
            is UserIdentity    -> throw IllegalArgumentException("This method doesn't support verification of other users devices")
            null               -> throw IllegalArgumentException("Cross signing has not been bootstrapped for our own user")
        }

        return verification.toPendingVerificationRequest()
    }

    override fun requestKeyVerificationInDMs(
            methods: List<VerificationMethod>,
            otherUserId: String,
            roomId: String,
            localId: String?
    ): PendingVerificationRequest {
        Timber.i("## SAS Requesting verification to user: $otherUserId in room $roomId")
        val verification = when (val identity = runBlocking { olmMachine.getIdentity(otherUserId) }) {
            is UserIdentity    -> runBlocking { identity.requestVerification(methods, roomId, localId!!) }
            is OwnUserIdentity -> throw IllegalArgumentException("This method doesn't support verification of our own user")
            null               -> throw IllegalArgumentException("The user that we wish to verify doesn't support cross signing")
        }

        return verification.toPendingVerificationRequest()
    }

    override fun readyPendingVerification(
            methods: List<VerificationMethod>,
            otherUserId: String,
            transactionId: String
    ): Boolean {
        val request = this.olmMachine.getVerificationRequest(otherUserId, transactionId)

        return if (request != null) {
            runBlocking { request.acceptWithMethods(methods) }

            if (request.isReady()) {
                val qrcode = request.startQrVerification()

                if (qrcode != null) {
                    this.dispatcher.dispatchTxAdded(qrcode)
                }

                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override fun readyPendingVerificationInDMs(
            methods: List<VerificationMethod>,
            otherUserId: String,
            roomId: String,
            transactionId: String
    ): Boolean {
        return readyPendingVerification(methods, otherUserId, transactionId)
    }

    override fun beginKeyVerification(
            method: VerificationMethod,
            otherUserId: String,
            otherDeviceId: String,
            transactionId: String?
    ): String? {
        return if (method == VerificationMethod.SAS) {
            if (transactionId != null) {
                val request = this.olmMachine.getVerificationRequest(otherUserId, transactionId)

                runBlocking {
                    val sas = request?.startSasVerification()

                    if (sas != null) {
                        dispatcher.dispatchTxAdded(sas)
                        sas.transactionId
                    } else {
                        null
                    }
                }
            } else {
                // This starts the short SAS flow, the one that doesn't start with
                // a `m.key.verification.request`, Element web stopped doing this, might
                // be wise do do so as well
                // DeviceListBottomSheetViewModel triggers this, interestingly the method that
                // triggers this is called `manuallyVerify()`
                runBlocking {
                    val verification = olmMachine.getDevice(otherUserId, otherDeviceId)?.startVerification()
                    if (verification != null) {
                        dispatcher.dispatchTxAdded(verification)
                        verification.transactionId
                    } else {
                        null
                    }
                }
            }
        } else {
            throw IllegalArgumentException("Unknown verification method")
        }
    }

    override fun beginKeyVerificationInDMs(
            method: VerificationMethod,
            transactionId: String,
            roomId: String,
            otherUserId: String,
            otherDeviceId: String
    ): String {
        beginKeyVerification(method, otherUserId, otherDeviceId, transactionId)
        // TODO what's the point of returning the same ID we got as an argument?
        // We do this because the old verification service did so
        return transactionId
    }

    override fun cancelVerificationRequest(request: PendingVerificationRequest) {
        val verificationRequest = request.transactionId?.let {
            this.olmMachine.getVerificationRequest(request.otherUserId, it)
        }
        runBlocking { verificationRequest?.cancel() }
    }

    override fun declineVerificationRequestInDMs(
            otherUserId: String,
            transactionId: String,
            roomId: String
    ) {
        val verificationRequest = this.olmMachine.getVerificationRequest(otherUserId, transactionId)
        runBlocking { verificationRequest?.cancel() }
    }
}
