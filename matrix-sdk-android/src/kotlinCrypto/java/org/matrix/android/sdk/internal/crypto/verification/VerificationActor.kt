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

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.SendToDeviceObject
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationCancelContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationDoneContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationStartContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationCancel
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationDone
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationRequest
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import org.matrix.android.sdk.internal.crypto.model.rest.toValue
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.SendVerificationMessageTask
import org.matrix.android.sdk.internal.crypto.tools.withOlmUtility
import org.matrix.android.sdk.internal.crypto.verification.qrcode.QrCodeData
import org.matrix.android.sdk.internal.crypto.verification.qrcode.generateSharedSecretV2
import org.matrix.android.sdk.internal.crypto.verification.qrcode.toQrCodeData
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import java.util.Locale

// data class AddRequestActions(
//        val request: PendingVerificationRequest,
//        // only allow one active verification between two users
//        // so if there are already active requests they should be canceled
//        val toCancel: List<PendingVerificationRequest>
// )

private val loggerTag = LoggerTag("Verification", LoggerTag.CRYPTO)

internal class VerificationActor @AssistedInject constructor(
        @Assisted private val channel: Channel<VerificationIntent>,
        private val clock: Clock,
        @UserId private val myUserId: String,
        private val cryptoStore: IMXCryptoStore,
        private val sendVerificationMessageTask: SendVerificationMessageTask,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val sendToDeviceTask: SendToDeviceTask,
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        private val crossSigningService: dagger.Lazy<CrossSigningService>
) {

    @AssistedFactory
    interface Factory {
        fun create(channel: Channel<VerificationIntent>): VerificationActor
    }

    // map [sender : [transaction]]
    private val txMap = HashMap<String, MutableMap<String, VerificationTransaction>>()

    // we need to keep track of finished transaction
    // It will be used for gossiping (to send request after request is completed and 'done' by other)
    private val pastTransactions = HashMap<String, MutableMap<String, VerificationTransaction>>()

    /**
     * Map [sender: [PendingVerificationRequest]]
     * For now we keep all requests (even terminated ones) during the lifetime of the app.
     */
    private val pendingRequests = HashMap<String, MutableList<KotlinVerificationRequest>>()

    val eventFlow = MutableSharedFlow<VerificationEvent>(replay = 0)

    suspend fun send(intent: VerificationIntent) {
        channel.send(intent)
    }

    private suspend fun withMatchingRequest(
            otherUserId: String,
            requestId: String,
            viaRoom: String?,
            block: suspend ((KotlinVerificationRequest) -> Unit)
    ) {
        val matchingRequest = pendingRequests[otherUserId]
                ?.firstOrNull { it.requestId == requestId }
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
                handleRequestAdd(msg)
            }
            is VerificationIntent.OnReadyReceived -> {
                handleReadyReceived(msg)
            }
            is VerificationIntent.FailToSendRequest -> {
                // just delete it?
                val requestsForUser = pendingRequests.getOrPut(msg.request.otherUserId) { mutableListOf() }
                val index = requestsForUser.indexOfFirst {
                    it.requestId == msg.request.transactionId
                }
                if (index != -1) {
                    requestsForUser.removeAt(index)
                }
            }
//            is VerificationIntent.UpdateRequest -> {
//                updatePendingRequest(msg.request)
//            }
            is VerificationIntent.GetExistingRequestInRoom -> {
                val existing = pendingRequests.flatMap { entry ->
                    entry.value.filter { it.roomId == msg.roomId && it.requestId == msg.transactionId }
                }.firstOrNull()
                msg.deferred.complete(existing?.toPendingVerificationRequest())
            }
            is VerificationIntent.OnVerificationRequestReceived -> {
                handleIncomingRequest(msg)
            }
            is VerificationIntent.ActionReadyRequest -> {
                handleReadyRequest(msg)
            }
            is VerificationIntent.ActionStartSasVerification -> {
                handleSasStart(msg)
            }
            is VerificationIntent.ActionReciprocateQrVerification -> {
                handleReciprocateQR(msg)
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
                pendingRequests
                        .flatMap { it.value }
                        .firstOrNull { it.requestId == msg.transactionId }
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
                val matchingRequest = pendingRequests[msg.fromUser]
                        ?.firstOrNull { it.requestId == msg.transactionId } ?: return
                if (matchingRequest.state != EVerificationState.HandledByOtherSession) {
                    cancelRequest(matchingRequest, CancelCode.InvalidMessage)
                }
            }
            is VerificationIntent.GetExistingRequestsForUser -> {
                pendingRequests[msg.userId].orEmpty().let { requests ->
                    msg.deferred.complete(requests.map { it.toPendingVerificationRequest() })
                }
            }
            is VerificationIntent.GetExistingTransaction -> {
                txMap[msg.fromUser]?.get(msg.transactionId)?.let {
                    msg.deferred.complete(it)
                }
            }
            is VerificationIntent.GetExistingRequest -> {
                pendingRequests[msg.otherUserId]
                        ?.firstOrNull { msg.transactionId == it.requestId }
                        ?.let {
                            msg.deferred.complete(it.toPendingVerificationRequest())
                        }
            }
            is VerificationIntent.OnCancelReceived -> {
                withMatchingRequest(msg.fromUser, msg.validCancel.transactionId, msg.viaRoom) { request ->
                    // update as canceled
                    request.state = EVerificationState.Cancelled
                    val cancelCode = safeValueOf(msg.validCancel.code)
                    request.cancelCode = cancelCode
                    val existingTx = txMap[msg.fromUser]?.get(msg.validCancel.transactionId)
                    if (existingTx != null) {
                        existingTx.state = VerificationTxState.Cancelled(cancelCode, false)
                        txMap[msg.fromUser]?.remove(msg.validCancel.transactionId)
                        eventFlow.emit(VerificationEvent.TransactionUpdated(existingTx))
                    }
                    eventFlow.emit(VerificationEvent.RequestUpdated(request.toPendingVerificationRequest()))

                }
            }
            is VerificationIntent.OnReadyByAnotherOfMySessionReceived -> {
                handleReadyByAnotherOfMySessionReceived(msg)
            }
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

        pendingRequests.getOrPut(msg.senderId) { mutableListOf() }
                .add(pendingVerificationRequest)
        dispatchRequestAdded(pendingVerificationRequest)
    }

    private suspend fun onStartReceived(msg: VerificationIntent.OnStartReceived) {
        val requestId = msg.validVerificationInfoStart.transactionId
        val matchingRequest = pendingRequests[msg.fromUser]?.firstOrNull { it.requestId == requestId }
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
        eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
    }

    private suspend fun handleReceiveStartForQR(request: KotlinVerificationRequest, reciprocate: ValidVerificationInfoStart.ReciprocateVerificationInfoStart) {
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
        val existing = getExistingTransaction(msg.fromUser, requestId)
        if (existing != null) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] No existing Sas transaction for ${request.requestId}")
            tryOrNull { cancelRequest(request, CancelCode.UnexpectedMessage) }
            return
        }

        val sasTx = SasV1Transaction(
                channel = channel,
                transactionId = requestId,
                state = VerificationTxState.None,
                otherUserId = request.otherUserId,
                myUserId = myUserId,
                myTrustedMSK = cryptoStore.getMyCrossSigningInfo()
                        ?.takeIf { it.isTrusted() }
                        ?.masterKey()
                        ?.unpaddedBase64PublicKey,
                otherDeviceId = request.otherDeviceId(),
                myDeviceId = cryptoStore.getDeviceId(),
                myDeviceFingerprint = cryptoStore.getUserDevice(myUserId, cryptoStore.getDeviceId())?.fingerprint().orEmpty(),
                startReq = sasStart,
                isIncoming = true,
                isToDevice = msg.viaRoom == null
        )
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
        val mxDeviceInfo = cryptoStore.getUserDevice(userId = request.otherUserId, deviceId = otherDeviceId)

        if (mxDeviceInfo?.fingerprint() == null) {
            Timber.e("## SAS Failed to find device key ")
            // TODO force download keys!!
            // would be probably better to download the keys
            // for now I cancel
            cancelRequest(request, CancelCode.UserError)
            return
        }

        val concat = sasTx.getSAS().publicKey + sasStart.canonicalJson
        val commitment = hashUsingAgreedHashMethod(agreedHash, concat)

        val accept = SasV1Transaction.sasAccept(
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
            sendToOther(request, EventType.KEY_VERIFICATION_ACCEPT, accept)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}] Failed to send accept for ${request.requestId}")
            tryOrNull { cancelRequest(request, CancelCode.User) }
        }

        sasTx.accepted = accept.asValidObject()
        sasTx.state = VerificationTxState.SasAccepted

        addTransaction(sasTx)
    }

    private suspend fun handleReceiveAccept(matchingRequest: KotlinVerificationRequest, msg: VerificationIntent.OnAcceptReceived) {
        val requestId = msg.validAccept.transactionId

        val existing = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.v("on accept received in room ${msg.viaRoom} for verification id:${requestId} in room ${matchingRequest.roomId}")
                }

        // Existing should be in
        if (existing.state != VerificationTxState.SasStarted) {
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
        val pubKey = existing.getSAS().publicKey

        val keyMessage = SasV1Transaction.sasKeyMessage(matchingRequest.roomId != null, requestId, pubKey)

        try {

            if (BuildConfig.LOG_PRIVATE_DATA) {
                Timber.tag(loggerTag.value)
                        .v("[${myUserId.take(8)}]: Sending my key $pubKey")
            }
            sendToOther(
                    matchingRequest,
                    EventType.KEY_VERIFICATION_KEY,
                    keyMessage,
            )
        } catch (failure: Throwable) {
            existing.state = VerificationTxState.Cancelled(CancelCode.UserError, true)
            matchingRequest.cancelCode = CancelCode.UserError
            matchingRequest.state = EVerificationState.Cancelled
            eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
            eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
            return
        }
        existing.accepted = accept
        existing.state = VerificationTxState.SasKeySent
        eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
    }

    private suspend fun handleSasStart(msg: VerificationIntent.ActionStartSasVerification) {
        val matchingRequest = pendingRequests
                .flatMap { entry ->
                    entry.value.filter { it.requestId == msg.requestId }
                }.firstOrNull()
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Unknown request"))
                }

        if (matchingRequest.state != EVerificationState.Ready) {
            msg.deferred.completeExceptionally(java.lang.IllegalStateException("Can't start a non ready request"))
            return
        }

        val otherDeviceId = matchingRequest.otherDeviceId() ?: return Unit.also {
            msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Failed to find other device Id"))
        }

        val existingTransaction = getExistingTransaction(msg.otherUserId, msg.requestId)
        if (existingTransaction is SasVerificationTransaction) {
            // there is already an existing transaction??
            msg.deferred.completeExceptionally(IllegalStateException("Already started"))
            return
        }
        val startMessage = SasV1Transaction.sasStart(
                inRoom = matchingRequest.roomId != null,
                fromDevice = cryptoStore.getDeviceId(),
                requestId = msg.requestId
        )

        sendToOther(
                matchingRequest,
                EventType.KEY_VERIFICATION_START,
                startMessage,
        )

        // should check if already one (and cancel it)
        val tx = SasV1Transaction(
                channel = channel,
                transactionId = msg.requestId,
                state = VerificationTxState.SasStarted,
                otherUserId = msg.otherUserId,
                myUserId = myUserId,
                myTrustedMSK = cryptoStore.getMyCrossSigningInfo()
                        ?.takeIf { it.isTrusted() }
                        ?.masterKey()
                        ?.unpaddedBase64PublicKey,
                otherDeviceId = otherDeviceId,
                myDeviceId = cryptoStore.getDeviceId(),
                myDeviceFingerprint = cryptoStore.getUserDevice(myUserId, cryptoStore.getDeviceId())?.fingerprint().orEmpty(),
                startReq = startMessage.asValidObject() as ValidVerificationInfoStart.SasVerificationInfoStart,
                isIncoming = false,
                isToDevice = matchingRequest.roomId == null
        )

        matchingRequest.state = EVerificationState.WeStarted
        eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
        addTransaction(tx)

        msg.deferred.complete(tx)
    }

    private suspend fun handleReciprocateQR(msg: VerificationIntent.ActionReciprocateQrVerification) {
        val matchingRequest = pendingRequests
                .flatMap { entry ->
                    entry.value.filter { it.requestId == msg.requestId }
                }.firstOrNull()
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Unknown request"))
                }

        if (matchingRequest.state != EVerificationState.Ready) {
            msg.deferred.completeExceptionally(java.lang.IllegalStateException("Can't start a non ready request"))
            return
        }

        val otherDeviceId = matchingRequest.otherDeviceId() ?: return Unit.also {
            msg.deferred.completeExceptionally(java.lang.IllegalArgumentException("Failed to find other device Id"))
        }

        val existingTransaction = getExistingTransaction(msg.otherUserId, msg.requestId)
        // what if there is an existing??
        if (existingTransaction != null) {
            // cancel or replace??
            return
        }

        val myMasterKey = crossSigningService.get()
                .getUserCrossSigningKeys(myUserId)?.masterKey()?.unpaddedBase64PublicKey
        var canTrustOtherUserMasterKey = false

        // Check the other device view of my MSK
        val otherQrCodeData = msg.scannedData.toQrCodeData()
        when (otherQrCodeData) {
            null -> {
                msg.deferred.completeExceptionally(IllegalArgumentException("Malformed QrCode data"))
                return
            }
            is QrCodeData.VerifyingAnotherUser -> {
                // key2 (aka otherUserMasterCrossSigningPublicKey) is what the one displaying the QR code (other user) think my MSK is.
                // Let's check that it's correct
                // If not -> Cancel
                if (otherQrCodeData.otherUserMasterCrossSigningPublicKey != myMasterKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.otherUserMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                } else Unit
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted -> {
                // key1 (aka userMasterCrossSigningPublicKey) is the session displaying the QR code view of our MSK.
                // Let's check that I see the same MSK
                // If not -> Cancel
                if (otherQrCodeData.userMasterCrossSigningPublicKey != myMasterKey) {
                    Timber.d("## Verification QR: Invalid other master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
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
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                } else {
                    // Nothing special here, we will send a reciprocate start event, and then the other session will trust it's view of the MSK
                }
            }
        }

        val toVerifyDeviceIds = mutableListOf<String>()

        // Let's now check the other user/device key material
        when (otherQrCodeData) {
            is QrCodeData.VerifyingAnotherUser -> {
                // key1(aka userMasterCrossSigningPublicKey) is the MSK of the one displaying the QR code (i.e other user)
                // Let's check that it matches what I think it should be
                if (otherQrCodeData.userMasterCrossSigningPublicKey
                        != crossSigningService.get().getUserCrossSigningKeys(msg.otherUserId)?.masterKey()?.unpaddedBase64PublicKey) {
                    Timber.d("## Verification QR: Invalid user master key ${otherQrCodeData.userMasterCrossSigningPublicKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                } else {
                    // It does so i should mark it as trusted
                    canTrustOtherUserMasterKey = true
                }
            }
            is QrCodeData.SelfVerifyingMasterKeyTrusted -> {
                // key2 (aka otherDeviceKey) is my current device key in POV of the one displaying the QR code (i.e other device)
                // Let's check that it's correct
                if (otherQrCodeData.otherDeviceKey
                        != cryptoStore.getUserDevice(myUserId, cryptoStore.getDeviceId())?.fingerprint()) {
                    Timber.d("## Verification QR: Invalid other device key ${otherQrCodeData.otherDeviceKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
                    return
                } else Unit // Nothing special here, we will send a reciprocate start event, and then the other session will trust my device
                // and thus allow me to request SSSS secret
            }
            is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> {
                // key1 (aka otherDeviceKey) is the device key of the one displaying the QR code (i.e other device)
                // Let's check that it matches what I have locally
                if (otherQrCodeData.deviceKey
                        != cryptoStore.getUserDevice(msg.otherUserId, otherDeviceId ?: "")?.fingerprint()) {
                    Timber.d("## Verification QR: Invalid device key ${otherQrCodeData.deviceKey}")
                    cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
                    msg.deferred.complete(null)
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
            cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
            return
        }

        // All checks are correct
        // Send the shared secret so that sender can trust me
        // qrCodeData.sharedSecret will be used to send the start request
        val message = if (matchingRequest.roomId != null) {
            MessageVerificationStartContent(
                    fromDevice = cryptoStore.getDeviceId(),
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
                    fromDevice = cryptoStore.getDeviceId(),
                    sharedSecret = otherQrCodeData.sharedSecret,
                    method = VERIFICATION_METHOD_RECIPROCATE,
            )
        }

        try {
            sendToOther(matchingRequest, EventType.KEY_VERIFICATION_START, message)
        } catch (failure: Throwable) {
            msg.deferred.completeExceptionally(failure)
            return
        }

        matchingRequest.state = EVerificationState.WeStarted
        eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))

        val tx = KotlinQRVerification(
                qrCodeText = msg.scannedData,
                state = VerificationTxState.WaitingOtherReciprocateConfirm,
                method = VerificationMethod.QR_CODE_SCAN,
                transactionId = msg.requestId,
                otherUserId = msg.otherUserId,
                otherDeviceId = matchingRequest.otherDeviceId(),
                isIncoming = false
        )
        addTransaction(tx)
    }

    private suspend fun handleReceiveKey(matchingRequest: KotlinVerificationRequest, msg: VerificationIntent.OnKeyReceived) {
        val requestId = msg.validKey.transactionId

        val existing = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]: No matching transaction for key tId:$requestId")
                }

        // Existing should be in SAS key sent
        val isCorrectState = if (existing.isIncoming) {
            existing.state == VerificationTxState.SasAccepted
        } else {
            existing.state == VerificationTxState.SasKeySent
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
            val pubKey = existing.getSAS().publicKey
            val keyMessage = SasV1Transaction.sasKeyMessage(matchingRequest.roomId != null, requestId, pubKey)
            try {
                sendToOther(
                        matchingRequest,
                        EventType.KEY_VERIFICATION_KEY,
                        keyMessage,
                )
                if (BuildConfig.LOG_PRIVATE_DATA) {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:i calculate SAS my key $pubKey their Key: $otherKey")
                }
                existing.calculateSASBytes(otherKey)
                existing.state = VerificationTxState.SasShortCodeReady
                if (BuildConfig.LOG_PRIVATE_DATA) {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:i CODE ${existing.getDecimalCodeRepresentation()}")
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]:i EMOJI CODE ${existing.getEmojiCodeRepresentation().joinToString(" ") { it.emoji }}")
                }
                eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
            } catch (failure: Throwable) {
                existing.state = VerificationTxState.Cancelled(CancelCode.UserError, true)
                matchingRequest.state = EVerificationState.Cancelled
                matchingRequest.cancelCode = CancelCode.UserError
                eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
                eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
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
                            .v("[${myUserId.take(8)}]:o calculate SAS my key ${existing.getSAS().publicKey} their Key: $otherKey")
                }
                existing.calculateSASBytes(otherKey)
                existing.state = VerificationTxState.SasShortCodeReady
                eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
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

        val existing = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}] on Mac for unknown transaction with id:$requestId")
                }

        when (existing.state) {
            is VerificationTxState.SasMacSent -> {
                existing.theirMac = msg.validMac
                finalizeSasTransaction(existing, msg.validMac, matchingRequest, existing.transactionId)
            }
            is VerificationTxState.SasShortCodeReady -> {
                // I can start verify, store it
                existing.theirMac = msg.validMac
                existing.state = VerificationTxState.SasMacReceived(false)
                eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
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
        val matchingRequest = pendingRequests.flatMap { it.value }.firstOrNull { it.requestId == transactionId }
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Request"))
                }
        if (matchingRequest.isFinished()) {
            return Unit.also {
                msg.deferred.completeExceptionally(IllegalStateException("Request was cancelled"))
            }
        }
        val existing = getExistingTransaction(transactionId)
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Transaction"))
                }

        val isCorrectState = when (val state = existing.state) {
            is VerificationTxState.SasShortCodeReady -> true
            is VerificationTxState.SasMacReceived -> !state.codeConfirmed
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

        val existing = getExistingTransaction(msg.fromUser, requestId)
                ?: return Unit.also {
                    Timber.v("on accept received in room ${msg.viaRoom} for verification id:${requestId} in room ${matchingRequest.roomId}")
                }

        val state = existing.state
        val isCorrectState = state is VerificationTxState.Done && !state.otherDone

        if (isCorrectState) {
            // XXX whatabout waiting for done?
            matchingRequest.state = EVerificationState.Done
            eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
//            updatePendingRequest(
//                    matchingRequest.copy(
//                            isSuccessful = true
//                    )
//            )
        } else {
            // TODO cancel?
        }
    }

    private suspend fun handleSasCodeMatch(msg: VerificationIntent.ActionSASCodeMatches) {
        val transactionId = msg.transactionId
        val matchingRequest = pendingRequests.flatMap { it.value }.firstOrNull { it.requestId == transactionId }
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Request"))
                }

        if (matchingRequest.state != EVerificationState.WeStarted
                && matchingRequest.state != EVerificationState.Started) {
            return Unit.also {
                msg.deferred.completeExceptionally(IllegalStateException("Can't accept code in state: ${matchingRequest.state}"))
            }
        }

        val existing = getExistingTransaction(transactionId)
                ?: return Unit.also {
                    msg.deferred.completeExceptionally(IllegalStateException("Unknown Transaction"))
                }

        val isCorrectState = when (val state = existing.state) {
            is VerificationTxState.SasShortCodeReady -> true
            is VerificationTxState.SasMacReceived -> !state.codeConfirmed
            else -> false
        }
        if (!isCorrectState) {
            return Unit.also {
                msg.deferred.completeExceptionally(IllegalStateException("Unexpected action, can't match in this state"))
            }
        }

        val macInfo = existing.computeMyMac()

        val macMsg = SasV1Transaction.sasMacMessage(matchingRequest.roomId != null, transactionId, macInfo)
        try {
            sendToOther(matchingRequest, EventType.KEY_VERIFICATION_MAC, macMsg)
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
            existing.state = VerificationTxState.SasMacSent
            eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
        }

        msg.deferred.complete(Unit)
    }

    private suspend fun finalizeSasTransaction(
            existing: SasV1Transaction,
            theirMac: ValidVerificationInfoMac,
            matchingRequest: KotlinVerificationRequest,
            transactionId: String
    ) {
        val result = existing.verifyMacs(
                theirMac,
                cryptoStore.getUserDeviceList(matchingRequest.otherUserId).orEmpty(),
                cryptoStore.getCrossSigningInfo(matchingRequest.otherUserId)?.masterKey()?.unpaddedBase64PublicKey
        )

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] verify macs result $result id:$transactionId")
        when (result) {
            is SasV1Transaction.MacVerificationResult.Success -> {
                // mark the devices as locally trusted
                result.verifiedDeviceId.forEach { deviceId ->
                    val actualTrustLevel = cryptoStore.getUserDevice(matchingRequest.otherUserId, deviceId)?.trustLevel
                    setDeviceVerificationAction.handle(
                            trustLevel = DeviceTrustLevel(
                                    actualTrustLevel?.crossSigningVerified == true,
                                    true
                            ),
                            userId = matchingRequest.otherUserId,
                            deviceId = deviceId
                    )

                    if (matchingRequest.otherUserId == myUserId && crossSigningService.get().canCrossSign()) {
                        // If me it's reasonable to sign and upload the device signature for the other part
                        try {
                            crossSigningService.get().trustDevice(deviceId)
                        } catch (failure: Throwable) {
                            // network problem??
                            Timber.w("## Verification: Failed to sign new device $deviceId, ${failure.localizedMessage}")
                        }
                    }
                }

                if (result.otherMskTrusted) {
                    if (matchingRequest.otherUserId == myUserId) {
                        cryptoStore.markMyMasterKeyAsLocallyTrusted(true)
                    } else {
                        // what should we do if this fails :/
                        if (crossSigningService.get().canCrossSign()) {
                            crossSigningService.get().trustUser(matchingRequest.otherUserId)
                        }
                    }
                }

                // we should send done and wait for done
                sendToOther(
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

                existing.state = VerificationTxState.Done(false)
                eventFlow.emit(VerificationEvent.TransactionUpdated(existing))
                pastTransactions.getOrPut(transactionId) { mutableMapOf() }[transactionId] = existing
                txMap[matchingRequest.otherUserId]?.remove(transactionId)
                matchingRequest.state = EVerificationState.WaitingForDone
                eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
            }
            SasV1Transaction.MacVerificationResult.MismatchKeys,
            SasV1Transaction.MacVerificationResult.MismatchMacCrossSigning,
            is SasV1Transaction.MacVerificationResult.MismatchMacDevice,
            SasV1Transaction.MacVerificationResult.NoDevicesVerified -> {
                cancelRequest(matchingRequest, CancelCode.MismatchedKeys)
            }
        }
    }

    private suspend fun handleReadyRequest(msg: VerificationIntent.ActionReadyRequest) {
        val existing = pendingRequests
                .flatMap { it.value }
                .firstOrNull { it.requestId == msg.transactionId }
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
            cancelRequest(existing, CancelCode.UnknownMethod)
            msg.deferred.complete(null)
            return
        }

        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}] Request ${msg.transactionId} agreement is $commonMethods")

        val qrCodeData = if (otherUserMethods.canScanCode() && msg.methods.contains(VerificationMethod.QR_CODE_SHOW)) {
            createQrCodeData(msg.transactionId, existing.otherUserId, existing.requestInfo?.fromDevice)
        } else {
            null
        }

        val readyInfo = ValidVerificationInfoReady(
                msg.transactionId,
                cryptoStore.getDeviceId(),
                commonMethods
        )

        val message = SasV1Transaction.sasReady(
                inRoom = existing.roomId != null,
                requestId = msg.transactionId,
                methods = commonMethods,
                fromDevice = cryptoStore.getDeviceId()
        )
        try {
            sendToOther(existing, EventType.KEY_VERIFICATION_READY, message)
        } catch (failure: Throwable) {
            msg.deferred.completeExceptionally(failure)
            return
        }

        existing.readyInfo = readyInfo
        existing.qrCodeData = qrCodeData
        existing.state = EVerificationState.Ready
        eventFlow.emit(VerificationEvent.RequestUpdated(existing.toPendingVerificationRequest()))

        Timber.tag(loggerTag.value).v("Request ${msg.transactionId} updated $existing")
        msg.deferred.complete(existing.toPendingVerificationRequest())
    }

    private fun createQrCodeData(requestId: String, otherUserId: String, otherDeviceId: String?): QrCodeData? {
        return when {
            myUserId != otherUserId ->
                createQrCodeDataForDistinctUser(requestId, otherUserId)
            cryptoStore.getMyCrossSigningInfo()?.isTrusted().orFalse() ->
                // This is a self verification and I am the old device (Osborne2)
                createQrCodeDataForVerifiedDevice(requestId, otherUserId, otherDeviceId)
            else ->
                // This is a self verification and I am the new device (Dynabook)
                createQrCodeDataForUnVerifiedDevice(requestId)
        }
    }

    private fun getMethodAgreement(
            otherUserMethods: List<String>?,
            methods: List<VerificationMethod>,
    ): List<String> {
        if (otherUserMethods.isNullOrEmpty()) {
            return emptyList()
        }

        val result = mutableSetOf<String>()

        if (VERIFICATION_METHOD_SAS in otherUserMethods && VerificationMethod.SAS in methods) {
            // Other can do SAS and so do I
            result.add(VERIFICATION_METHOD_SAS)
        }

        if (VERIFICATION_METHOD_QR_CODE_SCAN in otherUserMethods || VERIFICATION_METHOD_QR_CODE_SHOW in otherUserMethods) {
            if (VERIFICATION_METHOD_QR_CODE_SCAN in otherUserMethods && VerificationMethod.QR_CODE_SHOW in methods) {
                // Other can Scan and I can show QR code
                result.add(VERIFICATION_METHOD_QR_CODE_SHOW)
                result.add(VERIFICATION_METHOD_RECIPROCATE)
            }
            if (VERIFICATION_METHOD_QR_CODE_SHOW in otherUserMethods && VerificationMethod.QR_CODE_SCAN in methods) {
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

    private suspend fun handleRequestAdd(msg: VerificationIntent.ActionRequestVerification) {
        val requestsForUser = pendingRequests.getOrPut(msg.otherUserId) { mutableListOf() }
        // there can only be one active request per user, so cancel existing ones
        requestsForUser.toList().forEach { existingRequest ->
            if (!existingRequest.isFinished()) {
                Timber.d("## SAS, cancelling pending requests to start a new one")
                cancelRequest(existingRequest, CancelCode.User)
            }
        }

        val validLocalId = LocalEcho.createLocalEchoId()

        val methodValues = if (cryptoStore.getMyCrossSigningInfo()?.isTrusted().orFalse()) {
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
                fromDevice = cryptoStore.getDeviceId(),
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
                val event = createEventAndLocalEcho(
                        localId = validLocalId,
                        type = EventType.MESSAGE,
                        roomId = msg.roomId,
                        content = info.toContent()
                )
                val eventId = sendEventInRoom(event)
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
                requestsForUser.add(request)
                msg.deferred.complete(request.toPendingVerificationRequest())
                eventFlow.emit(VerificationEvent.RequestAdded(request.toPendingVerificationRequest()))
            } else {
                val requestId = LocalEcho.createLocalEchoId()
                sendToDeviceEvent(
                        messageType = EventType.KEY_VERIFICATION_REQUEST,
                        toSendToDeviceObject = KeyVerificationRequest(
                                transactionId = requestId,
                                fromDevice = cryptoStore.getDeviceId(),
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
                requestsForUser.add(request)
                msg.deferred.complete(request.toPendingVerificationRequest())
                eventFlow.emit(VerificationEvent.RequestAdded(request.toPendingVerificationRequest()))
            }
        } catch (failure: Throwable) {
            // some network problem
            msg.deferred.completeExceptionally(failure)
            return
        }
    }

    private suspend fun handleReadyReceived(msg: VerificationIntent.OnReadyReceived) {
        val matchingRequest = pendingRequests[msg.fromUser]?.firstOrNull { it.requestId == msg.transactionId }
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .v("[${myUserId.take(8)}]: No matching request to ready tId:${msg.transactionId}")
//                    cancelRequest(msg.transactionId, msg.viaRoom, msg.fromUser, msg.readyInfo.fromDevice, CancelCode.UnknownTransaction)
                }
        val myDevice = cryptoStore.getDeviceId()

        if (matchingRequest.state != EVerificationState.WaitingForReady) {
            cancelRequest(matchingRequest, CancelCode.UnexpectedMessage)
            return
        }
        // for room verification
        if (msg.fromUser == myUserId && msg.readyInfo.fromDevice != myDevice) {
            // it's a ready from another of my devices, so we should just
            // ignore following messages related to that request
            Timber.tag(loggerTag.value)
                    .v("[${myUserId.take(8)}]: ready from another of my devices, make inactive")
            matchingRequest.state = EVerificationState.HandledByOtherSession
            eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
            return
        }

        matchingRequest.readyInfo = msg.readyInfo
        matchingRequest.state = EVerificationState.Ready
        eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))

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
            val deviceIds = cryptoStore.getUserDevices(matchingRequest.otherUserId)?.keys
                    ?.filter { it != msg.readyInfo.fromDevice }
                    // if it's me we don't want to send self cancel
                    ?.filter { it != myDevice }
                    .orEmpty()

            try {
                sendToDeviceEvent(
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
        val matchingRequest = pendingRequests[msg.fromUser]?.firstOrNull { it.requestId == msg.transactionId }
                ?: return

        // it's a ready from another of my devices, so we should just
        // ignore following messages related to that request
        Timber.tag(loggerTag.value)
                .v("[${myUserId.take(8)}]: ready from another of my devices, make inactive")
        matchingRequest.state = EVerificationState.HandledByOtherSession
        eventFlow.emit(VerificationEvent.RequestUpdated(matchingRequest.toPendingVerificationRequest()))
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
//        eventFlow.emit(VerificationEvent.RequestUpdated(updated))
//    }

    private suspend fun dispatchRequestAdded(tx: KotlinVerificationRequest) {
        Timber.v("## SAS dispatchRequestAdded txId:${tx.requestId}")
        eventFlow.emit(VerificationEvent.RequestAdded(tx.toPendingVerificationRequest()))
    }

// Utilities

    private fun createQrCodeDataForDistinctUser(requestId: String, otherUserId: String): QrCodeData.VerifyingAnotherUser? {
        val myMasterKey = cryptoStore.getMyCrossSigningInfo()
                ?.masterKey()
                ?.unpaddedBase64PublicKey
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val otherUserMasterKey = cryptoStore.getCrossSigningInfo(otherUserId)
                ?.masterKey()
                ?.unpaddedBase64PublicKey
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
    private fun createQrCodeDataForVerifiedDevice(requestId: String, otherUserId: String, otherDeviceId: String?): QrCodeData.SelfVerifyingMasterKeyTrusted? {
        val myMasterKey = cryptoStore.getMyCrossSigningInfo()
                ?.masterKey()
                ?.unpaddedBase64PublicKey
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val otherDeviceKey = otherDeviceId
                ?.let {
                    cryptoStore.getUserDevice(otherUserId, otherDeviceId)?.fingerprint()
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
    private fun createQrCodeDataForUnVerifiedDevice(requestId: String): QrCodeData.SelfVerifyingMasterKeyNotTrusted? {
        val myMasterKey = cryptoStore.getMyCrossSigningInfo()
                ?.masterKey()
                ?.unpaddedBase64PublicKey
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val myDeviceKey = cryptoStore.getUserDevice(myUserId, cryptoStore.getDeviceId())?.fingerprint()
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

    private fun createEventAndLocalEcho(localId: String = LocalEcho.createLocalEchoId(), type: String, roomId: String, content: Content): Event {
        return Event(
                roomId = roomId,
                originServerTs = clock.epochMillis(),
                senderId = myUserId,
                eventId = localId,
                type = type,
                content = content,
                unsignedData = UnsignedData(age = null, transactionId = localId)
        ).also {
            localEchoEventFactory.createLocalEcho(it)
        }
    }

    private suspend fun sendEventInRoom(event: Event): String {
        return sendVerificationMessageTask.execute(SendVerificationMessageTask.Params(event, 5)).eventId
    }

    private suspend fun sendToDeviceEvent(messageType: String, toSendToDeviceObject: SendToDeviceObject, otherUserId: String, targetDevices: List<String>) {
        // TODO currently to device verification messages are sent unencrypted
        // as per spec not recommended
        // > verification messages may be sent unencrypted, though this is not encouraged.

        val contentMap = MXUsersDevicesMap<Any>()

        targetDevices.forEach {
            contentMap.setObject(otherUserId, it, toSendToDeviceObject)
        }

        sendToDeviceTask
                .execute(SendToDeviceTask.Params(messageType, contentMap))
    }

    suspend fun sendToOther(
            request: KotlinVerificationRequest,
            type: String,
            verificationInfo: VerificationInfo<*>,
    ) {
        val roomId = request.roomId
        if (roomId != null) {
            val event = createEventAndLocalEcho(
                    type = type,
                    roomId = roomId,
                    content = verificationInfo.toEventContent()!!
            )
            sendEventInRoom(event)
        } else {
            sendToDeviceEvent(
                    type,
                    verificationInfo.toSendToDeviceObject()!!,
                    request.otherUserId,
                    request.otherDeviceId()?.let { listOf(it) }.orEmpty()
            )
        }
    }

    private suspend fun cancelRequest(request: KotlinVerificationRequest, code: CancelCode) {
        request.state = EVerificationState.Cancelled
        request.cancelCode = code
        eventFlow.emit(VerificationEvent.RequestUpdated(request.toPendingVerificationRequest()))

        // should also update SAS/QR transaction
        getExistingTransaction(request.otherUserId, request.requestId)?.let {
            it.state = VerificationTxState.Cancelled(code, true)
            txMap[request.otherUserId]?.remove(request.requestId)
            eventFlow.emit(VerificationEvent.TransactionUpdated(it))
        }
        cancelRequest(request.requestId, request.roomId, request.otherUserId, request.otherDeviceId(), code)
    }

    private suspend fun cancelRequest(transactionId: String, roomId: String?, otherUserId: String?, otherDeviceId: String?, code: CancelCode) {
        try {
            if (roomId == null) {
                cancelTransactionToDevice(
                        transactionId,
                        otherUserId.orEmpty(),
                        otherDeviceId.orEmpty(),
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

    private suspend fun cancelTransactionToDevice(transactionId: String, otherUserId: String, otherUserDeviceId: String?, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val cancelMessage = KeyVerificationCancel.create(transactionId, code)
        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(otherUserId, otherUserDeviceId, cancelMessage)
        sendToDeviceTask
                .execute(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_CANCEL, contentMap))
    }

    private suspend fun cancelTransactionInRoom(roomId: String, transactionId: String, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val cancelMessage = MessageVerificationCancelContent.create(transactionId, code)
        val event = createEventAndLocalEcho(
                type = EventType.KEY_VERIFICATION_CANCEL,
                roomId = roomId,
                content = cancelMessage.toEventContent()
        )
        sendEventInRoom(event)
    }

    private fun hashUsingAgreedHashMethod(hashMethod: String?, toHash: String): String {
        if ("sha256" == hashMethod?.lowercase(Locale.ROOT)) {
            return withOlmUtility {
                it.sha256(toHash)
            }
        }
        throw java.lang.IllegalArgumentException("Unsupported hash method $hashMethod")
    }

    private suspend fun addTransaction(tx: SasV1Transaction) {
        val txInnerMap = txMap.getOrPut(tx.otherUserId) { mutableMapOf() }
        txInnerMap[tx.transactionId] = tx
        eventFlow.emit(VerificationEvent.TransactionAdded(tx))
    }

    private fun getExistingTransaction(otherUserId: String, transactionId: String): SasV1Transaction? {
        return txMap[otherUserId]?.get(transactionId)
    }

    private inline fun <reified T: VerificationTransaction> getExistingTransaction(transactionId: String, type: T): T? {
        txMap.forEach {
            val match = it.value.values
                    .firstOrNull { it.transactionId == transactionId }
                    ?.takeIf { it is T }
            if (match != null) return match as? T
        }
        return null
    }
}
