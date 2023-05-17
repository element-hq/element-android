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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationAcceptContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationCancelContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationDoneContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationKeyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationMacContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationReadyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationStartContent
import org.matrix.android.sdk.internal.crypto.DeviceListManager
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationAccept
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationCancel
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationDone
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationKey
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationMac
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationReady
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationRequest
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultVerificationService @Inject constructor(
        @UserId private val userId: String,
        @DeviceId private val myDeviceId: String?,
        private val cryptoStore: IMXCryptoStore,
//        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
//        private val secretShareManager: SecretShareManager,
//        private val myDeviceInfoHolder: Lazy<MyDeviceInfoHolder>,
        private val deviceListManager: DeviceListManager,
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
//        private val verificationTransportRoomMessageFactor Oy: VerificationTransportRoomMessageFactory,
//        private val verificationTransportToDeviceFactory: VerificationTransportToDeviceFactory,
//        private val crossSigningService: CrossSigningService,
        private val cryptoCoroutineScope: CoroutineScope,
        verificationActorFactory: VerificationActor.Factory,
//        private val taskExecutor: TaskExecutor,
//        private val localEchoEventFactory: LocalEchoEventFactory,
//        private val sendVerificationMessageTask: SendVerificationMessageTask,
//        private val clock: Clock,
) : VerificationService {

    val executorScope = CoroutineScope(SupervisorJob() + coroutineDispatchers.dmVerif)

//    private val eventFlow: Flow<VerificationEvent>
    private val stateMachine: VerificationActor

    init {
        stateMachine = verificationActorFactory.create(executorScope)
    }
    // It's obselete but not deprecated
    // It's ok as it will be replaced by rust implementation
//    lateinit var stateManagerActor : SendChannel<VerificationIntent>
//    val stateManagerActor = executorScope.actor {
//        val actor = verificationActorFactory.create(channel)
//        eventFlow = actor.eventFlow
//        for (msg in channel) actor.onReceive(msg)
//    }

//    private val mutex = Mutex()

    // Event received from the sync
    fun onToDeviceEvent(event: Event) {
        cryptoCoroutineScope.launch(coroutineDispatchers.dmVerif) {
            when (event.getClearType()) {
                EventType.KEY_VERIFICATION_START -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onStartRequestReceived(null, event)
                }
                EventType.KEY_VERIFICATION_CANCEL -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onCancelReceived(event)
                }
                EventType.KEY_VERIFICATION_ACCEPT -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onAcceptReceived(event)
                }
                EventType.KEY_VERIFICATION_KEY -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onKeyReceived(event)
                }
                EventType.KEY_VERIFICATION_MAC -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onMacReceived(event)
                }
                EventType.KEY_VERIFICATION_READY -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onReadyReceived(event)
                }
                EventType.KEY_VERIFICATION_DONE -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onDoneReceived(event)
                }
                MessageType.MSGTYPE_VERIFICATION_REQUEST -> {
                    Timber.v("## SAS onToDeviceEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
                    onRequestReceived(event)
                }
                else -> {
                    // ignore
                }
            }
        }
    }

    fun onRoomEvent(roomId: String, event: Event) {
        Timber.v("## SAS onRoomEvent ${event.getClearType()} from ${event.senderId?.take(10)}")
        cryptoCoroutineScope.launch(coroutineDispatchers.dmVerif) {
            when (event.getClearType()) {
                EventType.KEY_VERIFICATION_START -> {
                    onRoomStartRequestReceived(roomId, event)
                }
                EventType.KEY_VERIFICATION_CANCEL -> {
                    // MultiSessions | ignore events if i didn't sent the start from this device, or accepted from this device
                    onRoomCancelReceived(roomId, event)
                }
                EventType.KEY_VERIFICATION_ACCEPT -> {
                    onRoomAcceptReceived(roomId, event)
                }
                EventType.KEY_VERIFICATION_KEY -> {
                    onRoomKeyRequestReceived(roomId, event)
                }
                EventType.KEY_VERIFICATION_MAC -> {
                    onRoomMacReceived(roomId, event)
                }
                EventType.KEY_VERIFICATION_READY -> {
                    onRoomReadyReceived(roomId, event)
                }
                EventType.KEY_VERIFICATION_DONE -> {
                    onRoomDoneReceived(roomId, event)
                }
//                EventType.MESSAGE -> {
//                    if (MessageType.MSGTYPE_VERIFICATION_REQUEST == event.getClearContent().toModel<MessageContent>()?.msgType) {
//                        onRoomRequestReceived(roomId, event)
//                    }
//                }
                else -> {
                    // ignore
                }
            }
        }
    }

    override fun requestEventFlow(): Flow<VerificationEvent> {
        return stateMachine.eventFlow
    }
//    private var listeners = ArrayList<VerificationService.Listener>()
//
//    override fun addListener(listener: VerificationService.Listener) {
//        if (!listeners.contains(listener)) {
//            listeners.add(listener)
//        }
//    }
//
//    override fun removeListener(listener: VerificationService.Listener) {
//        listeners.remove(listener)
//    }

//    private suspend fun dispatchTxAdded(tx: VerificationTransaction) {
//        listeners.forEach {
//            try {
//                it.transactionCreated(tx)
//            } catch (e: Throwable) {
//                Timber.e(e, "## Error while notifying listeners")
//            }
//        }
//    }
//
//    private suspend fun dispatchTxUpdated(tx: VerificationTransaction) {
//        listeners.forEach {
//            try {
//                it.transactionUpdated(tx)
//            } catch (e: Throwable) {
//                Timber.e(e, "## Error while notifying listeners for tx:${tx.state}")
//            }
//        }
//    }
//
//    private suspend fun dispatchRequestAdded(tx: PendingVerificationRequest) {
//        Timber.v("## SAS dispatchRequestAdded txId:${tx.transactionId}")
//        listeners.forEach {
//            try {
//                it.verificationRequestCreated(tx)
//            } catch (e: Throwable) {
//                Timber.e(e, "## Error while notifying listeners")
//            }
//        }
//    }
//
//    private suspend fun dispatchRequestUpdated(tx: PendingVerificationRequest) {
//        listeners.forEach {
//            try {
//                it.verificationRequestUpdated(tx)
//            } catch (e: Throwable) {
//                Timber.e(e, "## Error while notifying listeners")
//            }
//        }
//    }

    override suspend fun markedLocallyAsManuallyVerified(userId: String, deviceID: String) {
        setDeviceVerificationAction.handle(
                DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true),
                userId,
                deviceID
        )

        // TODO
//        listeners.forEach {
//            try {
//                it.markedAsManuallyVerified(userId, deviceID)
//            } catch (e: Throwable) {
//                Timber.e(e, "## Error while notifying listeners")
//            }
//        }
    }

//    override suspend fun sasCodeMatch(theyMatch: Boolean, transactionId: String) {
//        val deferred = CompletableDeferred<Unit>()
//        stateMachine.send(
//                if (theyMatch) {
//                    VerificationIntent.ActionSASCodeMatches(
//                            transactionId,
//                            deferred,
//                    )
//                } else {
//                    VerificationIntent.ActionSASCodeDoesNotMatch(
//                            transactionId,
//                            deferred,
//                    )
//                }
//        )
//        deferred.await()
//    }

    suspend fun onRoomReadyFromOneOfMyOtherDevice(event: Event) {
        val requestInfo = event.content.toModel<MessageRelationContent>()
                ?: return

        stateMachine.send(
                VerificationIntent.OnReadyByAnotherOfMySessionReceived(
                        transactionId = requestInfo.relatesTo?.eventId.orEmpty(),
                        fromUser = event.senderId.orEmpty(),
                        viaRoom = event.roomId

                )
        )
//        val requestId = requestInfo.relatesTo?.eventId ?: return
//        getExistingVerificationRequestInRoom(event.roomId.orEmpty(), requestId)?.let {
//            stateMachine.send(
//                    VerificationIntent.UpdateRequest(
//                            it.copy(handledByOtherSession = true)
//                    )
//            )
//        }
    }

    private suspend fun onRequestReceived(event: Event) {
        val validRequestInfo = event.getClearContent().toModel<KeyVerificationRequest>()?.asValidObject()

        if (validRequestInfo == null) {
            // ignore
            Timber.e("## SAS Received invalid key request")
            return
        }
        val senderId = event.senderId ?: return

        val otherDeviceId = validRequestInfo.fromDevice
        Timber.v("## SAS onRequestReceived from $senderId and device $otherDeviceId, txId:${validRequestInfo.transactionId}")

        val deferred = CompletableDeferred<PendingVerificationRequest>()
        stateMachine.send(
                VerificationIntent.OnVerificationRequestReceived(
                        senderId = senderId,
                        roomId = null,
                        timeStamp = event.originServerTs,
                        validRequestInfo = validRequestInfo,
                )
        )
        deferred.await()
        checkKeysAreDownloaded(senderId)
    }

    suspend fun onRoomRequestReceived(roomId: String, event: Event) {
        Timber.v("## SAS Verification request from ${event.senderId} in room ${event.roomId}")
        val requestInfo = event.getClearContent().toModel<MessageVerificationRequestContent>() ?: return
        val validRequestInfo = requestInfo
                // copy the EventId to the transactionId
                .copy(transactionId = event.eventId)
                .asValidObject() ?: return

        val senderId = event.senderId ?: return

        if (requestInfo.toUserId != userId) {
            // I should ignore this, it's not for me
            Timber.w("## SAS Verification ignoring request from ${event.senderId}, not sent to me")
            return
        }

        stateMachine.send(
                VerificationIntent.OnVerificationRequestReceived(
                        senderId = senderId,
                        roomId = roomId,
                        timeStamp = event.originServerTs,
                        validRequestInfo = validRequestInfo,
                )
        )

        // force download keys to ensure we are up to date
        checkKeysAreDownloaded(senderId)
//        // Remember this request
//        val requestsForUser = pendingRequests.getOrPut(senderId) { mutableListOf() }
//
//        val pendingVerificationRequest = PendingVerificationRequest(
//                ageLocalTs = event.ageLocalTs ?: clock.epochMillis(),
//                isIncoming = true,
//                otherUserId = senderId, // requestInfo.toUserId,
//                roomId = event.roomId,
//                transactionId = event.eventId,
//                localId = event.eventId!!,
//                requestInfo = validRequestInfo
//        )
//        requestsForUser.add(pendingVerificationRequest)
//        dispatchRequestAdded(pendingVerificationRequest)

        /*
         * After the m.key.verification.ready event is sent, either party can send an m.key.verification.start event
         * to begin the verification.
         * If both parties send an m.key.verification.start event, and they both specify the same verification method,
         * then the event sent by the user whose user ID is the smallest is used, and the other m.key.verification.start
         * event is ignored.
         * In the case of a single user verifying two of their devices, the device ID is compared instead.
         * If both parties send an m.key.verification.start event, but they specify different verification methods,
         * the verification should be cancelled with a code of m.unexpected_message.
         */
    }

    override suspend fun onPotentiallyInterestingEventRoomFailToDecrypt(event: Event) {
        // When Should/Can we cancel??
        val relationContent = event.content.toModel<EncryptedEventContent>()?.relatesTo
        if (relationContent?.type == RelationType.REFERENCE) {
            val relatedId = relationContent.eventId ?: return
            val sender = event.senderId ?: return
            val roomId = event.roomId ?: return
            stateMachine.send(
                    VerificationIntent.OnUnableToDecryptVerificationEvent(
                            fromUser = sender,
                            roomId = roomId,
                            transactionId = relatedId
                    )
            )
//            // at least if request was sent by me, I can safely cancel without interfering
//            pendingRequests[event.senderId]?.firstOrNull {
//                it.transactionId == relatedId && !it.isIncoming
//            }?.let { pr ->
//                verificationTransportRoomMessageFactory.createTransport(event.roomId ?: "", null)
//                        .cancelTransaction(
//                                relatedId,
//                                event.senderId ?: "",
//                                event.getSenderKey() ?: "",
//                                CancelCode.InvalidMessage
//                        )
//                updatePendingRequest(pr.copy(cancelConclusion = CancelCode.InvalidMessage))
//            }
        }
    }

    private suspend fun onRoomStartRequestReceived(roomId: String, event: Event) {
        val startReq = event.getClearContent().toModel<MessageVerificationStartContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )

        val validStartReq = startReq?.asValidObject() ?: return

        stateMachine.send(
                VerificationIntent.OnStartReceived(
                        fromUser = event.senderId.orEmpty(),
                        viaRoom = roomId,
                        validVerificationInfoStart = validStartReq,
                )
        )
    }

    private suspend fun onStartRequestReceived(roomId: String? = null, event: Event) {
        Timber.e("## SAS received Start request ${event.eventId}")
        val startReq = event.getClearContent().toModel<KeyVerificationStart>()
        val validStartReq = startReq?.asValidObject() ?: return
        Timber.v("## SAS received Start request $startReq")

        val otherUserId = event.senderId ?: return
        stateMachine.send(
                VerificationIntent.OnStartReceived(
                        fromUser = otherUserId,
                        viaRoom = roomId,
                        validVerificationInfoStart = validStartReq
                )
        )
//        if (validStartReq == null) {
//            Timber.e("## SAS received invalid verification request")
//            if (startReq?.transactionId != null) {
//                verificationTransportToDeviceFactory.createTransport(null).cancelTransaction(
//                        startReq.transactionId,
//                        otherUserId,
//                        startReq.fromDevice ?: event.getSenderKey()!!,
//                        CancelCode.UnknownMethod
//                )
//            }
//            return
//        }
//        // Download device keys prior to everything
//        handleStart(otherUserId, validStartReq) {
//            it.transport = verificationTransportToDeviceFactory.createTransport(it)
//        }?.let {
//            verificationTransportToDeviceFactory.createTransport(null).cancelTransaction(
//                    validStartReq.transactionId,
//                    otherUserId,
//                    validStartReq.fromDevice,
//                    it
//            )
//        }
    }

    /**
     * Return a CancelCode to make the caller cancel the verification. Else return null
     */
//    private suspend fun handleStart(
//            otherUserId: String?,
//            startReq: ValidVerificationInfoStart,
//            txConfigure: (DefaultVerificationTransaction) -> Unit
//    ): CancelCode? {
//        Timber.d("## SAS onStartRequestReceived $startReq")
//        otherUserId ?: return null // just ignore
// //        if (otherUserId?.let { checkKeysAreDownloaded(it, startReq.fromDevice) } != null) {
//        val tid = startReq.transactionId
//        var existing = getExistingTransaction(otherUserId, tid)
//
//        // After the m.key.verification.ready event is sent, either party can send an
//        // m.key.verification.start event to begin the verification. If both parties
//        // send an m.key.verification.start event, and they both specify the same
//        // verification method, then the event sent by the user whose user ID is the
//        // smallest is used, and the other m.key.verification.start event is ignored.
//        // In the case of a single user verifying two of their devices, the device ID is
//        // compared instead .
//        if (existing is DefaultOutgoingSASDefaultVerificationTransaction) {
//            val readyRequest = getExistingVerificationRequest(otherUserId, tid)
//            if (readyRequest?.isReady == true) {
//                if (isOtherPrioritary(otherUserId, existing.otherDeviceId ?: "")) {
//                    Timber.d("## SAS concurrent start isOtherPrioritary, clear")
//                    // The other is prioritary!
//                    // I should replace my outgoing with an incoming
//                    removeTransaction(otherUserId, tid)
//                    existing = null
//                } else {
//                    Timber.d("## SAS concurrent start i am prioritary, ignore")
//                    // i am prioritary, ignore this start event!
//                    return null
//                }
//            }
//        }
//
//        when (startReq) {
//            is ValidVerificationInfoStart.SasVerificationInfoStart -> {
//                when (existing) {
//                    is SasVerificationTransaction -> {
//                        // should cancel both!
//                        Timber.v("## SAS onStartRequestReceived - Request exist with same id ${startReq.transactionId}")
//                        existing.cancel(CancelCode.UnexpectedMessage)
//                        // Already cancelled, so return null
//                        return null
//                    }
//                    is QrCodeVerificationTransaction -> {
//                        // Nothing to do?
//                    }
//                    null -> {
//                        getExistingTransactionsForUser(otherUserId)
//                                ?.filterIsInstance(SasVerificationTransaction::class.java)
//                                ?.takeIf { it.isNotEmpty() }
//                                ?.also {
//                                    // Multiple keyshares between two devices:
//                                    // any two devices may only have at most one key verification in flight at a time.
//                                    Timber.v("## SAS onStartRequestReceived - Already a transaction with this user ${startReq.transactionId}")
//                                }
//                                ?.forEach {
//                                    it.cancel(CancelCode.UnexpectedMessage)
//                                }
//                                ?.also {
//                                    return CancelCode.UnexpectedMessage
//                                }
//                    }
//                }
//
//                // Ok we can create a SAS transaction
//                Timber.v("## SAS onStartRequestReceived - request accepted ${startReq.transactionId}")
//                // If there is a corresponding request, we can auto accept
//                // as we are the one requesting in first place (or we accepted the request)
//                // I need to check if the pending request was related to this device also
//                val autoAccept = getExistingVerificationRequests(otherUserId).any {
//                    it.transactionId == startReq.transactionId &&
//                            (it.requestInfo?.fromDevice == this.deviceId || it.readyInfo?.fromDevice == this.deviceId)
//                }
//                val tx = DefaultIncomingSASDefaultVerificationTransaction(
// //                            this,
//                        setDeviceVerificationAction,
//                        userId,
//                        deviceId,
//                        cryptoStore,
//                        crossSigningService,
//                        outgoingKeyRequestManager,
//                        secretShareManager,
//                        myDeviceInfoHolder.get().myDevice.fingerprint()!!,
//                        startReq.transactionId,
//                        otherUserId,
//                        autoAccept
//                ).also { txConfigure(it) }
//                addTransaction(tx)
//                tx.onVerificationStart(startReq)
//                return null
//            }
//            is ValidVerificationInfoStart.ReciprocateVerificationInfoStart -> {
//                // Other user has scanned my QR code
//                if (existing is DefaultQrCodeVerificationTransaction) {
//                    existing.onStartReceived(startReq)
//                    return null
//                } else {
//                    Timber.w("## SAS onStartRequestReceived - unexpected message ${startReq.transactionId} / $existing")
//                    return CancelCode.UnexpectedMessage
//                }
//            }
//        }
// //        } else {
// //            return CancelCode.UnexpectedMessage
// //        }
//    }

    private fun isOtherPrioritary(otherUserId: String, otherDeviceId: String): Boolean {
        if (userId < otherUserId) {
            return false
        } else if (userId > otherUserId) {
            return true
        } else {
            return otherDeviceId < myDeviceId ?: ""
        }
    }

    private suspend fun checkKeysAreDownloaded(
            otherUserId: String,
    ): Boolean {
        return try {
            deviceListManager.downloadKeys(listOf(otherUserId), false)
                    .getUserDeviceIds(otherUserId)
                    ?.contains(userId)
                    ?: deviceListManager.downloadKeys(listOf(otherUserId), true)
                            .getUserDeviceIds(otherUserId)
                            ?.contains(userId)
                    ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun onRoomCancelReceived(roomId: String, event: Event) {
        val cancelReq = event.getClearContent().toModel<MessageVerificationCancelContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )

        val validCancelReq = cancelReq?.asValidObject() ?: return
        event.senderId ?: return
        stateMachine.send(
                VerificationIntent.OnCancelReceived(
                        viaRoom = roomId,
                        fromUser = event.senderId,
                        validCancel = validCancelReq
                )
        )

//        if (validCancelReq == null) {
//            // ignore
//            Timber.e("## SAS Received invalid cancel request")
//            // TODO should we cancel?
//            return
//        }
//        getExistingVerificationRequest(event.senderId ?: "", validCancelReq.transactionId)?.let {
//            updatePendingRequest(it.copy(cancelConclusion = safeValueOf(validCancelReq.code)))
//        }
//        handleOnCancel(event.senderId!!, validCancelReq)
    }

    private suspend fun onCancelReceived(event: Event) {
        Timber.v("## SAS onCancelReceived")
        val cancelReq = event.getClearContent().toModel<KeyVerificationCancel>()?.asValidObject()
                ?: return

        event.senderId ?: return
        stateMachine.send(
                VerificationIntent.OnCancelReceived(
                        viaRoom = null,
                        fromUser = event.senderId,
                        validCancel = cancelReq
                )
        )
    }

//    private fun handleOnCancel(otherUserId: String, cancelReq: ValidVerificationInfoCancel) {
//        Timber.v("## SAS onCancelReceived otherUser: $otherUserId reason: ${cancelReq.reason}")
//
//        val existingTransaction = getExistingTransaction(otherUserId, cancelReq.transactionId)
//        val existingRequest = getExistingVerificationRequest(otherUserId, cancelReq.transactionId)
//
//        if (existingRequest != null) {
//            // Mark this request as cancelled
//            updatePendingRequest(
//                    existingRequest.copy(
//                            cancelConclusion = safeValueOf(cancelReq.code)
//                    )
//            )
//        }
//
//        existingTransaction?.state = VerificationTxState.Cancelled(safeValueOf(cancelReq.code), false)
//    }

    private suspend fun onRoomAcceptReceived(roomId: String, event: Event) {
        Timber.d("##  SAS Received Accept via DM $event")
        val accept = event.getClearContent().toModel<MessageVerificationAcceptContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
                ?: return

        val validAccept = accept.asValidObject() ?: return

        handleAccept(roomId, validAccept, event.senderId!!)
    }

    private suspend fun onAcceptReceived(event: Event) {
        Timber.d("##  SAS Received Accept $event")
        val acceptReq = event.getClearContent().toModel<KeyVerificationAccept>()?.asValidObject() ?: return
        handleAccept(null, acceptReq, event.senderId!!)
    }

    private suspend fun handleAccept(roomId: String?, acceptReq: ValidVerificationInfoAccept, senderId: String) {
        stateMachine.send(
                VerificationIntent.OnAcceptReceived(
                        viaRoom = roomId,
                        validAccept = acceptReq,
                        fromUser = senderId
                )
        )
    }

    private suspend fun onRoomKeyRequestReceived(roomId: String, event: Event) {
        val keyReq = event.getClearContent().toModel<MessageVerificationKeyContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
                ?.asValidObject()
        if (keyReq == null) {
            // ignore
            Timber.e("## SAS Received invalid key request")
            // TODO should we cancel?
            return
        }
        handleKeyReceived(roomId, event, keyReq)
    }

    private suspend fun onKeyReceived(event: Event) {
        val keyReq = event.getClearContent().toModel<KeyVerificationKey>()?.asValidObject()

        if (keyReq == null) {
            // ignore
            Timber.e("## SAS Received invalid key request")
            return
        }
        handleKeyReceived(null, event, keyReq)
    }

    private suspend fun handleKeyReceived(roomId: String?, event: Event, keyReq: ValidVerificationInfoKey) {
        Timber.d("##  SAS Received Key from ${event.senderId} with info $keyReq")
        val otherUserId = event.senderId ?: return
        stateMachine.send(
                VerificationIntent.OnKeyReceived(
                        roomId,
                        otherUserId,
                        keyReq
                )
        )
    }

    private suspend fun onRoomMacReceived(roomId: String, event: Event) {
        val macReq = event.getClearContent().toModel<MessageVerificationMacContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
                ?.asValidObject()
        if (macReq == null || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid mac request")
            // TODO should we cancel?
            return
        }
        stateMachine.send(
                VerificationIntent.OnMacReceived(
                        viaRoom = roomId,
                        fromUser = event.senderId,
                        validMac = macReq
                )
        )
    }

    private suspend fun onRoomReadyReceived(roomId: String, event: Event) {
        val readyReq = event.getClearContent().toModel<MessageVerificationReadyContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
                ?.asValidObject()

        if (readyReq == null || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid room ready request $readyReq senderId=${event.senderId}")
            Timber.e("## SAS Received invalid room ready content=${event.getClearContent()}")
            Timber.e("## SAS Received invalid room ready content=${event}")
            // TODO should we cancel?
            return
        }
        stateMachine.send(
                VerificationIntent.OnReadyReceived(
                        transactionId = readyReq.transactionId,
                        fromUser = event.senderId,
                        viaRoom = roomId,
                        readyInfo = readyReq
                )
        )
        // if it's a ready send by one of my other device I should stop handling in it on my side.
//        if (event.senderId == userId && readyReq.fromDevice != deviceId) {
//            getExistingVerificationRequestInRoom(roomId, readyReq.transactionId)?.let {
//                updatePendingRequest(
//                        it.copy(
//                                handledByOtherSession = true
//                        )
//                )
//            }
//            return
//        }
//
//        handleReadyReceived(event.senderId, readyReq) {
//            verificationTransportRoomMessageFactory.createTransport(roomId, it)
//        }
    }

    private suspend fun onReadyReceived(event: Event) {
        val readyReq = event.getClearContent().toModel<KeyVerificationReady>()?.asValidObject()
        Timber.v("## SAS onReadyReceived $readyReq")

        if (readyReq == null || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid ready request $readyReq senderId=${event.senderId}")
            Timber.e("## SAS Received invalid ready content=${event.getClearContent()}")
            // TODO should we cancel?
            return
        }

        stateMachine.send(
                VerificationIntent.OnReadyReceived(
                        transactionId = readyReq.transactionId,
                        fromUser = event.senderId,
                        viaRoom = null,
                        readyInfo = readyReq
                )
        )
//        if (checkKeysAreDownloaded(event.senderId, readyReq.fromDevice) == null) {
//            Timber.e("## SAS Verification device ${readyReq.fromDevice} is not known")
//            // TODO cancel?
//            return
//        }
//
//        handleReadyReceived(event.senderId, readyReq) {
//            verificationTransportToDeviceFactory.createTransport(it)
//        }
    }

    private suspend fun onDoneReceived(event: Event) {
        Timber.v("## onDoneReceived")
        val doneReq = event.getClearContent().toModel<KeyVerificationDone>()?.asValidObject()
        if (doneReq == null || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid done request ${doneReq}")
            return
        }
        stateMachine.send(
                VerificationIntent.OnDoneReceived(
                        transactionId = doneReq.transactionId,
                        fromUser = event.senderId,
                        viaRoom = null,
                )
        )

//        handleDoneReceived(event.senderId, doneReq)
//
//        if (event.senderId == userId) {
//            // We only send gossiping request when the other sent us a done
//            // We can ask without checking too much thinks (like trust), because we will check validity of secret on reception
//            getExistingTransaction(userId, doneReq.transactionId)
//                    ?: getOldTransaction(userId, doneReq.transactionId)
//                            ?.let { vt ->
//                                val otherDeviceId = vt.otherDeviceId ?: return@let
//                                if (!crossSigningService.canCrossSign()) {
//                                    cryptoCoroutineScope.launch {
//                                        secretShareManager.requestSecretTo(otherDeviceId, MASTER_KEY_SSSS_NAME)
//                                        secretShareManager.requestSecretTo(otherDeviceId, SELF_SIGNING_KEY_SSSS_NAME)
//                                        secretShareManager.requestSecretTo(otherDeviceId, USER_SIGNING_KEY_SSSS_NAME)
//                                        secretShareManager.requestSecretTo(otherDeviceId, KEYBACKUP_SECRET_SSSS_NAME)
//                                    }
//                                }
//                            }
//        }
    }

//    private suspend fun handleDoneReceived(senderId: String, doneReq: ValidVerificationDone) {
//        Timber.v("## SAS Done received $doneReq")
//        val existing = getExistingTransaction(senderId, doneReq.transactionId)
//        if (existing == null) {
//            Timber.e("## SAS Received Invalid done unknown request:${doneReq.transactionId} ")
//            return
//        }
//        if (existing is DefaultQrCodeVerificationTransaction) {
//            existing.onDoneReceived()
//        } else {
//            // SAS do not care for now?
//        }
//
//        // Now transactions are updated, let's also update Requests
//        val existingRequest = getExistingVerificationRequests(senderId).find { it.transactionId == doneReq.transactionId }
//        if (existingRequest == null) {
//            Timber.e("## SAS Received Done for unknown request txId:${doneReq.transactionId}")
//            return
//        }
//        updatePendingRequest(existingRequest.copy(isSuccessful = true))
//    }

    private suspend fun onRoomDoneReceived(roomId: String, event: Event) {
        val doneReq = event.getClearContent().toModel<MessageVerificationDoneContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
                ?.asValidObject()

        if (doneReq == null || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid Done request ${doneReq}")
            // TODO should we cancel?
            return
        }

        stateMachine.send(
                VerificationIntent.OnDoneReceived(
                        transactionId = doneReq.transactionId,
                        fromUser = event.senderId,
                        viaRoom = roomId,
                )
        )
    }

    private suspend fun onMacReceived(event: Event) {
        val macReq = event.getClearContent().toModel<KeyVerificationMac>()?.asValidObject()

        if (macReq == null || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid mac request")
            return
        }
        stateMachine.send(
                VerificationIntent.OnMacReceived(
                        viaRoom = null,
                        fromUser = event.senderId,
                        validMac = macReq
                )
        )
    }
//
//    private suspend fun handleMacReceived(senderId: String, macReq: ValidVerificationInfoMac) {
//        Timber.v("## SAS Received $macReq")
//        val existing = getExistingTransaction(senderId, macReq.transactionId)
//        if (existing == null) {
//            Timber.e("## SAS Received Mac for unknown transaction ${macReq.transactionId}")
//            return
//        }
//        if (existing is SASDefaultVerificationTransaction) {
//            existing.onKeyVerificationMac(macReq)
//        } else {
//            // not other types known for now
//        }
//    }

//    private suspend fun handleReadyReceived(
//            senderId: String,
//            readyReq: ValidVerificationInfoReady,
//            transportCreator: (DefaultVerificationTransaction) -> VerificationTransport
//    ) {
//        val existingRequest = getExistingVerificationRequests(senderId).find { it.transactionId == readyReq.transactionId }
//        if (existingRequest == null) {
//            Timber.e("## SAS Received Ready for unknown request txId:${readyReq.transactionId} fromDevice ${readyReq.fromDevice}")
//            return
//        }
//
//        val qrCodeData = readyReq.methods
//                // Check if other user is able to scan QR code
//                .takeIf { it.contains(VERIFICATION_METHOD_QR_CODE_SCAN) }
//                ?.let {
//                    createQrCodeData(existingRequest.transactionId, existingRequest.otherUserId, readyReq.fromDevice)
//                }
//
//        if (readyReq.methods.contains(VERIFICATION_METHOD_RECIPROCATE)) {
//            // Create the pending transaction
//            val tx = DefaultQrCodeVerificationTransaction(
//                    setDeviceVerificationAction = setDeviceVerificationAction,
//                    transactionId = readyReq.transactionId,
//                    otherUserId = senderId,
//                    otherDeviceId = readyReq.fromDevice,
//                    crossSigningService = crossSigningService,
//                    outgoingKeyRequestManager = outgoingKeyRequestManager,
//                    secretShareManager = secretShareManager,
//                    cryptoStore = cryptoStore,
//                    qrCodeData = qrCodeData,
//                    userId = userId,
//                    deviceId = deviceId ?: "",
//                    isIncoming = false
//            )
//
//            tx.transport = transportCreator.invoke(tx)
//
//            addTransaction(tx)
//        }
//
//        updatePendingRequest(
//                existingRequest.copy(
//                        readyInfo = readyReq
//                )
//        )
//
//        // if it's a to_device verification request, we need to notify others that the
//        // request was accepted by this one
//        if (existingRequest.roomId == null) {
//            notifyOthersOfAcceptance(existingRequest, readyReq.fromDevice)
//        }
//    }

    /**
     * Gets a list of device ids excluding the current one.
     */
//    private fun getMyOtherDeviceIds(): List<String> = cryptoStore.getUserDevices(userId)?.keys?.filter { it != deviceId }.orEmpty()

    /**
     * Notifies other devices that the current verification request is being handled by [acceptedByDeviceId].
     */
//    private fun notifyOthersOfAcceptance(request: PendingVerificationRequest, acceptedByDeviceId: String) {
//        val otherUserId = request.otherUserId
//        // this user should be me, as we use to device verification only for self verification
//        // but the spec is not that restrictive
//        val deviceIds = cryptoStore.getUserDevices(otherUserId)?.keys
//                ?.filter { it != acceptedByDeviceId }
//                // if it's me we don't want to send self cancel
//                ?.filter { it != deviceId }
//                .orEmpty()
//
//        val transport = verificationTransportToDeviceFactory.createTransport(null)
//        transport.cancelTransaction(
//                request.transactionId.orEmpty(),
//                otherUserId,
//                deviceIds,
//                CancelCode.AcceptedByAnotherDevice
//        )
//    }

//    private suspend fun createQrCodeData(requestId: String, otherUserId: String, otherDeviceId: String?): QrCodeData? {
// //        requestId ?: run {
// //            Timber.w("## Unknown requestId")
// //            return null
// //        }
//
//        return when {
//            userId != otherUserId ->
//                createQrCodeDataForDistinctUser(requestId, otherUserId)
//            crossSigningService.isCrossSigningVerified() ->
//                // This is a self verification and I am the old device (Osborne2)
//                createQrCodeDataForVerifiedDevice(requestId, otherDeviceId)
//            else ->
//                // This is a self verification and I am the new device (Dynabook)
//                createQrCodeDataForUnVerifiedDevice(requestId)
//        }
//    }

//    private suspend fun createQrCodeDataForDistinctUser(requestId: String, otherUserId: String): QrCodeData.VerifyingAnotherUser? {
//        val myMasterKey = crossSigningService.getMyCrossSigningKeys()
//                ?.masterKey()
//                ?.unpaddedBase64PublicKey
//                ?: run {
//                    Timber.w("## Unable to get my master key")
//                    return null
//                }
//
//        val otherUserMasterKey = crossSigningService.getUserCrossSigningKeys(otherUserId)
//                ?.masterKey()
//                ?.unpaddedBase64PublicKey
//                ?: run {
//                    Timber.w("## Unable to get other user master key")
//                    return null
//                }
//
//        return QrCodeData.VerifyingAnotherUser(
//                transactionId = requestId,
//                userMasterCrossSigningPublicKey = myMasterKey,
//                otherUserMasterCrossSigningPublicKey = otherUserMasterKey,
//                sharedSecret = generateSharedSecretV2()
//        )
//    }

    // Create a QR code to display on the old device (Osborne2)
//    private suspend fun createQrCodeDataForVerifiedDevice(requestId: String, otherDeviceId: String?): QrCodeData.SelfVerifyingMasterKeyTrusted? {
//        val myMasterKey = crossSigningService.getMyCrossSigningKeys()
//                ?.masterKey()
//                ?.unpaddedBase64PublicKey
//                ?: run {
//                    Timber.w("## Unable to get my master key")
//                    return null
//                }
//
//        val otherDeviceKey = otherDeviceId
//                ?.let {
//                    cryptoStore.getUserDevice(userId, otherDeviceId)?.fingerprint()
//                }
//                ?: run {
//                    Timber.w("## Unable to get other device data")
//                    return null
//                }
//
//        return QrCodeData.SelfVerifyingMasterKeyTrusted(
//                transactionId = requestId,
//                userMasterCrossSigningPublicKey = myMasterKey,
//                otherDeviceKey = otherDeviceKey,
//                sharedSecret = generateSharedSecretV2()
//        )
//    }

    // Create a QR code to display on the new device (Dynabook)
//    private suspend fun createQrCodeDataForUnVerifiedDevice(requestId: String): QrCodeData.SelfVerifyingMasterKeyNotTrusted? {
//        val myMasterKey = crossSigningService.getMyCrossSigningKeys()
//                ?.masterKey()
//                ?.unpaddedBase64PublicKey
//                ?: run {
//                    Timber.w("## Unable to get my master key")
//                    return null
//                }
//
//        val myDeviceKey = myDeviceInfoHolder.get().myDevice.fingerprint()
//                ?: run {
//                    Timber.w("## Unable to get my fingerprint")
//                    return null
//                }
//
//        return QrCodeData.SelfVerifyingMasterKeyNotTrusted(
//                transactionId = requestId,
//                deviceKey = myDeviceKey,
//                userMasterCrossSigningPublicKey = myMasterKey,
//                sharedSecret = generateSharedSecretV2()
//        )
//    }

//    private fun handleDoneReceived(senderId: String, doneInfo: ValidVerificationDone) {
//        val existingRequest = getExistingVerificationRequest(senderId)?.find { it.transactionId == doneInfo.transactionId }
//        if (existingRequest == null) {
//            Timber.e("## SAS Received Done for unknown request txId:${doneInfo.transactionId}")
//            return
//        }
//        updatePendingRequest(existingRequest.copy(isSuccessful = true))
//    }

    // TODO All this methods should be delegated to a TransactionStore
    override suspend fun getExistingTransaction(otherUserId: String, tid: String): VerificationTransaction? {
        val deferred = CompletableDeferred<VerificationTransaction?>()
        stateMachine.send(
                VerificationIntent.GetExistingTransaction(
                        fromUser = otherUserId,
                        transactionId = tid,
                        deferred = deferred
                )
        )
        return deferred.await()
    }

    override suspend fun getExistingVerificationRequests(otherUserId: String): List<PendingVerificationRequest> {
        val deferred = CompletableDeferred<List<PendingVerificationRequest>>()
        stateMachine.send(
                VerificationIntent.GetExistingRequestsForUser(
                        userId = otherUserId,
                        deferred = deferred
                )
        )
        return deferred.await()
    }

    override suspend fun getExistingVerificationRequest(otherUserId: String, tid: String?): PendingVerificationRequest? {
        val deferred = CompletableDeferred<PendingVerificationRequest?>()
        tid ?: return null
        stateMachine.send(
                VerificationIntent.GetExistingRequest(
                        transactionId = tid,
                        otherUserId = otherUserId,
                        deferred = deferred
                )
        )
        return deferred.await()
    }

    override suspend fun getExistingVerificationRequestInRoom(roomId: String, tid: String): PendingVerificationRequest? {
        val deferred = CompletableDeferred<PendingVerificationRequest?>()
        stateMachine.send(
                VerificationIntent.GetExistingRequestInRoom(
                        transactionId = tid, roomId = roomId,
                        deferred = deferred
                )
        )
        return deferred.await()
    }

//    private suspend fun getExistingTransactionsForUser(otherUser: String): Collection<VerificationTransaction>? {
//        mutex.withLock {
//            return txMap[otherUser]?.values
//        }
//    }

//    private suspend fun removeTransaction(otherUser: String, tid: String) {
//        mutex.withLock {
//            txMap[otherUser]?.remove(tid)?.also {
//                it.removeListener(this)
//            }
//        }?.let {
//            rememberOldTransaction(it)
//        }
//    }

//    private suspend fun addTransaction(tx: DefaultVerificationTransaction) {
//        mutex.withLock {
//            val txInnerMap = txMap.getOrPut(tx.otherUserId) { HashMap() }
//            txInnerMap[tx.transactionId] = tx
//            dispatchTxAdded(tx)
//            tx.addListener(this)
//        }
//    }

//    private suspend fun rememberOldTransaction(tx: DefaultVerificationTransaction) {
//        mutex.withLock {
//            pastTransactions.getOrPut(tx.otherUserId) { HashMap() }[tx.transactionId] = tx
//        }
//    }

//    private suspend fun getOldTransaction(userId: String, tid: String?): DefaultVerificationTransaction? {
//        return tid?.let {
//            mutex.withLock {
//                pastTransactions[userId]?.get(it)
//            }
//        }
//    }

    override suspend fun startKeyVerification(method: VerificationMethod, otherUserId: String, requestId: String): String? {
        require(method == VerificationMethod.SAS) { "Unknown verification method" }
        val deferred = CompletableDeferred<VerificationTransaction>()
        stateMachine.send(
                VerificationIntent.ActionStartSasVerification(
                        otherUserId = otherUserId,
                        requestId = requestId,
                        deferred = deferred
                )
        )
        return deferred.await().transactionId
    }

    override suspend fun reciprocateQRVerification(otherUserId: String, requestId: String, scannedData: String): String? {
        val deferred = CompletableDeferred<VerificationTransaction?>()
        stateMachine.send(
                VerificationIntent.ActionReciprocateQrVerification(
                        otherUserId = otherUserId,
                        requestId = requestId,
                        scannedData = scannedData,
                        deferred = deferred
                )
        )
        return deferred.await()?.transactionId
    }

    override suspend fun requestKeyVerificationInDMs(
            methods: List<VerificationMethod>,
            otherUserId: String,
            roomId: String,
            localId: String?
    ): PendingVerificationRequest {
        Timber.i("## SAS Requesting verification to user: $otherUserId in room $roomId")

        checkKeysAreDownloaded(otherUserId)

//        val requestsForUser = pendingRequests.getOrPut(otherUserId) { mutableListOf() }

//        val transport = verificationTransportRoomMessageFactory.createTransport(roomId)

        val deferred = CompletableDeferred<PendingVerificationRequest>()
        stateMachine.send(
                VerificationIntent.ActionRequestVerification(
                        roomId = roomId,
                        otherUserId = otherUserId,
                        methods = methods,
                        deferred = deferred
                )
        )

        return deferred.await()
//        result.toCancel.forEach {
//            try {
//                transport.cancelTransaction(it.transactionId.orEmpty(), it.otherUserId, "", CancelCode.User)
//            } catch (failure: Throwable) {
//                // continue anyhow
//            }
//        }
//        val verificationRequest = result.request
//
//        val requestInfo = verificationRequest.requestInfo
//        try {
//            val sentRequest = transport.sendVerificationRequest(requestInfo.methods, verificationRequest.localId, otherUserId, roomId, null)
//            // We need to update with the syncedID
//            val updatedRequest = verificationRequest.copy(
//                    transactionId = sentRequest.transactionId,
//                    // localId stays different
//                    requestInfo = sentRequest
//            )
//            updatePendingRequest(updatedRequest)
//            return updatedRequest
//        } catch (failure: Throwable) {
//            Timber.i("## Failed to send request $verificationRequest")
//            stateManagerActor.send(
//                    VerificationIntent.FailToSendRequest(verificationRequest)
//            )
//            throw failure
//        }
    }

    override suspend fun requestSelfKeyVerification(methods: List<VerificationMethod>): PendingVerificationRequest {
        return requestDeviceVerification(methods, userId, null)
    }

    override suspend fun requestDeviceVerification(methods: List<VerificationMethod>, otherUserId: String, otherDeviceId: String?): PendingVerificationRequest {
        // TODO refactor this with the DM one

        val targetDevices = otherDeviceId?.let { listOf(it) }
                ?: cryptoStore.getUserDevices(otherUserId)
                        ?.filter { it.key != myDeviceId }
                        ?.values?.map { it.deviceId }.orEmpty()

        Timber.i("## Requesting verification to user: $otherUserId with device list $targetDevices")

//        val transport = verificationTransportToDeviceFactory.createTransport(otherUserId, otherDeviceId)

        val deferred = CompletableDeferred<PendingVerificationRequest>()
        stateMachine.send(
                VerificationIntent.ActionRequestVerification(
                        roomId = null,
                        otherUserId = otherUserId,
                        targetDevices = targetDevices,
                        methods = methods,
                        deferred = deferred
                )
        )

        return deferred.await()
//        result.toCancel.forEach {
//            try {
//                transport.cancelTransaction(it.transactionId.orEmpty(), it.otherUserId, "", CancelCode.User)
//            } catch (failure: Throwable) {
//                // continue anyhow
//            }
//        }
//        val verificationRequest = result.request
//
//        val requestInfo = verificationRequest.requestInfo
//        try {
//            val sentRequest = transport.sendVerificationRequest(requestInfo.methods, verificationRequest.localId, otherUserId, null, targetDevices)
//            // We need to update with the syncedID
//            val updatedRequest = verificationRequest.copy(
//                    transactionId = sentRequest.transactionId,
//                    // localId stays different
//                    requestInfo = sentRequest
//            )
//            updatePendingRequest(updatedRequest)
//            return updatedRequest
//        } catch (failure: Throwable) {
//            Timber.i("## Failed to send request $verificationRequest")
//            stateManagerActor.send(
//                    VerificationIntent.FailToSendRequest(verificationRequest)
//            )
//            throw failure
//        }

//        // Cancel existing pending requests?
//        requestsForUser.toList().forEach { existingRequest ->
//            existingRequest.transactionId?.let { tid ->
//                if (!existingRequest.isFinished) {
//                    Timber.d("## SAS, cancelling pending requests to start a new one")
//                    updatePendingRequest(existingRequest.copy(cancelConclusion = CancelCode.User))
//                    existingRequest.targetDevices?.forEach {
//                        transport.cancelTransaction(tid, existingRequest.otherUserId, it, CancelCode.User)
//                    }
//                }
//            }
//        }
//
//        val localId = LocalEcho.createLocalEchoId()
//
//        val verificationRequest = PendingVerificationRequest(
//                transactionId = localId,
//                ageLocalTs = clock.epochMillis(),
//                isIncoming = false,
//                roomId = null,
//                localId = localId,
//                otherUserId = otherUserId,
//                targetDevices = targetDevices
//        )
//
//        // We can SCAN or SHOW QR codes only if cross-signing is enabled
//        val methodValues = if (crossSigningService.isCrossSigningInitialized()) {
//            // Add reciprocate method if application declares it can scan or show QR codes
//            // Not sure if it ok to do that (?)
//            val reciprocateMethod = methods
//                    .firstOrNull { it == VerificationMethod.QR_CODE_SCAN || it == VerificationMethod.QR_CODE_SHOW }
//                    ?.let { listOf(VERIFICATION_METHOD_RECIPROCATE) }.orEmpty()
//            methods.map { it.toValue() } + reciprocateMethod
//        } else {
//            // Filter out SCAN and SHOW qr code method
//            methods
//                    .filter { it != VerificationMethod.QR_CODE_SHOW && it != VerificationMethod.QR_CODE_SCAN }
//                    .map { it.toValue() }
//        }
//                .distinct()
//
//        dispatchRequestAdded(verificationRequest)
//        val info = transport.sendVerificationRequest(methodValues, localId, otherUserId, null, targetDevices)
//        // Nothing special to do in to device mode
//        updatePendingRequest(
//                verificationRequest.copy(
//                        // localId stays different
//                        requestInfo = info
//                )
//        )
//
//        requestsForUser.add(verificationRequest)
//
//        return verificationRequest
    }

    override suspend fun cancelVerificationRequest(request: PendingVerificationRequest) {
        val deferred = CompletableDeferred<Unit>()
        stateMachine.send(
                VerificationIntent.ActionCancel(
                        transactionId = request.transactionId,
                        deferred
                )
        )
        deferred.await()
//        if (request.roomId != null) {
//            val transport = verificationTransportRoomMessageFactory.createTransport(request.roomId)
//            transport.cancelTransaction(request.transactionId ?: "", request.otherUserId, null, CancelCode.User)
//        } else {
//            // TODO is there a difference between incoming/outgoing?
//            val transport = verificationTransportToDeviceFactory.createTransport(request.otherUserId, null)
//            request.targetDevices?.forEach { deviceId ->
//                transport.cancelTransaction(request.transactionId ?: "", request.otherUserId, deviceId, CancelCode.User)
//            }
//        }
    }

    override suspend fun cancelVerificationRequest(otherUserId: String, transactionId: String) {
        getExistingVerificationRequest(otherUserId, transactionId)?.let {
            cancelVerificationRequest(it)
        }
    }

    override suspend fun declineVerificationRequestInDMs(otherUserId: String, transactionId: String, roomId: String) {
        val deferred = CompletableDeferred<Unit>()
        stateMachine.send(
                VerificationIntent.ActionCancel(
                        transactionId,
                        deferred
                )
        )
        deferred.await()
//        verificationTransportRoomMessageFactory.createTransport(roomId, null)
//                .cancelTransaction(transactionId, otherUserId, null, CancelCode.User)
//
//        getExistingVerificationRequest(otherUserId, transactionId)?.let {
//            updatePendingRequest(
//                    it.copy(
//                            cancelConclusion = CancelCode.User
//                    )
//            )
//        }
    }

//    private suspend fun updatePendingRequest(updated: PendingVerificationRequest) {
//        stateManagerActor.send(
//                VerificationIntent.UpdateRequest(updated)
//        )
//    }

//    override fun beginKeyVerificationInDMs(
//            method: VerificationMethod,
//            transactionId: String,
//            roomId: String,
//            otherUserId: String,
//            otherDeviceId: String
//    ): String {
//        if (method == VerificationMethod.SAS) {
//            val tx = DefaultOutgoingSASDefaultVerificationTransaction(
//                    setDeviceVerificationAction,
//                    userId,
//                    deviceId,
//                    cryptoStore,
//                    crossSigningService,
//                    outgoingKeyRequestManager,
//                    secretShareManager,
//                    myDeviceInfoHolder.get().myDevice.fingerprint()!!,
//                    transactionId,
//                    otherUserId,
//                    otherDeviceId
//            )
//            tx.transport = verificationTransportRoomMessageFactory.createTransport(roomId, tx)
//            addTransaction(tx)
//
//            tx.start()
//            return transactionId
//        } else {
//            throw IllegalArgumentException("Unknown verification method")
//        }
//    }

//    override fun readyPendingVerificationInDMs(
//            methods: List<VerificationMethod>,
//            otherUserId: String,
//            roomId: String,
//            transactionId: String
//    ): Boolean {
//        Timber.v("## SAS readyPendingVerificationInDMs $otherUserId room:$roomId tx:$transactionId")
//        // Let's find the related request
//        val existingRequest = getExistingVerificationRequest(otherUserId, transactionId)
//        if (existingRequest != null) {
//            // we need to send a ready event, with matching methods
//            val transport = verificationTransportRoomMessageFactory.createTransport(roomId, null)
//            val computedMethods = computeReadyMethods(
//                    transactionId,
//                    otherUserId,
//                    existingRequest.requestInfo?.fromDevice ?: "",
//                    existingRequest.requestInfo?.methods,
//                    methods
//            ) {
//                verificationTransportRoomMessageFactory.createTransport(roomId, it)
//            }
//            if (methods.isNullOrEmpty()) {
//                Timber.i("Cannot ready this request, no common methods found txId:$transactionId")
//                // TODO buttons should not be shown in  this case?
//                return false
//            }
//            // TODO this is not yet related to a transaction, maybe we should use another method like for cancel?
//            val readyMsg = transport.createReady(transactionId, deviceId ?: "", computedMethods)
//            transport.sendToOther(
//                    EventType.KEY_VERIFICATION_READY,
//                    readyMsg,
//                    VerificationTxState.None,
//                    CancelCode.User,
//                    null // TODO handle error?
//            )
//            updatePendingRequest(existingRequest.copy(readyInfo = readyMsg.asValidObject()))
//            return true
//        } else {
//            Timber.e("## SAS readyPendingVerificationInDMs Verification not found")
//            // :/ should not be possible... unless live observer very slow
//            return false
//        }
//    }

    override suspend fun readyPendingVerification(
            methods: List<VerificationMethod>,
            otherUserId: String,
            transactionId: String
    ): Boolean {
        Timber.v("## SAS readyPendingVerification $otherUserId tx:$transactionId")
        val deferred = CompletableDeferred<PendingVerificationRequest?>()
        stateMachine.send(
                VerificationIntent.ActionReadyRequest(
                        transactionId = transactionId,
                        methods = methods,
                        deferred = deferred
                )
        )
//        val request = deferred.await()
//        if (request?.readyInfo != null) {
//            val transport = transportForRequest(request)
//            try {
//                val readyMsg = transport.createReady(transactionId, request.readyInfo.fromDevice, request.readyInfo.methods)
//                transport.sendVerificationReady(
//                        readyMsg,
//                        request.otherUserId,
//                        request.requestInfo?.fromDevice,
//                        request.roomId
//                )
//                return true
//            } catch (failure: Throwable) {
//                // revert back
//                stateManagerActor.send(
//                        VerificationIntent.UpdateRequest(
//                                request.copy(
//                                        readyInfo = null
//                                )
//                        )
//                )
//            }
//        }
        return deferred.await() != null

//        // Let's find the related request
//        val existingRequest = getExistingVerificationRequest(otherUserId, transactionId)
//                ?: return false.also {
//                    Timber.e("## SAS readyPendingVerification Verification not found")
//                    // :/ should not be possible... unless live observer very slow
//                }
//        // we need to send a ready event, with matching methods
//
//        val otherUserMethods = existingRequest.requestInfo?.methods.orEmpty()
//        val computedMethods = computeReadyMethods(
// //                transactionId,
// //                otherUserId,
// //                existingRequest.requestInfo?.fromDevice ?: "",
//                otherUserMethods,
//                methods
//        )
//
//        if (methods.isEmpty()) {
//            Timber.i("## SAS Cannot ready this request, no common methods found txId:$transactionId")
//            // TODO buttons should not be shown in this case?
//            return false
//        }
//        // TODO this is not yet related to a transaction, maybe we should use another method like for cancel?
//        val transport = if (existingRequest.roomId != null) {
//            verificationTransportRoomMessageFactory.createTransport(existingRequest.roomId)
//        } else {
//            verificationTransportToDeviceFactory.createTransport()
//        }
//        val readyMsg = transport.createReady(transactionId, deviceId ?: "", computedMethods).also {
//            Timber.i("## SAS created ready Message ${it}")
//        }
//
//        val qrCodeData = if (otherUserMethods.canScanCode() && methods.contains(VerificationMethod.QR_CODE_SHOW)) {
//            createQrCodeData(transactionId, otherUserId, existingRequest.requestInfo?.fromDevice)
//        } else {
//            null
//        }
//
//        transport.sendVerificationReady(readyMsg, existingRequest.otherUserId, existingRequest.requestInfo?.fromDevice, existingRequest.roomId)
//        updatePendingRequest(
//                existingRequest.copy(
//                        readyInfo = readyMsg.asValidObject(),
//                        qrCodeText = qrCodeData?.toEncodedString()
//                )
//        )
//        return true
    }

//    private fun transportForRequest(request: PendingVerificationRequest): VerificationTransport {
//        return if (request.roomId != null) {
//            verificationTransportRoomMessageFactory.createTransport(request.roomId)
//        } else {
//            verificationTransportToDeviceFactory.createTransport(
//                    request.otherUserId,
//                    request.requestInfo?.fromDevice.orEmpty()
//            )
//        }
//    }

//    private suspend fun computeReadyMethods(
// //            transactionId: String,
// //            otherUserId: String,
// //            otherDeviceId: String,
//            otherUserMethods: List<String>?,
//            methods: List<VerificationMethod>,
//            transportCreator: (DefaultVerificationTransaction) -> VerificationTransport
//    ): List<String> {
//        if (otherUserMethods.isNullOrEmpty()) {
//            return emptyList()
//        }
//
//        val result = mutableSetOf<String>()
//
//        if (VERIFICATION_METHOD_SAS in otherUserMethods && VerificationMethod.SAS in methods) {
//            // Other can do SAS and so do I
//            result.add(VERIFICATION_METHOD_SAS)
//        }
//
//        if (VERIFICATION_METHOD_QR_CODE_SCAN in otherUserMethods || VERIFICATION_METHOD_QR_CODE_SHOW in otherUserMethods) {
//            // Other user wants to verify using QR code. Cross-signing has to be setup
// //            val qrCodeData = createQrCodeData(transactionId, otherUserId, otherDeviceId)
// //
// //            if (qrCodeData != null) {
//            if (VERIFICATION_METHOD_QR_CODE_SCAN in otherUserMethods && VerificationMethod.QR_CODE_SHOW in methods) {
//                // Other can Scan and I can show QR code
//                result.add(VERIFICATION_METHOD_QR_CODE_SHOW)
//                result.add(VERIFICATION_METHOD_RECIPROCATE)
//            }
//            if (VERIFICATION_METHOD_QR_CODE_SHOW in otherUserMethods && VerificationMethod.QR_CODE_SCAN in methods) {
//                // Other can show and I can scan QR code
//                result.add(VERIFICATION_METHOD_QR_CODE_SCAN)
//                result.add(VERIFICATION_METHOD_RECIPROCATE)
//            }
// //            }
//
// //            if (VERIFICATION_METHOD_RECIPROCATE in result) {
// //                // Create the pending transaction
// //                val tx = DefaultQrCodeVerificationTransaction(
// //                        setDeviceVerificationAction = setDeviceVerificationAction,
// //                        transactionId = transactionId,
// //                        otherUserId = otherUserId,
// //                        otherDeviceId = otherDeviceId,
// //                        crossSigningService = crossSigningService,
// //                        outgoingKeyRequestManager = outgoingKeyRequestManager,
// //                        secretShareManager = secretShareManager,
// //                        cryptoStore = cryptoStore,
// //                        qrCodeData = qrCodeData,
// //                        userId = userId,
// //                        deviceId = deviceId ?: "",
// //                        isIncoming = false
// //                )
// //
// //                tx.transport = transportCreator.invoke(tx)
// //
// //                addTransaction(tx)
// //            }
//        }
//
//        return result.toList()
//    }

//    /**
//     * This string must be unique for the pair of users performing verification for the duration that the transaction is valid.
//     */
//    private fun createUniqueIDForTransaction(otherUserId: String, otherDeviceID: String): String {
//        return buildString {
//            append(userId).append("|")
//            append(deviceId).append("|")
//            append(otherUserId).append("|")
//            append(otherDeviceID).append("|")
//            append(UUID.randomUUID().toString())
//        }
//    }

//    override suspend fun transactionUpdated(tx: VerificationTransaction) {
//        dispatchTxUpdated(tx)
//        if (tx.state is VerificationTxState.TerminalTxState) {
//            // remove
//            this.removeTransaction(tx.otherUserId, tx.transactionId)
//        }
//    }
}
