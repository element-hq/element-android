/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import androidx.annotation.VisibleForTesting
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.QRCodeVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationCancelContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationDoneContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationStartContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.SecretShareManager
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationCancel
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationDone
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationRequest
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import org.matrix.android.sdk.internal.crypto.model.rest.toValue
import org.matrix.android.sdk.internal.crypto.verification.qrcode.QrCodeData
import org.matrix.android.sdk.internal.crypto.verification.qrcode.generateSharedSecretV2
import org.matrix.android.sdk.internal.crypto.verification.qrcode.toQrCodeData
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import java.util.Locale

private val loggerTag = LoggerTag("Verification", LoggerTag.CRYPTO)

internal class VerificationActor @AssistedInject constructor(
        @Assisted private val scope: CoroutineScope,
        private val clock: Clock,
        @UserId private val myUserId: String,
        private val secretShareManager: SecretShareManager,
        private val transportLayer: VerificationTransportLayer,
        private val verificationRequestsStore: VerificationRequestsStore,
        private val olmPrimitiveProvider: VerificationCryptoPrimitiveProvider,
        private val verificationTrustBackend: VerificationTrustBackend,
) {

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope): VerificationActor
    }

    @VisibleForTesting
    val channel = Channel<VerificationIntent>(
            capacity = Channel.UNLIMITED,
    )

    init {
        scope.launch {
            for (msg in channel) {
                onReceive(msg)
            }
        }
    }

    // Replaces the typical list of listeners pattern.
    // Looks to me as the sane setup, not sure if more than 1 is needed as extraBufferCapacity
    val eventFlow = MutableSharedFlow<VerificationEvent>(extraBufferCapacity = 20, onBufferOverflow = BufferOverflow.SUSPEND)

    suspend fun send(intent: VerificationIntent) {
        channel.send(intent)
    }

    private suspend fun withMatchingRequest(
            otherUserId: String,
            requestId: String,
            block: suspend ((KotlinVerificationRequest) -> Unit)
    ) {
        val matchingRequest = verificationRequestsStore.getExistingRequest(otherUserId, requestId)
                ?: return Unit.also {
                    // Receive a transaction event with no matching request.. should ignore.
                    // Not supported any more to do raw start
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}] request $requestId not found!")
                }

        if (matchingRequest.state == EVerificationState.HandledByOtherSession) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] ignore transaction event for $requestId handled by other")
            return
        }

        if (matchingRequest.isFinished()) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] ignore transaction event for $requestId for finished request")
            return
        }
        block.invoke(matchingRequest)
    }

    private suspend fun withMatchingRequest(
            otherUserId: String,
            requestId: String,
            viaRoom: String?,
            block: suspend ((KotlinVerificationRequest) -> Unit)
    ) {
        val matchingRequest = verificationRequestsStore.getExistingRequest(otherUserId, requestId)
                ?: return Unit.also {
                    // Receive a transaction event with no matching request.. should ignore.
                    // Not supported any more to do raw start
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}] request $requestId not found!")
                }

        if (matchingRequest.state == EVerificationState.HandledByOtherSession) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] ignore transaction event for $requestId handled by other")
            return
        }

        if (matchingRequest.isFinished()) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] ignore transaction event for $requestId for finished request")
            return
        }

        if (viaRoom == null && matchingRequest.roomId != null) {
            // mismatch transport
            return Unit.also {
                Timber.v("Mismatch transport: received to device for in room verification id:${requestId}")
            }
        } else if (viaRoom != null && matchingRequest.roomId != viaRoom) {
            // mismatch transport or room
            return Unit.also {
                Timber.v("Mismatch transport: received in room ${viaRoom} for verification id:${requestId} in room ${matchingRequest.roomId}")
            }
        }

        block(matchingRequest)
    }

    suspend fun onReceive(msg: VerificationIntent) {
        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}]: $msg")
        when (msg) {
            is VerificationIntent.ActionRequestVerification -> {
                handleActionRequestVerification(msg)
            }
            is VerificationIntent.OnReadyReceived -> {
                handleReadyReceived(msg)
            }
//            is VerificationIntent.UpdateRequest -> {
//                updatePendingRequest(msg.request)
//            }
            is VerificationIntent.GetExistingRequestInRoom -> {
                val existing = verificationRequestsStore.getExistingRequestInRoom(msg.transactionId, msg.roomId)
                msg.deferred.complete(existing?.toPendingVerificationRequest())
            }
            is VerificationIntent.OnVerificationRequestReceived -> {
                handleIncomingRequest(msg)
            }
            is VerificationIntent.ActionReadyRequest -> {
                handleActionReadyRequest(msg)
            }
            is VerificationIntent.ActionStartSasVerification -> {
                handleSasStart(msg)
            }
            is VerificationIntent.ActionReciprocateQrVerification -> {
                handleActionReciprocateQR(msg)
            }
            is VerificationIntent.ActionConfirmCodeWasScanned -> {
                withMatchingRequest(msg.otherUserId, msg.requestId) {
                    handleActionQRScanConfirmed(it)
                }
                msg.deferred.complete(Unit)
            }
            is VerificationIntent.OnStartReceived -> {
                onStartReceived(msg)
            }
            is VerificationIntent.OnAcceptReceived -> {
                withMatchingRequest(msg.fromUser, msg.validAccept.transactionId, msg.viaRoom) {
                    handleReceiveAccept(it, msg)
                }
            }
            is VerificationIntent.OnKeyReceived -> {
                withMatchingRequest(msg.fromUser, msg.validKey.transactionId, msg.viaRoom) {
                    handleReceiveKey(it, msg)
                }
            }
            is VerificationIntent.ActionSASCodeDoesNotMatch -> {
                handleSasCodeDoesNotMatch(msg)
            }
            is VerificationIntent.ActionSASCodeMatches -> {
                handleSasCodeMatch(msg)
            }
            is VerificationIntent.OnMacReceived -> {
                withMatchingRequest(msg.fromUser, msg.validMac.transactionId, msg.viaRoom) {
                    handleMacReceived(it, msg)
                }
            }
            is VerificationIntent.OnDoneReceived -> {
                withMatchingRequest(msg.fromUser, msg.transactionId, msg.viaRoom) {
                    handleDoneReceived(it, msg)
                }
            }
            is VerificationIntent.ActionCancel -> {
                verificationRequestsStore.getExistingRequestWithRequestId(msg.transactionId)
                        ?.let { matchingRequest ->
                            try {
                                cancelRequest(matchingRequest, CancelCode.User)
                                msg.deferred.complete(Unit)
                            } catch (failure: Throwable) {
                                msg.deferred.completeExceptionally(failure)
                            }
                        }
            }
            is VerificationIntent.OnUnableToDecryptVerificationEvent -> {
                // at least if request was sent by me, I can safely cancel without interfering
                val matchingRequest = verificationRequestsStore.getExistingRequest(msg.fromUser, msg.transactionId)
                        ?: return
                if (matchingRequest.state != EVerificationState.HandledByOtherSession) {
                    cancelRequest(matchingRequest, CancelCode.InvalidMessage)
                }
            }
            is VerificationIntent.GetExistingRequestsForUser -> {
                verificationRequestsStore.getExistingRequestsForUser(msg.userId).let { requests ->
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]: Found $requests")
                    msg.deferred.complete(requests.map { it.toPendingVerificationRequest() })
                }
            }
            is VerificationIntent.GetExistingTransaction -> {
                verificationRequestsStore
                        .getExistingTransaction(msg.fromUser, msg.transactionId)
                        .let {
                            msg.deferred.complete(it)
                        }
            }
            is VerificationIntent.GetExistingRequest -> {
                verificationRequestsStore
                        .getExistingRequest(msg.otherUserId, msg.transactionId)
                        .let {
                            msg.deferred.complete(it?.toPendingVerificationRequest())
                        }
            }
            is VerificationIntent.OnCancelReceived -> {
                withMatchingRequest(msg.fromUser, msg.validCancel.transactionId, msg.viaRoom) { request ->
                    // update as canceled
                    request.state = EVerificationState.Cancelled
                    val cancelCode = safeValueOf(msg.validCancel.code)
                    request.cancelCode = cancelCode
                    // TODO or QR
                    val existingTx: KotlinSasTransaction? =
                            getExistingTransaction(msg.validCancel.transactionId) // txMap[msg.fromUser]?.get(msg.validCancel.transactionId)
                    if (existingTx != null) {
                        existingTx.state = SasTransactionState.Cancelled(cancelCode, false)
                        verificationRequestsStore.deleteTransaction(msg.fromUser, msg.validCancel.transactionId)
                        dispatchUpdate(VerificationEvent.TransactionUpdated(existingTx))
                    }
                    dispatchUpdate(VerificationEvent.RequestUpdated(request.toPendingVerificationRequest()))
                }
            }
            is VerificationIntent.OnReadyByAnotherOfMySessionReceived -> {
                handleReadyByAnotherOfMySessionReceived(msg)
            }
        }
    }

    private fun dispatchUpdate(update: VerificationEvent) {
        // We don't want to block on emit.
        // If no subscriber there is a small buffer
        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] Dispatch Request update ${update.transactionId}")
        scope.launch {
            eventFlow.emit(update)
        }
    }

    private suspend fun handleIncomingRequest(msg: VerificationIntent.OnVerificationRequestReceived) {
        val pendingVerificationRequest = KotlinVerificationRequest(
                requestId = msg.validRequestInfo.transactionId,
                incoming = true,
                otherUserId = msg.senderId,
                state = EVerificationState.Requested,
                ageLocalTs = msg.timeStamp ?: clock.epochMillis()
        ).apply {
            requestInfo = msg.validRequestInfo
            roomId = msg.roomId
        }
        verificationRequestsStore.addRequest(msg.senderId, pendingVerificationRequest)
        dispatchRequestAdded(pendingVerificationRequest)
    }

    private suspend fun onStartReceived(msg: VerificationIntent.OnStartReceived) {
        val requestId = msg.validVerificationInfoStart.transactionId
        val matchingRequest = verificationRequestsStore
                .getExistingRequestWithRequestId(msg.validVerificationInfoStart.transactionId)
                ?: return Unit.also {
                    // Receive a start with no matching request.. should ignore.
                    // Not supported any more to do raw start
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}] Start for request $requestId not found!")
                }

        if (matchingRequest.state == EVerificationState.HandledByOtherSession) {
            // ignore
            return
        }
        if (matchingRequest.state != EVerificationState.Ready) {
            cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
            return
        }

        if (msg.viaRoom == null && matchingRequest.roomId != null) {
            // mismatch transport
            return Unit.also {
                Timber.v("onStartReceived in to device for in room verification id:${requestId}")
            }
        } else if (msg.viaRoom != null && matchingRequest.roomId != msg.viaRoom) {
            // mismatch transport or room
            return Unit.also {
                Timber.v("onStartReceived in room ${msg.viaRoom} for verification id:${requestId} in room ${matchingRequest.roomId}")
            }
        }

        when (msg.validVerificationInfoStart) {
            is ValidVerificationInfoStart.ReciprocateVerificationInfoStart -> {
                handleReceiveStartForQR(matchingRequest, msg.validVerificationInfoStart)
            }
            is ValidVerificationInfoStart.SasVerificationInfoStart -> {
                handleReceiveStartForSas(
                        msg,
                        matchingRequest,
                        msg.validVerificationInfoStart
                )
            }
        }
        matchingRequest.state = EVerificationState.Started
        dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
    }

    private suspend fun handleReceiveStartForQR(request: KotlinVerificationRequest, reciprocate: ValidVerificationInfoStart.ReciprocateVerificationInfoStart) {
        // Ok so the other did scan our code
        val ourSecret = request.qrCodeData?.sharedSecret
        if (ourSecret != reciprocate.sharedSecret) {
            // something went wrong
            cancelRequest(request, CancelCode.MismatchedKeys)
            return
        }

        // The secret matches, we need manual action to confirm that it was scan
        val tx = KotlinQRVerification(
                channel = this.channel,
                state = QRCodeVerificationState.WaitingForScanConfirmation,
                qrCodeData = request.qrCodeData,
                method = VerificationMethod.QR_CODE_SCAN,
                transactionId = request.requestId,
                otherUserId = request.otherUserId,
                otherDeviceId = request.otherDeviceId(),
                isIncoming = false,
                isToDevice = request.roomId == null
        )
        addTransaction(tx)
    }

    private suspend fun handleReceiveStartForSas(
            msg: VerificationIntent.OnStartReceived,
            request: KotlinVerificationRequest,
            sasStart: ValidVerificationInfoStart.SasVerificationInfoStart
    ) {
        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] Incoming SAS start for request ${request.requestId}")
        // start is a bit special as it could be started from both side
        // the event sent by the user whose user ID is the smallest is used,
        // and the other m.key.verification.start event is ignored.
        // So let's check if I already send a start?
        val requestId = msg.validVerificationInfoStart.transactionId
        val existing: KotlinSasTransaction? = getExistingTransaction(msg.fromUser, requestId)
        if (existing != null) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] No existing Sas transaction for ${request.requestId}")
            tryOrNull { cancelRequest(request, CancelCode.UnexpectedMessage) }
            return
        }

        // we accept with the agreement methods
        // Select a key agreement protocol, a hash algorithm, a message authentication code,
        // and short authentication string methods out of the lists given in requester's message.
        // TODO create proper exceptions and catch in caller
        val agreedProtocol = sasStart.keyAgreementProtocols.firstOrNull { SasVerificationTransaction.KNOWN_AGREEMENT_PROTOCOLS.contains(it) }
                ?: return Unit.also {
                    Timber.e("## protocol agreement error for request ${request.requestId}")
                    cancelRequest(request, CancelCode.UnknownMethod)
                }
        val agreedHash = sasStart.hashes.firstOrNull { SasVerificationTransaction.KNOWN_HASHES.contains(it) }
                ?: return Unit.also {
                    Timber.e("## hash agreement error for request ${request.requestId}")
                    cancelRequest(request, CancelCode.UserError)
                }
        val agreedMac = sasStart.messageAuthenticationCodes.firstOrNull { SasVerificationTransaction.KNOWN_MACS.contains(it) }
                ?: return Unit.also {
                    Timber.e("## sas agreement error for request ${request.requestId}")
                    cancelRequest(request, CancelCode.UserError)
                }
        val agreedShortCode = sasStart.shortAuthenticationStrings
                .filter { SasVerificationTransaction.KNOWN_SHORT_CODES.contains(it) }
                .takeIf { it.isNotEmpty() }
                ?: return Unit.also {
                    Timber.e("## SAS agreement error for request ${request.requestId}")
                    cancelRequest(request, CancelCode.UserError)
                }

        val otherDeviceId = request.otherDeviceId()
                ?: return Unit.also {
                    Timber.e("## SAS Unexpected method")
                    cancelRequest(request, CancelCode.UnknownMethod)
                }
        // Bob’s device ensures that it has a copy of Alice’s device key.
        val mxDeviceInfo = verificationTrustBackend.getUserDevice(request.otherUserId, otherDeviceId)

        if (mxDeviceInfo?.fingerprint() == null) {
            Timber.e("## SAS Failed to find device key ")
            // TODO force download keys!!
            // would be probably better to download the keys
            // for now I cancel
            cancelRequest(request, CancelCode.UserError)
            return
        }
        val sasTx = KotlinSasTransaction(
                channel = channel,
                transactionId = requestId,
                state = SasTransactionState.None,
                otherUserId = request.otherUserId,
                myUserId = myUserId,
                myTrustedMSK = verificationTrustBackend.getMyTrustedMasterKeyBase64(),
                otherDeviceId = request.otherDeviceId(),
                myDeviceId = verificationTrustBackend.getMyDeviceId(),
                myDeviceFingerprint = verificationTrustBackend.getMyDevice().fingerprint().orEmpty(),
                startReq = sasStart,
                isIncoming = true,
                isToDevice = msg.viaRoom == null,
                olmSAS = olmPrimitiveProvider.provideOlmSas()
        )

        val concat = sasTx.olmSAS.publicKey + sasStart.canonicalJson
        val commitment = hashUsingAgreedHashMethod(agreedHash, concat)

        val accept = KotlinSasTransaction.sasAccept(
                inRoom = request.roomId != null,
                requestId = requestId,
                keyAgreementProtocol = agreedProtocol,
                hash = agreedHash,
                messageAuthenticationCode = agreedMac,
                shortAuthenticationStrings = agreedShortCode,
                commitment = commitment
        )

        // cancel if network error (would not send back a cancel but at least current user will see feedback?)
        try {
            transportLayer.sendToOther(request, EventType.KEY_VERIFICATION_ACCEPT, accept)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] Failed to send accept for ${request.requestId}")
            tryOrNull { cancelRequest(request, CancelCode.User) }
        }

        sasTx.accepted = accept.asValidObject()
        sasTx.state = SasTransactionState.SasAccepted

        addTransaction(sasTx)
    }

    private suspend fun handleReceiveAccept(matchingRequest: KotlinVerificationRequest, msg: VerificationIntent.OnAcceptReceived) {
        val requestId = msg.validAccept.transactionId

        val existing: KotlinSasTransaction = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.v("on accept received in room ${msg.viaRoom} for verification id:${requestId} in room ${matchingRequest.roomId}")
                }

        // Existing should be in
        if (existing.state != SasTransactionState.SasStarted) {
            // it's a wrong state should cancel?
            // TODO cancel
        }

        val accept = msg.validAccept
        // Check that the agreement is correct
        if (!SasVerificationTransaction.KNOWN_AGREEMENT_PROTOCOLS.contains(accept.keyAgreementProtocol) ||
                !SasVerificationTransaction.KNOWN_HASHES.contains(accept.hash) ||
                !SasVerificationTransaction.KNOWN_MACS.contains(accept.messageAuthenticationCode) ||
                accept.shortAuthenticationStrings.intersect(SasVerificationTransaction.KNOWN_SHORT_CODES).isEmpty()) {
            Timber.e("## SAS agreement error for request ${matchingRequest.requestId}")
            cancelRequest(matchingRequest, CancelCode.UnknownMethod)
            return
        }

        // Upon receipt of the m.key.verification.accept message from Bob’s device,
        // Alice’s device stores the commitment value for later use.

        //  Alice’s device creates an ephemeral Curve25519 key pair (dA,QA),
        // and replies with a to_device message with type set to “m.key.verification.key”, sending Alice’s public key QA
        val pubKey = existing.olmSAS.publicKey

        val keyMessage = KotlinSasTransaction.sasKeyMessage(matchingRequest.roomId != null, requestId, pubKey)

        try {
            if (BuildConfig.LOG_PRIVATE_DATA) {
                Timber.tag(loggerTag.value)
                        .v("[${myUserId.take(8)}]: Sending my key $pubKey")
            }
            transportLayer.sendToOther(
                    matchingRequest,
                    EventType.KEY_VERIFICATION_KEY,
                    keyMessage,
            )
        } catch (failure: Throwable) {
            existing.state = SasTransactionState.Cancelled(CancelCode.UserError, true)
            matchingRequest.cancelCode = CancelCode.UserError
            matchingRequest.state = EVerificationState.Cancelled
            dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
            dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
            return
        }
        existing.accepted = accept
        existing.state = SasTransactionState.SasKeySent
        dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
    }

    private suspend fun handleSasStart(msg: VerificationIntent.ActionStartSasVerification) {
        val matchingRequest = verificationRequestsStore.getExistingRequestWithRequestId(msg.requestId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]: Can't start unknown request ${msg.requestId}")
                    msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Unknown request"))
                }

        if (matchingRequest.state != EVerificationState.Ready) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}]: Can't start a non ready request ${msg.requestId}")
            msg.deferred.completeExceptionally(java.lang.IllegalStateException("Can't start a non ready request"))
            return
        }

        val otherDeviceId = matchingRequest.otherDeviceId() ?: return Unit.also {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}]: Can't start null other device id ${msg.requestId}")
            msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Failed to find other device Id"))
        }

        val existingTransaction = getExistingTransaction<VerificationTransaction>(msg.otherUserId, msg.requestId)
        if (existingTransaction is SasVerificationTransaction) {
            // there is already an existing transaction??
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}]: Can't start, already started ${msg.requestId}")
            msg.deferred.completeExceptionally(IllegalStateException("Already started"))
            return
        }
        val startMessage = KotlinSasTransaction.sasStart(
                inRoom = matchingRequest.roomId != null,
                fromDevice = verificationTrustBackend.getMyDeviceId(),
                requestId = msg.requestId
        )

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}]:sending start to other ${msg.requestId} in room ${matchingRequest.roomId}")
        transportLayer.sendToOther(
                matchingRequest,
                EventType.KEY_VERIFICATION_START,
                startMessage,
        )

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}]: start sent to other ${msg.requestId}")

        // should check if already one (and cancel it)
        val tx = KotlinSasTransaction(
                channel = channel,
                transactionId = msg.requestId,
                state = SasTransactionState.SasStarted,
                otherUserId = msg.otherUserId,
                myUserId = myUserId,
                myTrustedMSK = verificationTrustBackend.getMyTrustedMasterKeyBase64(),
                otherDeviceId = otherDeviceId,
                myDeviceId = verificationTrustBackend.getMyDeviceId(),
                myDeviceFingerprint = verificationTrustBackend.getMyDevice().fingerprint().orEmpty(),
                startReq = startMessage.asValidObject() as ValidVerificationInfoStart.SasVerificationInfoStart,
                isIncoming = false,
                isToDevice = matchingRequest.roomId == null,
                olmSAS = olmPrimitiveProvider.provideOlmSas()
        )

        matchingRequest.state = EVerificationState.WeStarted
        dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
        addTransaction(tx)

        msg.deferred.complete(tx)
    }

    private suspend fun handleActionReciprocateQR(msg: VerificationIntent.ActionReciprocateQrVerification) {
        Timber.tag(loggerTag.value)
                .d("[${myUserId.take(8)}] handle reciprocate for ${msg.requestId}")
        val matchingRequest = verificationRequestsStore.getExistingRequestWithRequestId(msg.requestId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] No matching request, abort ${msg.requestId}")
                    msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Unknown request"))
                }

        if (matchingRequest.state != EVerificationState.Ready) {
            Timber.tag(loggerTag.value)
                    .d("[${myUserId.take(8)}] Can't start if not ready, abort ${msg.requestId}")
            msg.deferred.completeExceptionally(java.lang.IllegalStateException("Can't start a non ready request"))
            return
        }

        val otherDeviceId = matchingRequest.otherDeviceId() ?: return Unit.also {
            msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Failed to find other device Id"))
        }

        val existingTransaction = getExistingTransaction<VerificationTransaction>(msg.otherUserId, msg.requestId)
        // what if there is an existing??
        if (existingTransaction != null) {
            // cancel or replace??
            Timber.tag(loggerTag.value)
                    .w("[${myUserId.take(8)}] There is already a started transaction for request  ${msg.requestId}")
            return
        }

        val myMasterKey = verificationTrustBackend.getUserMasterKeyBase64(myUserId)

        // Check the other device view of my MSK
        val otherQrCodeData = msg.scannedData.toQrCodeData()
        when (otherQrCodeData) {
            null -> {
                Timber.tag(loggerTag.value)
                        .d("[${myUserId.take(8)}] Malformed QR code  ${msg.requestId}")
                msg.deferred.completeExceptionally(IllegalArgumentException("Malformed QrCode data"))
                return
            }
            is QrCodeData.VerifyingAnotherUser -> {
                // key2 (aka otherUserMasterCrossSigningPublicKey) is what the one displaying the QR code (other user) think my MSK is.
                // Let's check that it's correct
                // If not -> Cancel
                val whatOtherThinksMyMskIs = otherQrCodeData.otherUserMasterCrossSigningPublicKey
                if (whatOtherThinksMyMskIs != myMasterKey) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] ## Verification QR: Invalid other master key ${otherQrCodeData.otherUserMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                }

                val whatIThinkOtherMskIs = verificationTrustBackend.getUserMasterKeyBase64(matchingRequest.otherUserId)
                if (whatIThinkOtherMskIs != otherQrCodeData.userMasterCrossSigningPublicKey) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] ## Verification QR: Invalid other master key ${otherQrCodeData.otherUserMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted -> {
                if (matchingRequest.otherUserId != myUserId) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] Self mode qr with wrong user ${matchingRequest.otherUserId}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedUser)
                    msg.deferred.complete(null)
                    return
                }
                // key1 (aka userMasterCrossSigningPublicKey) is the session displaying the QR code view of our MSK.
                // Let's check that I see the same MSK
                // If not -> Cancel
                val whatOtherThinksOurMskIs = otherQrCodeData.userMasterCrossSigningPublicKey
                if (whatOtherThinksOurMskIs != myMasterKey) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] ## Verification QR: Invalid other master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                }
                val whatOtherThinkMyDeviceKeyIs = otherQrCodeData.otherDeviceKey
                val myDeviceKey = verificationTrustBackend.getMyDevice().fingerprint()
                if (whatOtherThinkMyDeviceKeyIs != myDeviceKey) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] ## Verification QR: Invalid other device key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                if (matchingRequest.otherUserId != myUserId) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] Self mode qr with wrong user ${matchingRequest.otherUserId}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedUser)
                    msg.deferred.complete(null)
                    return
                }
                // key2 (aka userMasterCrossSigningPublicKey) is the session displaying the QR code view of our MSK.
                // Let's check that it's the good one
                // If not -> Cancel
                val otherDeclaredDeviceKey = otherQrCodeData.deviceKey
                val whatIThinkItIs = verificationTrustBackend.getUserDevice(matchingRequest.otherUserId, otherDeviceId)?.fingerprint()

                if (otherDeclaredDeviceKey != whatIThinkItIs) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] ## Verification QR: Invalid other device key $otherDeviceId")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                }

                val ownMasterKeyTrustedAsSeenByOther = otherQrCodeData.userMasterCrossSigningPublicKey
                if (ownMasterKeyTrustedAsSeenByOther != myMasterKey) {
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}] ## Verification QR: Invalid other master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                }
            }
        }

        // All checks are correct
        // Send the shared secret so that sender can trust me
        // qrCodeData.sharedSecret will be used to send the start request
        val message = if (matchingRequest.roomId != null) {
            MessageVerificationStartContent(
                    fromDevice = verificationTrustBackend.getMyDeviceId(),
                    hashes = null,
                    keyAgreementProtocols = null,
                    messageAuthenticationCodes = null,
                    shortAuthenticationStrings = null,
                    method = VERIFICATION_METHOD_RECIPROCATE,
                    relatesTo = RelationDefaultContent(
                            type = RelationType.REFERENCE,
                            eventId = msg.requestId
                    ),
                    sharedSecret = otherQrCodeData.sharedSecret
            )
        } else {
            KeyVerificationStart(
                    fromDevice = verificationTrustBackend.getMyDeviceId(),
                    sharedSecret = otherQrCodeData.sharedSecret,
                    method = VERIFICATION_METHOD_RECIPROCATE,
            )
        }

        try {
            transportLayer.sendToOther(matchingRequest, EventType.KEY_VERIFICATION_START, message)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .d("[${myUserId.take(8)}] Failed to send reciprocate message")
            msg.deferred.completeExceptionally(failure)
            return
        }

        matchingRequest.state = EVerificationState.WeStarted
        dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))

        val tx = KotlinQRVerification(
                channel = this.channel,
                state = QRCodeVerificationState.Reciprocated,
                qrCodeData = msg.scannedData.toQrCodeData(),
                method = VerificationMethod.QR_CODE_SCAN,
                transactionId = msg.requestId,
                otherUserId = msg.otherUserId,
                otherDeviceId = matchingRequest.otherDeviceId(),
                isIncoming = false,
                isToDevice = matchingRequest.roomId == null
        )

        addTransaction(tx)
        msg.deferred.complete(tx)
    }

    private suspend fun handleActionQRScanConfirmed(matchingRequest: KotlinVerificationRequest) {
        val transaction = getExistingTransaction<KotlinQRVerification>(matchingRequest.otherUserId, matchingRequest.requestId)
        if (transaction == null) {
            // return
            Timber.tag(loggerTag.value)
                    .d("[${myUserId.take(8)}]: No matching transaction for key tId:${matchingRequest.requestId}")
            return
        }

        if (transaction.state() == QRCodeVerificationState.WaitingForScanConfirmation) {
            completeValidQRTransaction(transaction, matchingRequest)
        } else {
            Timber.tag(loggerTag.value)
                    .d("[${myUserId.take(8)}]: Unexpected confirm in state tId:${matchingRequest.requestId}")
            // TODO throw?
            cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
            return
        }
    }

    private suspend fun handleReceiveKey(matchingRequest: KotlinVerificationRequest, msg: VerificationIntent.OnKeyReceived) {
        val requestId = msg.validKey.transactionId

        val existing: KotlinSasTransaction = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]: No matching transaction for key tId:$requestId")
                }

        // Existing should be in SAS key sent
        val isCorrectState = if (existing.isIncoming) {
            existing.state == SasTransactionState.SasAccepted
        } else {
            existing.state == SasTransactionState.SasKeySent
        }

        if (!isCorrectState) {
            // it's a wrong state should cancel?
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}]: Unexpected key in state ${existing.state} for tId:$requestId")
            cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
        }

        val otherKey = msg.validKey.key
        if (existing.isIncoming) {
            // ok i can now send my key and compute the sas code
            val pubKey = existing.olmSAS.publicKey
            val keyMessage = KotlinSasTransaction.sasKeyMessage(matchingRequest.roomId != null, requestId, pubKey)
            try {
                transportLayer.sendToOther(
                        matchingRequest,
                        EventType.KEY_VERIFICATION_KEY,
                        keyMessage,
                )
                if (BuildConfig.LOG_PRIVATE_DATA) {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:i calculate SAS my key $pubKey their Key: $otherKey")
                }
                existing.calculateSASBytes(otherKey)
                existing.state = SasTransactionState.SasShortCodeReady
                if (BuildConfig.LOG_PRIVATE_DATA) {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:i CODE ${existing.getDecimalCodeRepresentation()}")
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:i EMOJI CODE ${existing.getEmojiCodeRepresentation().joinToString(" ") { it.emoji }}")
                }
                dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
            } catch (failure: Throwable) {
                existing.state = SasTransactionState.Cancelled(CancelCode.UserError, true)
                matchingRequest.state = EVerificationState.Cancelled
                matchingRequest.cancelCode = CancelCode.UserError
                dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
                dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
                return
            }
        } else {
            // Upon receipt of the m.key.verification.key message from Bob’s device,
            // Alice’s device checks that the commitment property from the Bob’s m.key.verification.accept
            // message is the same as the expected value based on the value of the key property received
            // in Bob’s m.key.verification.key and the content of Alice’s m.key.verification.start message.

            // check commitment
            val concat = otherKey + existing.startReq!!.canonicalJson

            val otherCommitment = try {
                hashUsingAgreedHashMethod(existing.accepted?.hash, concat)
            } catch (failure: Throwable) {
                Timber.tag(loggerTag.value)
                        .v(failure, "[${myUserId.take(8)}]: Failed to  compute hash for tId:$requestId")
                cancelRequest(matchingRequest, CancelCode.InvalidMessage)
            }

            if (otherCommitment == existing.accepted?.commitment) {
                if (BuildConfig.LOG_PRIVATE_DATA) {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:o calculate SAS my key ${existing.olmSAS.publicKey} their Key: $otherKey")
                }
                existing.calculateSASBytes(otherKey)
                existing.state = SasTransactionState.SasShortCodeReady
                dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
                if (BuildConfig.LOG_PRIVATE_DATA) {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:o CODE ${existing.getDecimalCodeRepresentation()}")
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:o EMOJI CODE ${existing.getEmojiCodeRepresentation().joinToString(" ") { it.emoji }}")
                }
            } else {
                // bad commitment
                Timber.tag(loggerTag.value)
                        .v("[${myUserId.take(8)}]: Bad Commitment for tId:$requestId actual:$otherCommitment ")
                cancelRequest(matchingRequest, CancelCode.MismatchedCommitment)
                return
            }
        }
    }

    private suspend fun handleMacReceived(matchingRequest: KotlinVerificationRequest, msg: VerificationIntent.OnMacReceived) {
        val requestId = msg.validMac.transactionId

        val existing: KotlinSasTransaction = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}] on Mac for unknown transaction with id:$requestId")
                }

        when (existing.state) {
            is SasTransactionState.SasMacSent -> {
                existing.theirMac = msg.validMac
                finalizeSasTransaction(existing, msg.validMac, matchingRequest, existing.transactionId)
            }
            is SasTransactionState.SasShortCodeReady -> {
                // I can start verify, store it
                existing.theirMac = msg.validMac
                existing.state = SasTransactionState.SasMacReceived(false)
                dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
            }
            else -> {
                // it's a wrong state should cancel?
                Timber.tag(loggerTag.value)
                        .v("[${myUserId.take(8)}] on Mac in unexpected state ${existing.state} id:$requestId")
                cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
            }
        }
    }

    private suspend fun handleSasCodeDoesNotMatch(msg: VerificationIntent.ActionSASCodeDoesNotMatch) {
        val transactionId = msg.transactionId
        val matchingRequest = verificationRequestsStore.getExistingRequestWithRequestId(msg.transactionId)
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Request"))
                }
        if (matchingRequest.isFinished()) {
            return Unit.also {
                msg.deferred.completeExceptionally(IllegalStateException("Request was cancelled"))
            }
        }
        val existing: KotlinSasTransaction = getExistingTransaction(transactionId)
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Transaction"))
                }

        val isCorrectState = when (val state = existing.state) {
            is SasTransactionState.SasShortCodeReady -> true
            is SasTransactionState.SasMacReceived -> !state.codeConfirmed
            else -> false
        }
        if (!isCorrectState) {
            return Unit.also {
                msg.deferred.completeExceptionally(IllegalStateException("Unexpected action, can't match in this state"))
            }
        }
        try {
            cancelRequest(matchingRequest, CancelCode.MismatchedSas)
            msg.deferred.complete(Unit)
        } catch (failure: Throwable) {
            msg.deferred.completeExceptionally(failure)
        }
    }

    private suspend fun handleDoneReceived(matchingRequest: KotlinVerificationRequest, msg: VerificationIntent.OnDoneReceived) {
        val requestId = msg.transactionId

        val existing: VerificationTransaction = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.v("on accept received in room ${msg.viaRoom} for verification id:${requestId} in room ${matchingRequest.roomId}")
                }

        when {
            existing is KotlinSasTransaction -> {
                val state = existing.state
                val isCorrectState = state is SasTransactionState.Done && !state.otherDone

                if (isCorrectState) {
                    existing.state = SasTransactionState.Done(true)
                    dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
                    // we can forget about it
                    verificationRequestsStore.deleteTransaction(matchingRequest.otherUserId, matchingRequest.requestId)
                    // XXX whatabout waiting for done?
                    matchingRequest.state = EVerificationState.Done
                    dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
                } else {
                    // TODO cancel?
                    Timber.tag(loggerTag.value)
                            .d("[${myUserId.take(8)}]: Unexpected done in state $state")

                    cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
                }
            }
            existing is KotlinQRVerification -> {
                val state = existing.state()
                when (state) {
                    QRCodeVerificationState.Reciprocated -> {
                        completeValidQRTransaction(existing, matchingRequest)
                    }
                    QRCodeVerificationState.WaitingForOtherDone -> {
                        matchingRequest.state = EVerificationState.Done
                        dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
                    }
                    else -> {
                        Timber.tag(loggerTag.value)
                                .d("[${myUserId.take(8)}]: Unexpected done in state $state")
                        cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
                    }
                }
            }
            else -> {
                // unexpected message?
                cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
            }
        }
    }

    private suspend fun completeValidQRTransaction(existing: KotlinQRVerification, matchingRequest: KotlinVerificationRequest) {
        var shouldRequestSecret = false
        // Ok so the other side is fine let's trust what we need to trust
        when (existing.qrCodeData) {
            is QrCodeData.VerifyingAnotherUser -> {
                // let's trust him
                // it's his code scanned so user is him and other me
                try {
                    verificationTrustBackend.trustUser(matchingRequest.otherUserId)
                } catch (failure: Throwable) {
                    // fail silently?
                    // at least it will be marked as trusted locally?
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                // the other device is the one that doesn't trust yet our MSK
                // As all is good I can upload a signature for my new device

                // Also notify the secret share manager for the soon to come secret share requests
                secretShareManager.onVerificationCompleteForDevice(matchingRequest.otherDeviceId()!!)
                try {
                    verificationTrustBackend.trustOwnDevice(matchingRequest.otherDeviceId()!!)
                } catch (failure: Throwable) {
                    // network problem??
                    Timber.w("## Verification: Failed to sign new device ${matchingRequest.otherDeviceId()}, ${failure.localizedMessage}")
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted -> {
                // I can trust my MSK
                verificationTrustBackend.markMyMasterKeyAsTrusted()
                shouldRequestSecret = true
            }
            null -> {
                // This shouldn't happen? cancel?
            }
        }

        transportLayer.sendToOther(
                matchingRequest,
                EventType.KEY_VERIFICATION_DONE,
                if (matchingRequest.roomId != null) {
                    MessageVerificationDoneContent(
                            relatesTo = RelationDefaultContent(
                                    RelationType.REFERENCE,
                                    matchingRequest.requestId
                            )
                    )
                } else {
                    KeyVerificationDone(matchingRequest.requestId)
                }
        )

        existing.state = QRCodeVerificationState.Done
        dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
        // we can forget about it
        verificationRequestsStore.deleteTransaction(matchingRequest.otherUserId, matchingRequest.requestId)
        matchingRequest.state = EVerificationState.WaitingForDone
        dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))

        if (shouldRequestSecret) {
            matchingRequest.otherDeviceId()?.let { otherDeviceId ->
                secretShareManager.requestSecretTo(otherDeviceId, MASTER_KEY_SSSS_NAME)
                secretShareManager.requestSecretTo(otherDeviceId, SELF_SIGNING_KEY_SSSS_NAME)
                secretShareManager.requestSecretTo(otherDeviceId, USER_SIGNING_KEY_SSSS_NAME)
                secretShareManager.requestSecretTo(otherDeviceId, KEYBACKUP_SECRET_SSSS_NAME)
            }
        }
    }

    private suspend fun handleSasCodeMatch(msg: VerificationIntent.ActionSASCodeMatches) {
        val transactionId = msg.transactionId
        val matchingRequest = verificationRequestsStore.getExistingRequestWithRequestId(msg.transactionId)
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Request"))
                }

        if (matchingRequest.state != EVerificationState.WeStarted &&
                matchingRequest.state != EVerificationState.Started) {
            return Unit.also {
                msg.deferred.completeExceptionally(IllegalStateException("Can't accept code in state: ${matchingRequest.state}"))
            }
        }

        val existing: KotlinSasTransaction = getExistingTransaction(transactionId)
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Transaction"))
                }

        val isCorrectState = when (val state = existing.state) {
            is SasTransactionState.SasShortCodeReady -> true
            is SasTransactionState.SasMacReceived -> !state.codeConfirmed
            else -> false
        }
        if (!isCorrectState) {
            return Unit.also {
                msg.deferred.completeExceptionally(IllegalStateException("Unexpected action, can't match in this state"))
            }
        }

        val macInfo = existing.computeMyMac()

        val macMsg = KotlinSasTransaction.sasMacMessage(matchingRequest.roomId != null, transactionId, macInfo)
        try {
            transportLayer.sendToOther(matchingRequest, EventType.KEY_VERIFICATION_MAC, macMsg)
        } catch (failure: Throwable) {
            // it's a network problem, we don't need to cancel, user can retry?
            msg.deferred.completeExceptionally(failure)
            return
        }

        // Do I already have their Mac?
        val theirMac = existing.theirMac
        if (theirMac != null) {
            finalizeSasTransaction(existing, theirMac, matchingRequest, transactionId)
        } else {
            existing.state = SasTransactionState.SasMacSent
            dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
        }

        msg.deferred.complete(Unit)
    }

    private suspend fun finalizeSasTransaction(
            existing: KotlinSasTransaction,
            theirMac: ValidVerificationInfoMac,
            matchingRequest: KotlinVerificationRequest,
            transactionId: String
    ) {
        val result = existing.verifyMacs(
                theirMac,
                verificationTrustBackend.getUserDeviceList(matchingRequest.otherUserId),
                verificationTrustBackend.getUserMasterKeyBase64(matchingRequest.otherUserId)
        )

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] verify macs result $result id:$transactionId")
        when (result) {
            is KotlinSasTransaction.MacVerificationResult.Success -> {
                // mark the devices as locally trusted
                result.verifiedDeviceId.forEach { deviceId ->

                    verificationTrustBackend.locallyTrustDevice(matchingRequest.otherUserId, deviceId)

                    if (matchingRequest.otherUserId == myUserId && verificationTrustBackend.canCrossSign()) {
                        // If me it's reasonable to sign and upload the device signature for the other part
                        try {
                            verificationTrustBackend.trustOwnDevice(deviceId)
                        } catch (failure: Throwable) {
                            // network problem??
                            Timber.w("## Verification: Failed to sign new device $deviceId, ${failure.localizedMessage}")
                        }
                    }
                }

                if (result.otherMskTrusted) {
                    if (matchingRequest.otherUserId == myUserId) {
                        verificationTrustBackend.markMyMasterKeyAsTrusted()
                    } else {
                        // what should we do if this fails :/
                        if (verificationTrustBackend.canCrossSign()) {
                            verificationTrustBackend.trustUser(matchingRequest.otherUserId)
                        }
                    }
                }

                // we should send done and wait for done
                transportLayer.sendToOther(
                        matchingRequest,
                        EventType.KEY_VERIFICATION_DONE,
                        if (matchingRequest.roomId != null) {
                            MessageVerificationDoneContent(
                                    relatesTo = RelationDefaultContent(
                                            RelationType.REFERENCE,
                                            transactionId
                                    )
                            )
                        } else {
                            KeyVerificationDone(transactionId)
                        }
                )

                existing.state = SasTransactionState.Done(false)
                dispatchUpdate(VerificationEvent.TransactionUpdated(existing))
                verificationRequestsStore.rememberPastSuccessfulTransaction(existing)
                verificationRequestsStore.deleteTransaction(matchingRequest.otherUserId, transactionId)
                matchingRequest.state = EVerificationState.WaitingForDone
                dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
            }
            KotlinSasTransaction.MacVerificationResult.MismatchKeys,
            KotlinSasTransaction.MacVerificationResult.MismatchMacCrossSigning,
            is KotlinSasTransaction.MacVerificationResult.MismatchMacDevice,
            KotlinSasTransaction.MacVerificationResult.NoDevicesVerified -> {
                cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
            }
        }
    }

    private suspend fun handleActionReadyRequest(msg: VerificationIntent.ActionReadyRequest) {
        val existing = verificationRequestsStore.getExistingRequestWithRequestId(msg.transactionId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value).v("Request ${msg.transactionId} not found!")
                    msg.deferred.complete(null)
                }

        if (existing.state != EVerificationState.Requested) {
            Timber.tag(loggerTag.value).v("Request ${msg.transactionId} unexpected ready action")
            msg.deferred.completeExceptionally(IllegalStateException("Can't ready request in state ${existing.state}"))
            return
        }

        val otherUserMethods = existing.requestInfo?.methods.orEmpty()
        val commonMethods = getMethodAgreement(
                otherUserMethods,
                msg.methods
        )
        if (commonMethods.isEmpty()) {
            Timber.tag(loggerTag.value).v("Request ${msg.transactionId} no common methods")
            // Upon receipt of Alice’s m.key.verification.request message, if Bob’s device does not understand any of the methods,
            // it should not cancel the request as one of his other devices may support the request.

            // Instead, Bob’s device should tell Bob that no supported method was found, and allow him to manually reject the request.
            msg.deferred.completeExceptionally(IllegalStateException("Cannot understand any of the methods"))
            return
        }

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] Request ${msg.transactionId} agreement is $commonMethods")

        val qrCodeData = if (otherUserMethods.canScanCode() && msg.methods.contains(VerificationMethod.QR_CODE_SHOW)) {
            createQrCodeData(msg.transactionId, existing.otherUserId, existing.requestInfo?.fromDevice)
        } else {
            null
        }

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] Request ${msg.transactionId} code is $qrCodeData")

        val readyInfo = ValidVerificationInfoReady(
                msg.transactionId,
                verificationTrustBackend.getMyDeviceId(),
                commonMethods
        )

        val message = KotlinSasTransaction.sasReady(
                inRoom = existing.roomId != null,
                requestId = msg.transactionId,
                methods = commonMethods,
                fromDevice = verificationTrustBackend.getMyDeviceId()
        )

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] Request ${msg.transactionId} sending ready")
        try {
            transportLayer.sendToOther(existing, EventType.KEY_VERIFICATION_READY, message)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] Request ${msg.transactionId} failed to send ready")
            msg.deferred.completeExceptionally(failure)
            return
        }

        existing.readyInfo = readyInfo
        existing.qrCodeData = qrCodeData
        existing.state = EVerificationState.Ready

        // We want to try emit, if not this will suspend until someone consume the flow
        dispatchUpdate(VerificationEvent.RequestUpdated(existing.toPendingVerificationRequest()))

        Timber.tag(loggerTag.value).v("Request ${msg.transactionId} updated $existing")
        msg.deferred.complete(existing.toPendingVerificationRequest())
    }

    private suspend fun createQrCodeData(requestId: String, otherUserId: String, otherDeviceId: String?): QrCodeData? {
        return when {
            myUserId != otherUserId ->
                createQrCodeDataForDistinctUser(requestId, otherUserId)
            verificationTrustBackend.getMyTrustedMasterKeyBase64() != null ->
                // This is a self verification and I am the old device (Osborne2)
                createQrCodeDataForVerifiedDevice(requestId, otherUserId, otherDeviceId)
            else ->
                // This is a self verification and I am the new device (Dynabook)
                createQrCodeDataForUnVerifiedDevice(requestId)
        }
    }

    private fun getMethodAgreement(
            otherUserMethods: List<String>?,
            myMethods: List<VerificationMethod>,
    ): List<String> {
        if (otherUserMethods.isNullOrEmpty()) {
            return emptyList()
        }

        val result = mutableSetOf<String>()

        if (VERIFICATION_METHOD_SAS in otherUserMethods && VerificationMethod.SAS in myMethods) {
            // Other can do SAS and so do I
            result.add(VERIFICATION_METHOD_SAS)
        }

        if (VERIFICATION_METHOD_RECIPROCATE in otherUserMethods) {
            if (VERIFICATION_METHOD_QR_CODE_SCAN in otherUserMethods && VerificationMethod.QR_CODE_SHOW in myMethods) {
                // Other can Scan and I can show QR code
                result.add(VERIFICATION_METHOD_QR_CODE_SHOW)
                result.add(VERIFICATION_METHOD_RECIPROCATE)
            }
            if (VERIFICATION_METHOD_QR_CODE_SHOW in otherUserMethods && VerificationMethod.QR_CODE_SCAN in myMethods) {
                // Other can show and I can scan QR code
                result.add(VERIFICATION_METHOD_QR_CODE_SCAN)
                result.add(VERIFICATION_METHOD_RECIPROCATE)
            }
        }

        return result.toList()
    }

    private fun List<String>.canScanCode(): Boolean {
        return contains(VERIFICATION_METHOD_QR_CODE_SCAN) && contains(VERIFICATION_METHOD_RECIPROCATE)
    }

    private fun List<String>.canShowCode(): Boolean {
        return contains(VERIFICATION_METHOD_QR_CODE_SHOW) && contains(VERIFICATION_METHOD_RECIPROCATE)
    }

    private suspend fun handleActionRequestVerification(msg: VerificationIntent.ActionRequestVerification) {
        val requestsForUser = verificationRequestsStore.getExistingRequestsForUser(msg.otherUserId)
        // there can only be one active request per user, so cancel existing ones
        requestsForUser.toList().forEach { existingRequest ->
            if (!existingRequest.isFinished()) {
                Timber.d("## SAS, cancelling pending requests to start a new one")
                cancelRequest(existingRequest, CancelCode.User)
            }
        }

        // XXX We should probably throw here if you try to verify someone else from an untrusted session
        val shouldShowQROption = if (msg.otherUserId == myUserId) {
            true
        } else {
            // It's verifying someone else, I should trust my key before doing it?
            verificationTrustBackend.getUserMasterKeyBase64(myUserId) != null
        }
        val methodValues = if (shouldShowQROption) {
            // Add reciprocate method if application declares it can scan or show QR codes
            // Not sure if it ok to do that (?)
            val reciprocateMethod = msg.methods
                    .firstOrNull { it == VerificationMethod.QR_CODE_SCAN || it == VerificationMethod.QR_CODE_SHOW }
                    ?.let { listOf(VERIFICATION_METHOD_RECIPROCATE) }.orEmpty()
            msg.methods.map { it.toValue() } + reciprocateMethod
        } else {
            // Filter out SCAN and SHOW qr code method
            msg.methods
                    .filter { it != VerificationMethod.QR_CODE_SHOW && it != VerificationMethod.QR_CODE_SCAN }
                    .map { it.toValue() }
        }
                .distinct()

        val validInfo = ValidVerificationInfoRequest(
                transactionId = "",
                fromDevice = verificationTrustBackend.getMyDeviceId(),
                methods = methodValues,
                timestamp = clock.epochMillis()
        )

        try {
            if (msg.roomId != null) {
                val info = MessageVerificationRequestContent(
                        body = "$myUserId is requesting to verify your key, but your client does not support in-chat key verification." +
                                " You will need to use legacy key verification to verify keys.",
                        fromDevice = validInfo.fromDevice,
                        toUserId = msg.otherUserId,
                        timestamp = validInfo.timestamp,
                        methods = validInfo.methods
                )
                val eventId = transportLayer.sendInRoom(
                        type = EventType.MESSAGE,
                        roomId = msg.roomId,
                        content = info.toContent()
                )
                val request = KotlinVerificationRequest(
                        requestId = eventId,
                        incoming = false,
                        otherUserId = msg.otherUserId,
                        state = EVerificationState.WaitingForReady,
                        ageLocalTs = clock.epochMillis()
                ).apply {
                    roomId = msg.roomId
                    requestInfo = validInfo.copy(transactionId = eventId)
                }
                verificationRequestsStore.addRequest(msg.otherUserId, request)
                msg.deferred.complete(request.toPendingVerificationRequest())
                dispatchUpdate(VerificationEvent.RequestAdded(request.toPendingVerificationRequest()))
            } else {
                val requestId = LocalEcho.createLocalEchoId()
                transportLayer.sendToDeviceEvent(
                        messageType = EventType.KEY_VERIFICATION_REQUEST,
                        toSendToDeviceObject = KeyVerificationRequest(
                                transactionId = requestId,
                                fromDevice = verificationTrustBackend.getMyDeviceId(),
                                methods = validInfo.methods,
                                timestamp = validInfo.timestamp
                        ),
                        otherUserId = msg.otherUserId,
                        targetDevices = msg.targetDevices.orEmpty()
                )
                val request = KotlinVerificationRequest(
                        requestId = requestId,
                        incoming = false,
                        otherUserId = msg.otherUserId,
                        state = EVerificationState.WaitingForReady,
                        ageLocalTs = clock.epochMillis(),
                ).apply {
                    targetDevices = msg.targetDevices.orEmpty()
                    roomId = null
                    requestInfo = validInfo.copy(transactionId = requestId)
                }
                verificationRequestsStore.addRequest(msg.otherUserId, request)
                msg.deferred.complete(request.toPendingVerificationRequest())
                dispatchUpdate(VerificationEvent.RequestAdded(request.toPendingVerificationRequest()))
            }
        } catch (failure: Throwable) {
            // some network problem
            msg.deferred.completeExceptionally(failure)
            return
        }
    }

    private suspend fun handleReadyReceived(msg: VerificationIntent.OnReadyReceived) {
        val matchingRequest = verificationRequestsStore.getExistingRequest(msg.fromUser, msg.transactionId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]: No matching request to ready tId:${msg.transactionId}")
//                    cancelRequest(msg.transactionId, msg.viaRoom, msg.fromUser, msg.readyInfo.fromDevice, CancelCode.UnknownTransaction)
                }
        val myDevice = verificationTrustBackend.getMyDeviceId()

        if (matchingRequest.state != EVerificationState.WaitingForReady) {
            cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
            return
        }
        // for room verification (user)
        // TODO if room and incoming I should check that right?
        // actually it will not reach that point? handleReadyByAnotherOfMySessionReceived would be called instead? and
        // the actor never sees event send by me in rooms
        if (matchingRequest.otherUserId != myUserId && msg.fromUser == myUserId && msg.readyInfo.fromDevice != myDevice) {
            // it's a ready from another of my devices, so we should just
            // ignore following messages related to that request
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}]: ready from another of my devices, make inactive")
            matchingRequest.state = EVerificationState.HandledByOtherSession
            dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
            return
        }

        if (matchingRequest.requestInfo?.methods?.canShowCode().orFalse() &&
                msg.readyInfo.methods.canScanCode()) {
            matchingRequest.qrCodeData = createQrCodeData(matchingRequest.requestId, msg.fromUser, msg.readyInfo.fromDevice)
        }
        matchingRequest.readyInfo = msg.readyInfo
        matchingRequest.state = EVerificationState.Ready
        dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))

//        if (matchingRequest.readyInfo != null) {
//            // TODO we already received a ready, cancel? or ignore
//            Timber.tag(loggerTag.value)
//                    .v("[${myUserId.take(8)}]: already received a ready for transaction ${msg.transactionId}")
//            return
//        }
//
//        updatePendingRequest(
//                matchingRequest.copy(
//                        readyInfo = msg.readyInfo,
//                )
//        )

        if (msg.viaRoom == null) {
            // we should cancel to others if it was requested via to_device
            // via room the other session will see the ready in room an mark the transaction as inactive for them
            val deviceIds = verificationTrustBackend.getUserDeviceList(matchingRequest.otherUserId)
                    .filter { it.deviceId != msg.readyInfo.fromDevice }
                    // if it's me we don't want to send self cancel
                    .filter { it.deviceId != myDevice }
                    .map { it.deviceId }

            try {
                transportLayer.sendToDeviceEvent(
                        EventType.KEY_VERIFICATION_CANCEL,
                        KeyVerificationCancel(
                                msg.transactionId,
                                CancelCode.AcceptedByAnotherDevice.value,
                                CancelCode.AcceptedByAnotherDevice.humanReadable
                        ),
                        matchingRequest.otherUserId,
                        deviceIds,
                )
            } catch (failure: Throwable) {
                // just fail silently in this case
                Timber.v("Failed to notify that accepted by another device")
            }
        }
    }

    private suspend fun handleReadyByAnotherOfMySessionReceived(msg: VerificationIntent.OnReadyByAnotherOfMySessionReceived) {
        val matchingRequest = verificationRequestsStore.getExistingRequest(msg.fromUser, msg.transactionId)
                ?: return

        // it's a ready from another of my devices, so we should just
        // ignore following messages related to that request
        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}]: ready from another of my devices, make inactive")
        matchingRequest.state = EVerificationState.HandledByOtherSession
        dispatchUpdate(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
        return
    }

//    private suspend fun updatePendingRequest(updated: PendingVerificationRequest) {
//        val requestsForUser = pendingRequests.getOrPut(updated.otherUserId) { mutableListOf() }
//        val index = requestsForUser.indexOfFirst {
//            it.transactionId == updated.transactionId ||
//                    it.transactionId == null && it.localId == updated.localId
//        }
//        if (index != -1) {
//            requestsForUser.removeAt(index)
//        }
//        requestsForUser.add(updated)
//        dispatchUpdate(VerificationEvent.RequestUpdated(updated))
//    }

    private fun dispatchRequestAdded(tx: KotlinVerificationRequest) {
        Timber.v("## SAS dispatchRequestAdded txId:${tx.requestId}")
        dispatchUpdate(VerificationEvent.RequestAdded(tx.toPendingVerificationRequest()))
    }

// Utilities

    private suspend fun createQrCodeDataForDistinctUser(requestId: String, otherUserId: String): QrCodeData.VerifyingAnotherUser? {
        val myMasterKey = verificationTrustBackend.getMyTrustedMasterKeyBase64()
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val otherUserMasterKey = verificationTrustBackend.getUserMasterKeyBase64(otherUserId)
                ?: run {
                    Timber.w("## Unable to get other user master key")
                    return null
                }

        return QrCodeData.VerifyingAnotherUser(
                transactionId = requestId,
                userMasterCrossSigningPublicKey = myMasterKey,
                otherUserMasterCrossSigningPublicKey = otherUserMasterKey,
                sharedSecret = generateSharedSecretV2()
        )
    }

    // Create a QR code to display on the old device (Osborne2)
    private suspend fun createQrCodeDataForVerifiedDevice(requestId: String, otherUserId: String, otherDeviceId: String?): QrCodeData.SelfVerifyingMasterKeyTrusted? {
        val myMasterKey = verificationTrustBackend.getUserMasterKeyBase64(myUserId)
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val otherDeviceKey = otherDeviceId
                ?.let {
                    verificationTrustBackend.getUserDevice(otherUserId, otherDeviceId)?.fingerprint()
                }
                ?: run {
                    Timber.w("## Unable to get other device data")
                    return null
                }

        return QrCodeData.SelfVerifyingMasterKeyTrusted(
                transactionId = requestId,
                userMasterCrossSigningPublicKey = myMasterKey,
                otherDeviceKey = otherDeviceKey,
                sharedSecret = generateSharedSecretV2()
        )
    }

    // Create a QR code to display on the new device (Dynabook)
    private suspend fun createQrCodeDataForUnVerifiedDevice(requestId: String): QrCodeData.SelfVerifyingMasterKeyNotTrusted? {
        val myMasterKey = verificationTrustBackend.getUserMasterKeyBase64(myUserId)
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val myDeviceKey = verificationTrustBackend.getUserDevice(myUserId, verificationTrustBackend.getMyDeviceId())?.fingerprint()
                ?: return null.also {
                    Timber.w("## Unable to get my fingerprint")
                }

        return QrCodeData.SelfVerifyingMasterKeyNotTrusted(
                transactionId = requestId,
                deviceKey = myDeviceKey,
                userMasterCrossSigningPublicKey = myMasterKey,
                sharedSecret = generateSharedSecretV2()
        )
    }

    private suspend fun cancelRequest(request: KotlinVerificationRequest, code: CancelCode) {
        request.state = EVerificationState.Cancelled
        request.cancelCode = code
        dispatchUpdate(VerificationEvent.RequestUpdated(request.toPendingVerificationRequest()))

        // should also update SAS/QR transaction
        getExistingTransaction<KotlinSasTransaction>(request.otherUserId, request.requestId)?.let {
            it.state = SasTransactionState.Cancelled(code, true)
            verificationRequestsStore.deleteTransaction(request.otherUserId, request.requestId)
            dispatchUpdate(VerificationEvent.TransactionUpdated(it))
        }
        getExistingTransaction<KotlinQRVerification>(request.otherUserId, request.requestId)?.let {
            it.state = QRCodeVerificationState.Cancelled
            verificationRequestsStore.deleteTransaction(request.otherUserId, request.requestId)
            dispatchUpdate(VerificationEvent.TransactionUpdated(it))
        }

        cancelRequest(
                request.requestId,
                request.roomId,
                request.otherUserId,
                request.otherDeviceId()?.let { listOf(it) } ?: request.targetDevices ?: emptyList(),
                code
        )
    }

    private suspend fun cancelRequest(transactionId: String, roomId: String?, otherUserId: String?, otherDeviceIds: List<String>, code: CancelCode) {
        try {
            if (roomId == null) {
                cancelTransactionToDevice(
                        transactionId,
                        otherUserId.orEmpty(),
                        otherDeviceIds,
                        code
                )
            } else {
                cancelTransactionInRoom(
                        roomId,
                        transactionId,
                        code
                )
            }
        } catch (failure: Throwable) {
            Timber.w("FAILED to cancel request $transactionId reason:${code.humanReadable}")
            // continue anyhow
        }
    }

    private suspend fun cancelTransactionToDevice(transactionId: String, otherUserId: String, otherUserDeviceIds: List<String>, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val cancelMessage = KeyVerificationCancel.create(transactionId, code)
//        val contentMap = MXUsersDevicesMap<Any>()
//        contentMap.setObject(otherUserId, otherUserDeviceId, cancelMessage)
        transportLayer.sendToDeviceEvent(
                messageType = EventType.KEY_VERIFICATION_CANCEL,
                toSendToDeviceObject = cancelMessage,
                otherUserId = otherUserId,
                targetDevices = otherUserDeviceIds
        )
//        sendToDeviceTask
//                .execute(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_CANCEL, contentMap))
    }

    private suspend fun cancelTransactionInRoom(roomId: String, transactionId: String, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val cancelMessage = MessageVerificationCancelContent.create(transactionId, code)
        transportLayer.sendInRoom(
                type = EventType.KEY_VERIFICATION_CANCEL,
                roomId = roomId,
                content = cancelMessage.toEventContent()
        )
    }

    private fun hashUsingAgreedHashMethod(hashMethod: String?, toHash: String): String {
        if ("sha256" == hashMethod?.lowercase(Locale.ROOT)) {
            return olmPrimitiveProvider.sha256(toHash)
        }
        throw java.lang.IllegalArgumentException("Unsupported hash method $hashMethod")
    }

    private suspend fun addTransaction(tx: VerificationTransaction) {
        verificationRequestsStore.addTransaction(tx)
        dispatchUpdate(VerificationEvent.TransactionAdded(tx))
    }

    private inline fun <reified T : VerificationTransaction> getExistingTransaction(otherUserId: String, transactionId: String): T? {
        return verificationRequestsStore.getExistingTransaction(otherUserId, transactionId) as? T
    }

    private inline fun <reified T : VerificationTransaction> getExistingTransaction(transactionId: String): T? {
        return verificationRequestsStore.getExistingTransaction(transactionId)
                .takeIf { it is T } as? T
//        txMap.forEach {
//            val match = it.value.values
//                    .firstOrNull { it.transactionId == transactionId }
//                    ?.takeIf { it is T }
//            if (match != null) return match as? T
//        }
//        return null
    }
}
