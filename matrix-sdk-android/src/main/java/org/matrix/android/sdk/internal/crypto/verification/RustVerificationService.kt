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

import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.RequestSender
import org.matrix.android.sdk.internal.crypto.SasVerification
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationDone
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationKey
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationRequest
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import uniffi.olm.OutgoingVerificationRequest

@SessionScope
internal class RustVerificationService
@Inject
constructor(
        private val olmMachine: OlmMachine,
        private val requestSender: RequestSender,
) : DefaultVerificationTransaction.Listener, VerificationService {

    private val uiHandler = Handler(Looper.getMainLooper())
    private var listeners = ArrayList<VerificationService.Listener>()

    override fun addListener(listener: VerificationService.Listener) {
        uiHandler.post {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    override fun removeListener(listener: VerificationService.Listener) {
        uiHandler.post { listeners.remove(listener) }
    }

    private fun dispatchTxAdded(tx: VerificationTransaction) {
        uiHandler.post {
            listeners.forEach {
                try {
                    it.transactionCreated(tx)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    private fun dispatchTxUpdated(tx: VerificationTransaction) {
        uiHandler.post {
            listeners.forEach {
                try {
                    it.transactionUpdated(tx)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    private fun dispatchRequestAdded(tx: PendingVerificationRequest) {
        Timber.v("## SAS dispatchRequestAdded txId:${tx.transactionId} $tx")
        uiHandler.post {
            listeners.forEach {
                try {
                    it.verificationRequestCreated(tx)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    private fun dispatchRequestUpdated(tx: PendingVerificationRequest) {
        uiHandler.post {
            listeners.forEach {
                try {
                    it.verificationRequestUpdated(tx)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    override fun markedLocallyAsManuallyVerified(userId: String, deviceID: String) {
        TODO()
        // setDeviceVerificationAction.handle(DeviceTrustLevel(false, true),
        //         userId,
        //         deviceID)

        // listeners.forEach {
        //     try {
        //         it.markedAsManuallyVerified(userId, deviceID)
        //     } catch (e: Throwable) {
        //         Timber.e(e, "## Error while notifying listeners")
        //     }
        // }
    }

    suspend fun onEvent(event: Event) {
        when (event.getClearType()) {
            EventType.KEY_VERIFICATION_START -> {}
            EventType.KEY_VERIFICATION_CANCEL -> {}
            EventType.KEY_VERIFICATION_ACCEPT -> {}
            EventType.KEY_VERIFICATION_KEY -> {
                onKeyReceived(event)
            }
            EventType.KEY_VERIFICATION_MAC -> {}
            EventType.KEY_VERIFICATION_READY -> {}
            EventType.KEY_VERIFICATION_DONE -> {
                onDone(event)
            }
            MessageType.MSGTYPE_VERIFICATION_REQUEST -> {
                onRequestReceived(event)
            }
            else -> {
                // ignore
            }
        }
    }

    private fun onDone(event: Event) {
        val content = event.getClearContent().toModel<KeyVerificationDone>() ?: return
        val flowId = content.transactionId ?: return
        val sender = event.senderId ?: return

        val verification = this.getExistingTransaction(sender, flowId) ?: return
        dispatchTxUpdated(verification)
    }
    private fun onKeyReceived(event: Event) {
        val content = event.getClearContent().toModel<KeyVerificationKey>() ?: return
        val flowId = content.transactionId ?: return
        val sender = event.senderId ?: return

        val verification = this.getExistingTransaction(sender, flowId) ?: return
        dispatchTxUpdated(verification)
    }

    private fun onRequestReceived(event: Event) {
        val content = event.getClearContent().toModel<KeyVerificationRequest>() ?: return
        val flowId = content.transactionId
        val sender = event.senderId ?: return

        val request = this.getExistingVerificationRequest(sender, flowId) ?: return

        dispatchRequestAdded(request)
    }

    override fun onPotentiallyInterestingEventRoomFailToDecrypt(event: Event) {
        // TODO This should be handled inside the rust-sdk decryption method
    }

    override fun getExistingTransaction(
            otherUserId: String,
            tid: String,
    ): VerificationTransaction? {
        val verification = this.olmMachine.getVerification(otherUserId, tid) ?: return null
        return SasVerification(this.olmMachine.inner(), verification, this.requestSender, this.listeners)
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
            olmMachine.getVerificationRequest(otherUserId, tid)?.toPendingVerificationRequest()
        } else {
            null
        }
    }

    override fun getExistingVerificationRequestInRoom(
            roomId: String,
            tid: String?
    ): PendingVerificationRequest? {
        TODO()
    }

    override fun beginKeyVerification(
            method: VerificationMethod,
            otherUserId: String,
            otherDeviceId: String,
            transactionId: String?
    ): String? {
        // should check if already one (and cancel it)
        return if (method == VerificationMethod.SAS) {
            val flowId = transactionId ?: return null
            val request = this.olmMachine.getVerificationRequest(otherUserId, flowId)
            runBlocking {
                val response = request?.startSasVerification()
                if (response != null) {
                    sendRequest(response.request)
                    val sas = SasVerification(olmMachine.inner(), response.sas, requestSender, listeners)
                    dispatchTxAdded(sas)
                    sas.transactionId
                } else {
                    null
                }
            }
        } else {
            throw IllegalArgumentException("Unknown verification method")
        }
    }

    override fun requestKeyVerificationInDMs(
            methods: List<VerificationMethod>,
            otherUserId: String,
            roomId: String,
            localId: String?
    ): PendingVerificationRequest {
        Timber.i("## SAS Requesting verification to user: $otherUserId in room $roomId")

        // TODO cancel other active requests, create a new request here and
        // dispatch it
        TODO()
    }

    override fun requestKeyVerification(
            methods: List<VerificationMethod>,
            otherUserId: String,
            otherDevices: List<String>?
    ): PendingVerificationRequest {
        // This was mostly a copy paste of the InDMs method, do the same here
        TODO()
    }

    override fun cancelVerificationRequest(request: PendingVerificationRequest) {
        // TODO get the request out of the olm machine and cancel here
        TODO()
    }

    override fun declineVerificationRequestInDMs(
            otherUserId: String,
            transactionId: String,
            roomId: String
    ) {
        // TODO get an existing verification request out of the olm machine and
        // cancel it. update the pending request afterwards
    }

    override fun beginKeyVerificationInDMs(
            method: VerificationMethod,
            transactionId: String,
            roomId: String,
            otherUserId: String,
            otherDeviceId: String
    ): String {
        // TODO fetch out the verification request nad start SAS, return the
        // flow id
        return ""
    }

    override fun readyPendingVerificationInDMs(
            methods: List<VerificationMethod>,
            otherUserId: String,
            roomId: String,
            transactionId: String
    ): Boolean {
        Timber.e("## TRYING TO READY PENDING ROOM VERIFICATION")
        // TODO do the same as readyPendingVerification
        return true
    }

    override fun readyPendingVerification(
            methods: List<VerificationMethod>,
            otherUserId: String,
            transactionId: String
    ): Boolean {
        val request = this.olmMachine.getVerificationRequest(otherUserId, transactionId)

        return if (request != null) {
            val outgoingRequest = request.acceptWithMethods(methods)

            if (outgoingRequest != null) {
                runBlocking { sendRequest(outgoingRequest) }
                dispatchRequestUpdated(request.toPendingVerificationRequest())
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    // TODO create a class that handles this, the DefaultCryptoService has
    // similar needs so we could share code there, beware that local echo seems
    // to be handled here
    suspend fun sendRequest(request: OutgoingVerificationRequest) {
        when (request) {
            is OutgoingVerificationRequest.ToDevice -> {
                this.requestSender.sendToDevice(request.eventType, request.body)
            }
            else -> {}
        }

        // TODO move this into the VerificationRequest and Verification classes?
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        dispatchTxUpdated(tx)
    }
}
