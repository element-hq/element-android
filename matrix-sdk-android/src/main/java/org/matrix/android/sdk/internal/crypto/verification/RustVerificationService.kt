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
import org.matrix.android.sdk.internal.crypto.QrCodeVerification
import org.matrix.android.sdk.internal.crypto.RequestSender
import org.matrix.android.sdk.internal.crypto.SasVerification
import org.matrix.android.sdk.internal.crypto.VerificationRequest
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationCancel
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationDone
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationKey
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationRequest
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import uniffi.olm.Verification

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

    override fun markedLocallyAsManuallyVerified(userId: String, deviceID: String) {
        runBlocking { olmMachine.markDeviceAsTrusted(userId, deviceID) }
    }

    fun onEvent(event: Event) = when (event.getClearType()) {
        // TODO most of those methods do the same, we just need to get the
        //  flow id and the sender from the event, can we add a generic method for this?
        EventType.KEY_VERIFICATION_START -> onStart(event)
        EventType.KEY_VERIFICATION_CANCEL -> onCancel(event)
        EventType.KEY_VERIFICATION_ACCEPT -> {}
        EventType.KEY_VERIFICATION_KEY -> onKey(event)
        EventType.KEY_VERIFICATION_MAC -> {}
        EventType.KEY_VERIFICATION_READY -> {}
        EventType.KEY_VERIFICATION_DONE -> onDone(event)
        MessageType.MSGTYPE_VERIFICATION_REQUEST -> onRequest(event)
        else -> {}
    }

    private fun getAndDispatch(sender: String, flowId: String) {
        val verification = this.getExistingTransaction(sender, flowId) ?: return
        dispatchTxUpdated(verification)
    }

    private fun onCancel(event: Event) {
        val content = event.getClearContent().toModel<KeyVerificationCancel>() ?: return
        val flowId = content.transactionId ?: return
        val sender = event.senderId ?: return

        getAndDispatch(sender, flowId)
    }
    private fun onStart(event: Event) {
        val content = event.getClearContent().toModel<KeyVerificationStart>() ?: return
        val flowId = content.transactionId ?: return
        val sender = event.senderId ?: return

        getAndDispatch(sender, flowId)
    }

    private fun onDone(event: Event) {
        val content = event.getClearContent().toModel<KeyVerificationDone>() ?: return
        val flowId = content.transactionId ?: return
        val sender = event.senderId ?: return

        getAndDispatch(sender, flowId)
    }

    private fun onKey(event: Event) {
        val content = event.getClearContent().toModel<KeyVerificationKey>() ?: return
        val flowId = content.transactionId ?: return
        val sender = event.senderId ?: return

        getAndDispatch(sender, flowId)
    }

    private fun onRequest(event: Event) {
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
        return when (verification) {
            is Verification.QrCodeV1 -> QrCodeVerification(this.olmMachine.inner(), verification.qrcode, this.requestSender, this.listeners)
            is Verification.SasV1    -> SasVerification(this.olmMachine.inner(), verification.sas, this.requestSender, this.listeners)
        }
    }

    override fun getExistingVerificationRequests(
            otherUserId: String
    ): List<PendingVerificationRequest> {
        return this.getVerificationRequests(otherUserId).map {
            it.toPendingVerificationRequest()
        }
    }

    override fun getExistingVerificationRequest(
            otherUserId: String,
            tid: String?
    ): PendingVerificationRequest? {
        return if (tid != null) {
            this.getVerificationRequest(otherUserId, tid)?.toPendingVerificationRequest()
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
        val flowId = transactionId ?: return null

        // should check if already one (and cancel it)
        return if (method == VerificationMethod.SAS) {
            val request = this.getVerificationRequest(otherUserId, flowId)

            runBlocking {
                val sas = request?.startSasVerification()

                if (sas != null) {
                    val sasTransaction = SasVerification(olmMachine.inner(), sas, requestSender, listeners)
                    dispatchTxAdded(sasTransaction)
                    sasTransaction.transactionId
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
        val verificationRequest = request.transactionId?.let { this.getVerificationRequest(request.otherUserId, it) }
        runBlocking { verificationRequest?.cancel() }
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

    private fun getVerificationRequest(otherUserId: String, transactionId: String): VerificationRequest? {
        val request = this.olmMachine.getVerificationRequest(otherUserId, transactionId)

        return if (request != null) {
            VerificationRequest(
                    this.olmMachine.inner(),
                    request,
                    requestSender,
                    listeners,
            )
        } else {
            null
        }
    }

    private fun getVerificationRequests(userId: String): List<VerificationRequest> {
        return this.olmMachine.getVerificationRequests(userId).map {
            VerificationRequest(
                    this.olmMachine.inner(),
                    it,
                    this.requestSender,
                    this.listeners,
            )
        }
    }

    override fun readyPendingVerification(
            methods: List<VerificationMethod>,
            otherUserId: String,
            transactionId: String
    ): Boolean {
        val request = this.getVerificationRequest(otherUserId, transactionId)

        return if (request != null) {
            runBlocking { request.acceptWithMethods(methods) }

            if (request.isReady()) {
                val qrcode = request.startQrVerification()

                if (qrcode != null) {
                    dispatchTxAdded(qrcode)
                }

                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        dispatchTxUpdated(tx)
    }
}
