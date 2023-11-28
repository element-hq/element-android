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
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.OwnUserIdentity
import org.matrix.android.sdk.internal.crypto.UserIdentity
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.toValue
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.rustcomponents.sdk.crypto.VerificationRequestState
import timber.log.Timber
import javax.inject.Inject

/**
 * A helper class to deserialize to-device `m.key.verification.*` events to fetch the transaction id out.
 */
@JsonClass(generateAdapter = true)
internal data class ToDeviceVerificationEvent(
        @Json(name = "sender") val sender: String?,
        @Json(name = "transaction_id") val transactionId: String
)

/** Helper method to fetch the unique ID of the verification event. */
private fun getFlowId(event: Event): String? {
    return if (event.eventId != null) {
        event.getRelationContent()?.eventId
    } else {
        val content = event.getClearContent().toModel<ToDeviceVerificationEvent>() ?: return null
        content.transactionId
    }
}

/** Convert a list of VerificationMethod into a list of strings that can be passed to the Rust side. */
internal fun prepareMethods(methods: List<VerificationMethod>): List<String> {
    val stringMethods: MutableList<String> = methods.map { it.toValue() }.toMutableList()

    if (stringMethods.contains(VERIFICATION_METHOD_QR_CODE_SHOW) ||
            stringMethods.contains(VERIFICATION_METHOD_QR_CODE_SCAN)) {
        stringMethods.add(VERIFICATION_METHOD_RECIPROCATE)
    }

    return stringMethods
}

@SessionScope
internal class RustVerificationService @Inject constructor(
        private val olmMachine: OlmMachine,
        private val verificationListenersHolder: VerificationListenersHolder) : VerificationService {

    override fun requestEventFlow() = verificationListenersHolder.eventFlow

    /**
     *
     * All verification related events should be forwarded through this method to
     * the verification service.
     *
     * This method mainly just fetches the appropriate rust object that will be created or updated by the event and
     * dispatches updates to our listeners.
     */
    internal suspend fun onEvent(roomId: String?, event: Event) {
        if (roomId != null && event.unsignedData?.transactionId == null) {
            if (isVerificationEvent(event)) {
                try {
                    val clearEvent = if (event.isEncrypted()) {
                        event.copy(
                                content = event.getDecryptedContent(),
                                type = event.getDecryptedType(),
                                roomId = roomId
                        )
                    } else {
                        event
                    }
                    olmMachine.receiveVerificationEvent(roomId, clearEvent)
                } catch (failure: Throwable) {
                    Timber.w(failure, "Failed to receiveUnencryptedVerificationEvent ${failure.message}")
                }
            }
        }
        when (event.getClearType()) {
            EventType.KEY_VERIFICATION_REQUEST -> onRequest(event, fromRoomMessage = false)
            EventType.KEY_VERIFICATION_START -> onStart(event)
            EventType.KEY_VERIFICATION_READY,
            EventType.KEY_VERIFICATION_ACCEPT,
            EventType.KEY_VERIFICATION_KEY,
            EventType.KEY_VERIFICATION_MAC,
            EventType.KEY_VERIFICATION_CANCEL,
            EventType.KEY_VERIFICATION_DONE -> onUpdate(event)
            EventType.MESSAGE -> onRoomMessage(event)
            else -> Unit
        }
    }

    private fun isVerificationEvent(event: Event): Boolean {
        val eventType = event.getClearType()
        val eventContent = event.getClearContent() ?: return false
        return EventType.isVerificationEvent(eventType) ||
                (eventType == EventType.MESSAGE &&
                        eventContent[MessageContent.MSG_TYPE_JSON_KEY] == MessageType.MSGTYPE_VERIFICATION_REQUEST)
    }

    private suspend fun onRoomMessage(event: Event) {
        val messageContent = event.getClearContent()?.toModel<MessageContent>() ?: return
        if (messageContent.msgType == MessageType.MSGTYPE_VERIFICATION_REQUEST) {
            onRequest(event, fromRoomMessage = true)
        }
    }

    /** Dispatch updates after a verification event has been received. */
    private suspend fun onUpdate(event: Event) {
        Timber.v("[${olmMachine.userId().take(6)}] Verification on event ${event.getClearType()}")
        val sender = event.senderId ?: return
        val flowId = getFlowId(event) ?: return Unit.also {
            Timber.w("onUpdate for unknown flowId senderId ${event.getClearType()}")
        }

        val verificationRequest = olmMachine.getVerificationRequest(sender, flowId)
        if (event.getClearType() == EventType.KEY_VERIFICATION_READY) {
            // we start the qr here in order to display the code
            verificationRequest?.startQrCode()
        }
    }

    /** Check if the start event created new verification objects and dispatch updates. */
    private suspend fun onStart(event: Event) {
        if (event.unsignedData?.transactionId != null) return // remote echo
        val sender = event.senderId ?: return
        val flowId = getFlowId(event) ?: return

        // The events have already been processed by the sdk
        // The transaction are already created, we are just reacting here
        val transaction = getExistingTransaction(sender, flowId) ?: return Unit.also {
            Timber.w("onStart for unknown flowId $flowId senderId $sender")
        }

        val request = olmMachine.getVerificationRequest(sender, flowId)
        Timber.d("## Verification: matching request $request")

        if (request != null) {
            // If this is a SAS verification originating from a `m.key.verification.request`
            // event, we auto-accept here considering that we either initiated the request or
            // accepted the request. If it's a QR code verification, just dispatch an update.
            if (request.innerState() is VerificationRequestState.Ready && transaction is SasVerification) {
                // accept() will dispatch an update, no need to do it twice.
                Timber.d("## Verification: Auto accepting SAS verification with $sender")
                transaction.accept()
            }

            Timber.d("## Verification: start for $sender")
            // update the request as the start updates it's state
            verificationListenersHolder.dispatchRequestUpdated(request)
            verificationListenersHolder.dispatchTxUpdated(transaction)
        } else {
            // This didn't originate from a request, so tell our listeners that
            // this is a new verification.
            verificationListenersHolder.dispatchTxAdded(transaction)
            // The IncomingVerificationRequestHandler seems to only listen to updates
            // so let's trigger an update after the addition as well.
            verificationListenersHolder.dispatchTxUpdated(transaction)
        }
    }

    /** Check if the request event created a nev verification request object and dispatch that it dis so. */
    private suspend fun onRequest(event: Event, fromRoomMessage: Boolean) {
        val flowId = if (fromRoomMessage) {
            event.eventId
        } else {
            event.getClearContent().toModel<ToDeviceVerificationEvent>()?.transactionId
        } ?: return
        val sender = event.senderId ?: return
        val request = olmMachine.getVerificationRequest(sender, flowId) ?: return

        verificationListenersHolder.dispatchRequestAdded(request)
    }

    override suspend fun markedLocallyAsManuallyVerified(userId: String, deviceID: String) {
        olmMachine.getDevice(userId, deviceID)?.markAsTrusted()
    }

    override suspend fun getExistingTransaction(
            otherUserId: String,
            tid: String,
    ): VerificationTransaction? {
        return olmMachine.getVerification(otherUserId, tid)
    }

    override suspend fun getExistingVerificationRequests(
            otherUserId: String
    ): List<PendingVerificationRequest> {
        return olmMachine.getVerificationRequests(otherUserId).map {
            it.toPendingVerificationRequest()
        }
    }

    override suspend fun getExistingVerificationRequest(
            otherUserId: String,
            tid: String?
    ): PendingVerificationRequest? {
        return if (tid != null) {
            olmMachine.getVerificationRequest(otherUserId, tid)?.toPendingVerificationRequest()
        } else {
            null
        }
    }

    override suspend fun getExistingVerificationRequestInRoom(
            roomId: String,
            tid: String
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

    override suspend fun requestSelfKeyVerification(methods: List<VerificationMethod>): PendingVerificationRequest {
        val verification = when (val identity = olmMachine.getIdentity(olmMachine.userId())) {
            is OwnUserIdentity -> identity.requestVerification(methods)
            is UserIdentity    -> throw IllegalArgumentException("This method doesn't support verification of other users devices")
            null               -> throw IllegalArgumentException("Cross signing has not been bootstrapped for our own user")
        }
        return verification.toPendingVerificationRequest()
    }

    override suspend fun requestKeyVerificationInDMs(
            methods: List<VerificationMethod>,
            otherUserId: String,
            roomId: String,
            localId: String?
    ): PendingVerificationRequest {
        Timber.w("verification: requestKeyVerificationInDMs in room $roomId with $otherUserId")
        olmMachine.ensureUsersKeys(listOf(otherUserId), true)
        val verification = when (val identity = olmMachine.getIdentity(otherUserId)) {
            is UserIdentity    -> identity.requestVerification(methods, roomId, localId!!)
            is OwnUserIdentity -> throw IllegalArgumentException("This method doesn't support verification of our own user")
            null               -> throw IllegalArgumentException("The user that we wish to verify doesn't support cross signing")
        }

        return verification.toPendingVerificationRequest()
    }

    override suspend fun requestDeviceVerification(methods: List<VerificationMethod>,
                                                   otherUserId: String,
                                                   otherDeviceId: String?): PendingVerificationRequest {
        // how do we send request to several devices in rust?
        olmMachine.ensureUsersKeys(listOf(otherUserId))
        val request = if (otherDeviceId == null) {
            // Todo
            when (val identity = olmMachine.getIdentity(otherUserId)) {
                is OwnUserIdentity -> identity.requestVerification(methods)
                is UserIdentity -> {
                    throw IllegalArgumentException("to_device request only allowed for own user $otherUserId")
                }
                null -> throw IllegalArgumentException("Unknown identity")
            }
        } else {
            val otherDevice = olmMachine.getDevice(otherUserId, otherDeviceId)
            otherDevice?.requestVerification(methods) ?: throw IllegalArgumentException("Unknown device $otherDeviceId")
        }
        return request.toPendingVerificationRequest()
    }

    override suspend fun readyPendingVerification(
            methods: List<VerificationMethod>,
            otherUserId: String,
            transactionId: String
    ): Boolean {
        val request = olmMachine.getVerificationRequest(otherUserId, transactionId)
        return if (request != null) {
            request.acceptWithMethods(methods)
            request.startQrCode()
            request.innerState() is VerificationRequestState.Ready
        } else {
            false
        }
    }

    override suspend fun startKeyVerification(method: VerificationMethod, otherUserId: String, requestId: String): String? {
        return if (method == VerificationMethod.SAS) {
            val request = olmMachine.getVerificationRequest(otherUserId, requestId)
                    ?: throw UnsupportedOperationException("Unknown request with id: $requestId")

            val sas = request.startSasVerification()

            if (sas != null) {
                verificationListenersHolder.dispatchTxAdded(sas)
                // we need to update the request as the state mapping depends on the
                // sas or qr beeing started
                verificationListenersHolder.dispatchRequestUpdated(request)
                sas.transactionId
            } else {
                Timber.w("Failed to start verification with method $method")
                null
            }
        } else {
            throw UnsupportedOperationException("Unknown verification method")
        }
    }

    override suspend fun reciprocateQRVerification(otherUserId: String, requestId: String, scannedData: String): String? {
        val matchingRequest = olmMachine.getVerificationRequest(otherUserId, requestId)
                ?: return null
        val qrVerification = matchingRequest.scanQrCode(scannedData)
                ?: return null
        verificationListenersHolder.dispatchTxAdded(qrVerification)
        // we need to update the request as the state mapping depends on the
        // sas or qr beeing started
        verificationListenersHolder.dispatchRequestUpdated(matchingRequest)
        return qrVerification.transactionId
    }

    override suspend fun onPotentiallyInterestingEventRoomFailToDecrypt(event: Event) {
        // not available in rust
    }

    override suspend fun declineVerificationRequestInDMs(otherUserId: String, transactionId: String, roomId: String) {
        cancelVerificationRequest(otherUserId, transactionId)
    }

//    override suspend fun beginDeviceVerification(otherUserId: String, otherDeviceId: String): String? {
//        // This starts the short SAS flow, the one that doesn't start with
//        // a `m.key.verification.request`, Element web stopped doing this, might
//        // be wise do do so as well
//        // DeviceListBottomSheetViewModel triggers this, interestingly the method that
//        // triggers this is called `manuallyVerify()`
//        val otherDevice = olmMachine.getDevice(otherUserId, otherDeviceId)
//        val verification = otherDevice?.startVerification()
//        return if (verification != null) {
//            verificationListenersHolder.dispatchTxAdded(verification)
//            verification.transactionId
//        } else {
//            null
//        }
//    }

    override suspend fun cancelVerificationRequest(request: PendingVerificationRequest) {
        cancelVerificationRequest(request.otherUserId, request.transactionId)
    }

    override suspend fun cancelVerificationRequest(otherUserId: String, transactionId: String) {
        val verificationRequest = olmMachine.getVerificationRequest(otherUserId, transactionId)
        verificationRequest?.cancel()
    }
}
