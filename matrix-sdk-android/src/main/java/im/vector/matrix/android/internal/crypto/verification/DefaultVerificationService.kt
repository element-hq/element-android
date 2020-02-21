/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.verification

import android.os.Handler
import android.os.Looper
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningService
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.QrCodeVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationMethod
import im.vector.matrix.android.api.session.crypto.sas.VerificationService
import im.vector.matrix.android.api.session.crypto.sas.VerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationTxState
import im.vector.matrix.android.api.session.crypto.sas.safeValueOf
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageRelationContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationAcceptContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationCancelContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationDoneContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationKeyContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationMacContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationReadyContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationRequestContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationStartContent
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MyDeviceInfoHolder
import im.vector.matrix.android.internal.crypto.actions.SetDeviceVerificationAction
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationAccept
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationCancel
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationKey
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationMac
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationReady
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationRequest
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationStart
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import im.vector.matrix.android.internal.crypto.model.rest.toValue
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.verification.qrcode.DefaultQrCodeVerificationTransaction
import im.vector.matrix.android.internal.crypto.verification.qrcode.QrCodeData
import im.vector.matrix.android.internal.crypto.verification.qrcode.generateSharedSecretV2
import im.vector.matrix.android.internal.di.DeviceId
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.set

@SessionScope
internal class DefaultVerificationService @Inject constructor(
        @UserId private val userId: String,
        @DeviceId private val deviceId: String?,
        private val cryptoStore: IMXCryptoStore,
        private val myDeviceInfoHolder: Lazy<MyDeviceInfoHolder>,
        private val deviceListManager: DeviceListManager,
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val verificationTransportRoomMessageFactory: VerificationTransportRoomMessageFactory,
        private val verificationTransportToDeviceFactory: VerificationTransportToDeviceFactory,
        private val crossSigningService: CrossSigningService
) : DefaultVerificationTransaction.Listener, VerificationService {

    private val uiHandler = Handler(Looper.getMainLooper())

    // Cannot be injected in constructor as it creates a dependency cycle
    lateinit var cryptoService: CryptoService

    // map [sender : [transaction]]
    private val txMap = HashMap<String, HashMap<String, DefaultVerificationTransaction>>()

    /**
     * Map [sender: [PendingVerificationRequest]]
     * For now we keep all requests (even terminated ones) during the lifetime of the app.
     */
    private val pendingRequests = HashMap<String, ArrayList<PendingVerificationRequest>>()

    // Event received from the sync
    fun onToDeviceEvent(event: Event) {
        GlobalScope.launch(coroutineDispatchers.crypto) {
            when (event.getClearType()) {
                EventType.KEY_VERIFICATION_START         -> {
                    onStartRequestReceived(event)
                }
                EventType.KEY_VERIFICATION_CANCEL        -> {
                    onCancelReceived(event)
                }
                EventType.KEY_VERIFICATION_ACCEPT        -> {
                    onAcceptReceived(event)
                }
                EventType.KEY_VERIFICATION_KEY           -> {
                    onKeyReceived(event)
                }
                EventType.KEY_VERIFICATION_MAC           -> {
                    onMacReceived(event)
                }
                EventType.KEY_VERIFICATION_READY         -> {
                    onReadyReceived(event)
                }
                MessageType.MSGTYPE_VERIFICATION_REQUEST -> {
                    onRequestReceived(event)
                }
                else                                     -> {
                    // ignore
                }
            }
        }
    }

    fun onRoomEvent(event: Event) {
        GlobalScope.launch(coroutineDispatchers.crypto) {
            when (event.getClearType()) {
                EventType.KEY_VERIFICATION_START  -> {
                    onRoomStartRequestReceived(event)
                }
                EventType.KEY_VERIFICATION_CANCEL -> {
                    // MultiSessions | ignore events if i didn't sent the start from this device, or accepted from this device
                    onRoomCancelReceived(event)
                }
                EventType.KEY_VERIFICATION_ACCEPT -> {
                    onRoomAcceptReceived(event)
                }
                EventType.KEY_VERIFICATION_KEY    -> {
                    onRoomKeyRequestReceived(event)
                }
                EventType.KEY_VERIFICATION_MAC    -> {
                    onRoomMacReceived(event)
                }
                EventType.KEY_VERIFICATION_READY  -> {
                    onRoomReadyReceived(event)
                }
                EventType.KEY_VERIFICATION_DONE   -> {
                    onRoomDoneReceived(event)
                }
                EventType.MESSAGE                 -> {
                    if (MessageType.MSGTYPE_VERIFICATION_REQUEST == event.getClearContent().toModel<MessageContent>()?.msgType) {
                        onRoomRequestReceived(event)
                    }
                }
                else                              -> {
                    // ignore
                }
            }
        }
    }

    private var listeners = ArrayList<VerificationService.Listener>()

    override fun addListener(listener: VerificationService.Listener) {
        uiHandler.post {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    override fun removeListener(listener: VerificationService.Listener) {
        uiHandler.post {
            listeners.remove(listener)
        }
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
        setDeviceVerificationAction.handle(DeviceTrustLevel(false, true),
                userId,
                deviceID)

        listeners.forEach {
            try {
                it.markedAsManuallyVerified(userId, deviceID)
            } catch (e: Throwable) {
                Timber.e(e, "## Error while notifying listeners")
            }
        }
    }

    fun onRoomRequestHandledByOtherDevice(event: Event) {
        val requestInfo = event.content.toModel<MessageRelationContent>()
                ?: return
        val requestId = requestInfo.relatesTo?.eventId ?: return
        getExistingVerificationRequestInRoom(event.roomId ?: "", requestId)?.let {
            updatePendingRequest(
                    it.copy(
                            handledByOtherSession = true
                    )
            )
        }
    }

    private fun onRequestReceived(event: Event) {
        val requestInfo = event.getClearContent().toModel<KeyVerificationRequest>()!!

        if (!requestInfo.isValid()) {
            // ignore
            Timber.e("## SAS Received invalid key request")
            return
        }
        val senderId = event.senderId ?: return

        // We don't want to block here
        val otherDeviceId = requestInfo.fromDevice ?: return

        GlobalScope.launch {
            if (checkKeysAreDownloaded(senderId, otherDeviceId) == null) {
                Timber.e("## Verification device $otherDeviceId is not known")
            }
        }

        // Remember this request
        val requestsForUser = pendingRequests[senderId]
                ?: ArrayList<PendingVerificationRequest>().also {
                    pendingRequests[event.senderId] = it
                }

        val pendingVerificationRequest = PendingVerificationRequest(
                ageLocalTs = event.ageLocalTs ?: System.currentTimeMillis(),
                isIncoming = true,
                otherUserId = senderId, // requestInfo.toUserId,
                roomId = null,
                transactionId = requestInfo.transactionID,
                localID = requestInfo.transactionID!!,
                requestInfo = requestInfo
        )
        requestsForUser.add(pendingVerificationRequest)
        dispatchRequestAdded(pendingVerificationRequest)
    }

    suspend fun onRoomRequestReceived(event: Event) {
        Timber.v("## SAS Verification request from ${event.senderId} in room ${event.roomId}")
        val requestInfo = event.getClearContent().toModel<MessageVerificationRequestContent>()
                ?: return
        val senderId = event.senderId ?: return
        val fromDevice = requestInfo.fromDevice ?: return

        if (requestInfo.toUserId != userId) {
            // I should ignore this, it's not for me
            Timber.w("## SAS Verification ignoring request from ${event.senderId}, not sent to me")
            return
        }

        // We don't want to block here
        GlobalScope.launch {
            if (checkKeysAreDownloaded(senderId, fromDevice) == null) {
                Timber.e("## SAS Verification device $fromDevice is not known")
            }
        }

        // Remember this request
        val requestsForUser = pendingRequests[senderId]
                ?: ArrayList<PendingVerificationRequest>().also {
                    pendingRequests[event.senderId] = it
                }

        val pendingVerificationRequest = PendingVerificationRequest(
                ageLocalTs = event.ageLocalTs ?: System.currentTimeMillis(),
                isIncoming = true,
                otherUserId = senderId, // requestInfo.toUserId,
                roomId = event.roomId,
                transactionId = event.eventId,
                localID = event.eventId!!,
                requestInfo = requestInfo
        )
        requestsForUser.add(pendingVerificationRequest)
        dispatchRequestAdded(pendingVerificationRequest)

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

    private suspend fun onRoomStartRequestReceived(event: Event) {
        val startReq = event.getClearContent().toModel<MessageVerificationStartContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )

        val otherUserId = event.senderId
        if (startReq?.isValid()?.not() == true) {
            Timber.e("## received invalid verification request")
            if (startReq.transactionID != null) {
                verificationTransportRoomMessageFactory.createTransport(event.roomId ?: "", null)
                        .cancelTransaction(
                                startReq.transactionID ?: "",
                                otherUserId!!,
                                startReq.fromDevice ?: event.getSenderKey()!!,
                                CancelCode.UnknownMethod
                        )
            }
            return
        }

        handleStart(otherUserId, startReq as VerificationInfoStart) {
            it.transport = verificationTransportRoomMessageFactory.createTransport(event.roomId ?: "", it)
        }?.let {
            verificationTransportRoomMessageFactory.createTransport(event.roomId ?: "", null)
                    .cancelTransaction(
                            startReq.transactionID ?: "",
                            otherUserId!!,
                            startReq.fromDevice ?: event.getSenderKey()!!,
                            it
                    )
        }
    }

    private suspend fun onStartRequestReceived(event: Event) {
        Timber.e("## SAS received Start request ${event.eventId}")
        val startReq = event.getClearContent().toModel<KeyVerificationStart>()!!
        Timber.v("## SAS received Start request $startReq")

        val otherUserId = event.senderId
        if (!startReq.isValid()) {
            Timber.e("## SAS received invalid verification request")
            if (startReq.transactionID != null) {
                verificationTransportToDeviceFactory.createTransport(null).cancelTransaction(
                        startReq.transactionID,
                        otherUserId!!,
                        startReq.fromDevice ?: event.getSenderKey()!!,
                        CancelCode.UnknownMethod
                )
            }
            return
        }
        // Download device keys prior to everything
        handleStart(otherUserId, startReq) {
            it.transport = verificationTransportToDeviceFactory.createTransport(it)
        }?.let {
            verificationTransportToDeviceFactory.createTransport(null).cancelTransaction(
                    startReq.transactionID ?: "",
                    otherUserId!!,
                    startReq.fromDevice ?: event.getSenderKey()!!,
                    it
            )
        }
    }

    /**
     * Return a CancelCode to make the caller cancel the verification. Else return null
     */
    private suspend fun handleStart(otherUserId: String?, startReq: VerificationInfoStart, txConfigure: (DefaultVerificationTransaction) -> Unit): CancelCode? {
        Timber.d("## SAS onStartRequestReceived ${startReq.transactionID}")
        if (checkKeysAreDownloaded(otherUserId!!, startReq.fromDevice ?: "") != null) {
            val tid = startReq.transactionID!!
            val existing = getExistingTransaction(otherUserId, tid)

            when (startReq.method) {
                VERIFICATION_METHOD_SAS         -> {
                    when (existing) {
                        is SasVerificationTransaction    -> {
                            // should cancel both!
                            Timber.v("## SAS onStartRequestReceived - Request exist with same id ${startReq.transactionID}")
                            existing.cancel(CancelCode.UnexpectedMessage)
                            // Already cancelled, so return null
                            return null
                        }
                        is QrCodeVerificationTransaction -> {
                            // Nothing to do?
                        }
                        null                             -> {
                            getExistingTransactionsForUser(otherUserId)
                                    ?.filterIsInstance(SasVerificationTransaction::class.java)
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.also {
                                        // Multiple keyshares between two devices:
                                        // any two devices may only have at most one key verification in flight at a time.
                                        Timber.v("## SAS onStartRequestReceived - Already a transaction with this user ${startReq.transactionID}")
                                    }
                                    ?.forEach {
                                        it.cancel(CancelCode.UnexpectedMessage)
                                    }
                                    ?.also {
                                        return CancelCode.UnexpectedMessage
                                    }
                        }
                    }

                    // Ok we can create a SAS transaction
                    Timber.v("## SAS onStartRequestReceived - request accepted ${startReq.transactionID!!}")
                    // If there is a corresponding request, we can auto accept
                    // as we are the one requesting in first place (or we accepted the request)
                    // I need to check if the pending request was related to this device also
                    val autoAccept = getExistingVerificationRequest(otherUserId)?.any {
                        it.transactionId == startReq.transactionID
                                && (it.requestInfo?.fromDevice == this.deviceId || it.readyInfo?.fromDevice == this.deviceId)
                    }
                            ?: false
                    val tx = DefaultIncomingSASDefaultVerificationTransaction(
//                            this,
                            setDeviceVerificationAction,
                            userId,
                            deviceId,
                            cryptoStore,
                            crossSigningService,
                            myDeviceInfoHolder.get().myDevice.fingerprint()!!,
                            startReq.transactionID!!,
                            otherUserId,
                            autoAccept).also { txConfigure(it) }
                    addTransaction(tx)
                    tx.acceptVerificationEvent(otherUserId, startReq)
                    return null
                }
                VERIFICATION_METHOD_RECIPROCATE -> {
                    // Other user has scanned my QR code
                    if (existing is DefaultQrCodeVerificationTransaction) {
                        existing.onStartReceived(startReq)
                        return null
                    } else {
                        Timber.w("## SAS onStartRequestReceived - unexpected message ${startReq.transactionID}")
                        return CancelCode.UnexpectedMessage
                    }
                }
                else                            -> {
                    Timber.e("## SAS onStartRequestReceived - unknown method ${startReq.method}")
                    return CancelCode.UnknownMethod
                }
            }
        } else {
            return CancelCode.UnexpectedMessage
        }
    }

    // TODO Refacto: It could just return a boolean
    private suspend fun checkKeysAreDownloaded(otherUserId: String,
                                               otherDeviceId: String): MXUsersDevicesMap<CryptoDeviceInfo>? {
        return try {
            var keys = deviceListManager.downloadKeys(listOf(otherUserId), false)
            if (keys.getUserDeviceIds(otherUserId)?.contains(otherDeviceId) == true) {
                return keys
            } else {
                // force download
                keys = deviceListManager.downloadKeys(listOf(otherUserId), true)
                return keys.takeIf { keys.getUserDeviceIds(otherUserId)?.contains(otherDeviceId) == true }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun onRoomCancelReceived(event: Event) {
        val cancelReq = event.getClearContent().toModel<MessageVerificationCancelContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
        if (cancelReq == null || cancelReq.isValid().not()) {
            // ignore
            Timber.e("## SAS Received invalid key request")
            // TODO should we cancel?
            return
        }
        getExistingVerificationRequest(event.senderId ?: "", cancelReq.transactionID)?.let {
            updatePendingRequest(it.copy(cancelConclusion = safeValueOf(cancelReq.code)))
            // Should we remove it from the list?
        }
        handleOnCancel(event.senderId!!, cancelReq)
    }

    private fun onCancelReceived(event: Event) {
        Timber.v("## SAS onCancelReceived")
        val cancelReq = event.getClearContent().toModel<KeyVerificationCancel>()!!

        if (!cancelReq.isValid()) {
            // ignore
            Timber.e("## SAS Received invalid cancel request")
            return
        }
        val otherUserId = event.senderId!!

        handleOnCancel(otherUserId, cancelReq)
    }

    private fun handleOnCancel(otherUserId: String, cancelReq: VerificationInfoCancel) {
        Timber.v("## SAS onCancelReceived otherUser:$otherUserId reason:${cancelReq.reason}")

        val existingTransaction = getExistingTransaction(otherUserId, cancelReq.transactionID!!)
        val existingRequest = getExistingVerificationRequest(otherUserId, cancelReq.transactionID!!)

        if (existingRequest != null) {
            // Mark this request as cancelled
            updatePendingRequest(existingRequest.copy(
                    cancelConclusion = safeValueOf(cancelReq.code)
            ))
        }

        if (existingTransaction is SASDefaultVerificationTransaction) {
            existingTransaction.state = VerificationTxState.Cancelled(safeValueOf(cancelReq.code), false)
        }
    }

    private fun onRoomAcceptReceived(event: Event) {
        Timber.d("##  SAS Received Accept via DM $event")
        val accept = event.getClearContent().toModel<MessageVerificationAcceptContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
                ?: return
        handleAccept(accept, event.senderId!!)
    }

    private fun onAcceptReceived(event: Event) {
        Timber.d("##  SAS Received Accept $event")
        val acceptReq = event.getClearContent().toModel<KeyVerificationAccept>() ?: return
        handleAccept(acceptReq, event.senderId!!)
    }

    private fun handleAccept(acceptReq: VerificationInfoAccept, senderId: String) {
        if (!acceptReq.isValid()) {
            // ignore
            Timber.e("## SAS Received invalid accept request")
            return
        }
        val otherUserId = senderId
        val existing = getExistingTransaction(otherUserId, acceptReq.transactionID!!)
        if (existing == null) {
            Timber.e("## SAS Received invalid accept request")
            return
        }

        if (existing is SASDefaultVerificationTransaction) {
            existing.acceptVerificationEvent(otherUserId, acceptReq)
        } else {
            // not other types now
        }
    }

    private fun onRoomKeyRequestReceived(event: Event) {
        val keyReq = event.getClearContent().toModel<MessageVerificationKeyContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
        if (keyReq == null || keyReq.isValid().not()) {
            // ignore
            Timber.e("## SAS Received invalid key request")
            // TODO should we cancel?
            return
        }
        handleKeyReceived(event, keyReq)
    }

    private fun onKeyReceived(event: Event) {
        val keyReq = event.getClearContent().toModel<KeyVerificationKey>()!!

        if (!keyReq.isValid()) {
            // ignore
            Timber.e("## SAS Received invalid key request")
            return
        }
        handleKeyReceived(event, keyReq)
    }

    private fun handleKeyReceived(event: Event, keyReq: VerificationInfoKey) {
        Timber.d("##  SAS Received Key from ${event.senderId} with info $keyReq")
        val otherUserId = event.senderId!!
        val existing = getExistingTransaction(otherUserId, keyReq.transactionID!!)
        if (existing == null) {
            Timber.e("##  SAS Received invalid key request")
            return
        }
        if (existing is SASDefaultVerificationTransaction) {
            existing.acceptVerificationEvent(otherUserId, keyReq)
        } else {
            // not other types now
        }
    }

    private fun onRoomMacReceived(event: Event) {
        val macReq = event.getClearContent().toModel<MessageVerificationMacContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
        if (macReq == null || macReq.isValid().not() || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid mac request")
            // TODO should we cancel?
            return
        }
        handleMacReceived(event.senderId, macReq)
    }

    private suspend fun onRoomReadyReceived(event: Event) {
        val readyReq = event.getClearContent().toModel<MessageVerificationReadyContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )
        if (readyReq == null || readyReq.isValid().not() || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid ready request")
            // TODO should we cancel?
            return
        }
        if (checkKeysAreDownloaded(event.senderId, readyReq.fromDevice ?: "") == null) {
            Timber.e("## SAS Verification device ${readyReq.fromDevice} is not known")
            // TODO cancel?
            return
        }

        handleReadyReceived(event.senderId, readyReq) {
            verificationTransportRoomMessageFactory.createTransport(event.roomId!!, it)
        }
    }

    private suspend fun onReadyReceived(event: Event) {
        val readyReq = event.getClearContent().toModel<KeyVerificationReady>()

        if (readyReq == null || readyReq.isValid().not() || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid ready request")
            // TODO should we cancel?
            return
        }
        if (checkKeysAreDownloaded(event.senderId, readyReq.fromDevice ?: "") == null) {
            Timber.e("## SAS Verification device ${readyReq.fromDevice} is not known")
            // TODO cancel?
            return
        }

        handleReadyReceived(event.senderId, readyReq) {
            verificationTransportToDeviceFactory.createTransport(it)
        }
    }

    private fun onRoomDoneReceived(event: Event) {
        val doneReq = event.getClearContent().toModel<MessageVerificationDoneContent>()
                ?.copy(
                        // relates_to is in clear in encrypted payload
                        relatesTo = event.content.toModel<MessageRelationContent>()?.relatesTo
                )

        if (doneReq == null || doneReq.isValid().not() || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid Done request")
            // TODO should we cancel?
            return
        }

        handleDoneReceived(event.senderId, doneReq)
    }

    private fun onMacReceived(event: Event) {
        val macReq = event.getClearContent().toModel<KeyVerificationMac>()!!

        if (!macReq.isValid() || event.senderId == null) {
            // ignore
            Timber.e("## SAS Received invalid mac request")
            return
        }
        handleMacReceived(event.senderId, macReq)
    }

    private fun handleMacReceived(senderId: String, macReq: VerificationInfoMac) {
        Timber.v("## SAS Received $macReq")
        val existing = getExistingTransaction(senderId, macReq.transactionID!!)
        if (existing == null) {
            Timber.e("## SAS Received invalid Mac request")
            return
        }
        if (existing is SASDefaultVerificationTransaction) {
            existing.acceptVerificationEvent(senderId, macReq)
        } else {
            // not other types known for now
        }
    }

    private fun handleReadyReceived(senderId: String,
                                    readyReq: VerificationInfoReady,
                                    transportCreator: (DefaultVerificationTransaction) -> VerificationTransport) {
        val existingRequest = getExistingVerificationRequest(senderId)?.find { it.transactionId == readyReq.transactionID }
        if (existingRequest == null) {
            Timber.e("## SAS Received Ready for unknown request txId:${readyReq.transactionID} fromDevice ${readyReq.fromDevice}")
            return
        }

        val qrCodeData = readyReq.methods
                // Check if other user is able to scan QR code
                ?.takeIf { it.contains(VERIFICATION_METHOD_QR_CODE_SCAN) }
                ?.let {
                    createQrCodeData(existingRequest.transactionId, existingRequest.otherUserId, readyReq.fromDevice)
                }

        if (readyReq.methods.orEmpty().contains(VERIFICATION_METHOD_RECIPROCATE)) {
            // Create the pending transaction
            val tx = DefaultQrCodeVerificationTransaction(
                    setDeviceVerificationAction,
                    readyReq.transactionID!!,
                    senderId,
                    readyReq.fromDevice,
                    crossSigningService,
                    cryptoStore,
                    qrCodeData,
                    userId,
                    deviceId ?: "",
                    false)

            tx.transport = transportCreator.invoke(tx)

            addTransaction(tx)
        }

        updatePendingRequest(existingRequest.copy(
                readyInfo = readyReq
        ))
    }

    private fun createQrCodeData(requestId: String?, otherUserId: String, otherDeviceId: String?): QrCodeData? {
        requestId ?: run {
            Timber.w("## Unknown requestId")
            return null
        }

        return when {
            userId != otherUserId                        ->
                createQrCodeDataForDistinctUser(requestId, otherUserId)
            crossSigningService.isCrossSigningVerified() ->
                // This is a self verification and I am the old device (Osborne2)
                createQrCodeDataForVerifiedDevice(requestId, otherDeviceId)
            else                                         ->
                // This is a self verification and I am the new device (Dynabook)
                createQrCodeDataForUnVerifiedDevice(requestId)
        }
    }

    private fun createQrCodeDataForDistinctUser(requestId: String, otherUserId: String): QrCodeData.VerifyingAnotherUser? {
        val myMasterKey = crossSigningService.getMyCrossSigningKeys()
                ?.masterKey()
                ?.unpaddedBase64PublicKey
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val otherUserMasterKey = crossSigningService.getUserCrossSigningKeys(otherUserId)
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
    private fun createQrCodeDataForVerifiedDevice(requestId: String, otherDeviceId: String?): QrCodeData.SelfVerifyingMasterKeyTrusted? {
        val myMasterKey = crossSigningService.getMyCrossSigningKeys()
                ?.masterKey()
                ?.unpaddedBase64PublicKey
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val otherDeviceKey = otherDeviceId
                ?.let {
                    cryptoStore.getUserDevice(userId, otherDeviceId)?.fingerprint()
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
        val myMasterKey = crossSigningService.getMyCrossSigningKeys()
                ?.masterKey()
                ?.unpaddedBase64PublicKey
                ?: run {
                    Timber.w("## Unable to get my master key")
                    return null
                }

        val myDeviceKey = myDeviceInfoHolder.get().myDevice.fingerprint()
                ?: run {
                    Timber.w("## Unable to get my fingerprint")
                    return null
                }

        return QrCodeData.SelfVerifyingMasterKeyNotTrusted(
                transactionId = requestId,
                deviceKey = myDeviceKey,
                userMasterCrossSigningPublicKey = myMasterKey,
                sharedSecret = generateSharedSecretV2()
        )
    }

    private fun handleDoneReceived(senderId: String, doneInfo: VerificationInfo) {
        val existingRequest = getExistingVerificationRequest(senderId)?.find { it.transactionId == doneInfo.transactionID }
        if (existingRequest == null) {
            Timber.e("## SAS Received Done for unknown request txId:${doneInfo.transactionID}")
            return
        }
        updatePendingRequest(existingRequest.copy(isSuccessful = true))
    }

    // TODO All this methods should be delegated to a TransactionStore
    override fun getExistingTransaction(otherUserId: String, tid: String): VerificationTransaction? {
        synchronized(lock = txMap) {
            return txMap[otherUserId]?.get(tid)
        }
    }

    override fun getExistingVerificationRequest(otherUserId: String): List<PendingVerificationRequest>? {
        synchronized(lock = pendingRequests) {
            return pendingRequests[otherUserId]
        }
    }

    override fun getExistingVerificationRequest(otherUserId: String, tid: String?): PendingVerificationRequest? {
        synchronized(lock = pendingRequests) {
            return tid?.let { tid -> pendingRequests[otherUserId]?.firstOrNull { it.transactionId == tid } }
        }
    }

    override fun getExistingVerificationRequestInRoom(roomId: String, tid: String?): PendingVerificationRequest? {
        synchronized(lock = pendingRequests) {
            return tid?.let { tid ->
                pendingRequests.flatMap { entry ->
                    entry.value.filter { it.roomId == roomId && it.transactionId == tid }
                }.firstOrNull()
            }
        }
    }

    private fun getExistingTransactionsForUser(otherUser: String): Collection<VerificationTransaction>? {
        synchronized(txMap) {
            return txMap[otherUser]?.values
        }
    }

    private fun removeTransaction(otherUser: String, tid: String) {
        synchronized(txMap) {
            txMap[otherUser]?.remove(tid)?.removeListener(this)
        }
    }

    private fun addTransaction(tx: DefaultVerificationTransaction) {
        tx.otherUserId.let { otherUserId ->
            synchronized(txMap) {
                val txInnerMap = txMap.getOrPut(otherUserId) { HashMap() }
                txInnerMap[tx.transactionId] = tx
                dispatchTxAdded(tx)
                tx.addListener(this)
            }
        }
    }

    override fun beginKeyVerification(method: VerificationMethod, otherUserId: String, otherDeviceId: String, transactionId: String?): String? {
        val txID = transactionId?.takeIf { it.isNotEmpty() } ?: createUniqueIDForTransaction(otherUserId, otherDeviceId)
        // should check if already one (and cancel it)
        if (method == VerificationMethod.SAS) {
            val tx = DefaultOutgoingSASDefaultVerificationTransaction(
                    setDeviceVerificationAction,
                    userId,
                    deviceId,
                    cryptoStore,
                    crossSigningService,
                    myDeviceInfoHolder.get().myDevice.fingerprint()!!,
                    txID,
                    otherUserId,
                    otherDeviceId)
            tx.transport = verificationTransportToDeviceFactory.createTransport(tx)
            addTransaction(tx)

            tx.start()
            return txID
        } else {
            throw IllegalArgumentException("Unknown verification method")
        }
    }

    override fun requestKeyVerificationInDMs(methods: List<VerificationMethod>, otherUserId: String, roomId: String, localId: String?)
            : PendingVerificationRequest {
        Timber.i("## SAS Requesting verification to user: $otherUserId in room $roomId")

        val requestsForUser = pendingRequests[otherUserId]
                ?: ArrayList<PendingVerificationRequest>().also {
                    pendingRequests[otherUserId] = it
                }

        val transport = verificationTransportRoomMessageFactory.createTransport(roomId, null)

        // Cancel existing pending requests?
        requestsForUser.toImmutableList().forEach { existingRequest ->
            existingRequest.transactionId?.let { tid ->
                if (!existingRequest.isFinished) {
                    Timber.d("## SAS, cancelling pending requests to start a new one")
                    updatePendingRequest(existingRequest.copy(cancelConclusion = CancelCode.User))
                    transport.cancelTransaction(tid, existingRequest.otherUserId, "", CancelCode.User)
                }
            }
        }

        val localID = localId ?: LocalEcho.createLocalEchoId()

        val verificationRequest = PendingVerificationRequest(
                ageLocalTs = System.currentTimeMillis(),
                isIncoming = false,
                roomId = roomId,
                localID = localID,
                otherUserId = otherUserId
        )

        // We can SCAN or SHOW QR codes only if cross-signing is verified
        val methodValues = if (crossSigningService.isCrossSigningVerified()) {
            // Add reciprocate method if application declares it can scan or show QR codes
            // Not sure if it ok to do that (?)
            val reciprocateMethod = methods
                    .firstOrNull { it == VerificationMethod.QR_CODE_SCAN || it == VerificationMethod.QR_CODE_SHOW }
                    ?.let { listOf(VERIFICATION_METHOD_RECIPROCATE) }.orEmpty()
            methods.map { it.toValue() } + reciprocateMethod
        } else {
            // Filter out SCAN and SHOW qr code method
            methods
                    .filter { it != VerificationMethod.QR_CODE_SHOW && it != VerificationMethod.QR_CODE_SCAN }
                    .map { it.toValue() }
        }
                .distinct()

        transport.sendVerificationRequest(methodValues, localID, otherUserId, roomId, null) { syncedId, info ->
            // We need to update with the syncedID
            updatePendingRequest(verificationRequest.copy(
                    transactionId = syncedId,
                    // localId stays different
                    requestInfo = info
            ))
        }

        requestsForUser.add(verificationRequest)
        dispatchRequestAdded(verificationRequest)

        return verificationRequest
    }

    override fun requestKeyVerification(methods: List<VerificationMethod>, otherUserId: String, otherDevices: List<String>?): PendingVerificationRequest {
        // TODO refactor this with the DM one
        Timber.i("## Requesting verification to user: $otherUserId with device list $otherDevices")

        val targetDevices = otherDevices ?: cryptoService.getUserDevices(otherUserId).map { it.deviceId }
        val requestsForUser = pendingRequests[otherUserId]
                ?: ArrayList<PendingVerificationRequest>().also {
                    pendingRequests[otherUserId] = it
                }

        val transport = verificationTransportToDeviceFactory.createTransport(null)

        // Cancel existing pending requests?
        requestsForUser.toImmutableList().forEach { existingRequest ->
            existingRequest.transactionId?.let { tid ->
                if (!existingRequest.isFinished) {
                    Timber.d("## SAS, cancelling pending requests to start a new one")
                    updatePendingRequest(existingRequest.copy(cancelConclusion = CancelCode.User))
                    existingRequest.targetDevices?.forEach {
                        transport.cancelTransaction(tid, existingRequest.otherUserId, it, CancelCode.User)
                    }
                }
            }
        }

        val localID = LocalEcho.createLocalEchoId()

        val verificationRequest = PendingVerificationRequest(
                transactionId = localID,
                ageLocalTs = System.currentTimeMillis(),
                isIncoming = false,
                roomId = null,
                localID = localID,
                otherUserId = otherUserId,
                targetDevices = targetDevices
        )

        // We can SCAN or SHOW QR codes only if cross-signing is enabled
        val methodValues = if (crossSigningService.isCrossSigningVerified()) {
            // Add reciprocate method if application declares it can scan or show QR codes
            // Not sure if it ok to do that (?)
            val reciprocateMethod = methods
                    .firstOrNull { it == VerificationMethod.QR_CODE_SCAN || it == VerificationMethod.QR_CODE_SHOW }
                    ?.let { listOf(VERIFICATION_METHOD_RECIPROCATE) }.orEmpty()
            methods.map { it.toValue() } + reciprocateMethod
        } else {
            // Filter out SCAN and SHOW qr code method
            methods
                    .filter { it != VerificationMethod.QR_CODE_SHOW && it != VerificationMethod.QR_CODE_SCAN }
                    .map { it.toValue() }
        }
                .distinct()

        transport.sendVerificationRequest(methodValues, localID, otherUserId, null, targetDevices) { _, info ->
            // Nothing special to do in to device mode
            updatePendingRequest(verificationRequest.copy(
                    // localId stays different
                    requestInfo = info
            ))
        }

        requestsForUser.add(verificationRequest)
        dispatchRequestAdded(verificationRequest)

        return verificationRequest
    }

    override fun declineVerificationRequestInDMs(otherUserId: String, transactionId: String, roomId: String) {
        verificationTransportRoomMessageFactory.createTransport(roomId, null)
                .cancelTransaction(transactionId, otherUserId, null, CancelCode.User)

        getExistingVerificationRequest(otherUserId, transactionId)?.let {
            updatePendingRequest(it.copy(
                    cancelConclusion = CancelCode.User
            ))
        }
    }

    private fun updatePendingRequest(updated: PendingVerificationRequest) {
        val requestsForUser = pendingRequests[updated.otherUserId]
                ?: ArrayList<PendingVerificationRequest>().also {
                    pendingRequests[updated.otherUserId] = it
                }
        val index = requestsForUser.indexOfFirst {
            it.transactionId == updated.transactionId
                    || it.transactionId == null && it.localID == updated.localID
        }
        if (index != -1) {
            requestsForUser.removeAt(index)
        }
        requestsForUser.add(updated)
        dispatchRequestUpdated(updated)
    }

    override fun beginKeyVerificationInDMs(method: VerificationMethod,
                                           transactionId: String,
                                           roomId: String,
                                           otherUserId: String,
                                           otherDeviceId: String,
                                           callback: MatrixCallback<String>?): String? {
        if (method == VerificationMethod.SAS) {
            val tx = DefaultOutgoingSASDefaultVerificationTransaction(
                    setDeviceVerificationAction,
                    userId,
                    deviceId,
                    cryptoStore,
                    crossSigningService,
                    myDeviceInfoHolder.get().myDevice.fingerprint()!!,
                    transactionId,
                    otherUserId,
                    otherDeviceId)
            tx.transport = verificationTransportRoomMessageFactory.createTransport(roomId, tx)
            addTransaction(tx)

            tx.start()
            return transactionId
        } else {
            throw IllegalArgumentException("Unknown verification method")
        }
    }

    override fun readyPendingVerificationInDMs(methods: List<VerificationMethod>,
                                               otherUserId: String,
                                               roomId: String,
                                               transactionId: String): Boolean {
        Timber.v("## SAS readyPendingVerificationInDMs $otherUserId room:$roomId tx:$transactionId")
        // Let's find the related request
        val existingRequest = getExistingVerificationRequest(otherUserId, transactionId)
        if (existingRequest != null) {
            // we need to send a ready event, with matching methods
            val transport = verificationTransportRoomMessageFactory.createTransport(roomId, null)
            val computedMethods = computeReadyMethods(
                    transactionId,
                    otherUserId,
                    existingRequest.requestInfo?.fromDevice ?: "",
                    existingRequest.requestInfo?.methods,
                    methods) {
                verificationTransportRoomMessageFactory.createTransport(roomId, it)
            }
            if (methods.isNullOrEmpty()) {
                Timber.i("Cannot ready this request, no common methods found txId:$transactionId")
                // TODO buttons should not be shown in  this case?
                return false
            }
            // TODO this is not yet related to a transaction, maybe we should use another method like for cancel?
            val readyMsg = transport.createReady(transactionId, deviceId ?: "", computedMethods)
            transport.sendToOther(EventType.KEY_VERIFICATION_READY,
                    readyMsg,
                    VerificationTxState.None,
                    CancelCode.User,
                    null // TODO handle error?
            )
            updatePendingRequest(existingRequest.copy(readyInfo = readyMsg))
            return true
        } else {
            Timber.e("## SAS readyPendingVerificationInDMs Verification not found")
            // :/ should not be possible... unless live observer very slow
            return false
        }
    }

    override fun readyPendingVerification(methods: List<VerificationMethod>,
                                          otherUserId: String,
                                          transactionId: String): Boolean {
        Timber.v("## SAS readyPendingVerification $otherUserId tx:$transactionId")
        // Let's find the related request
        val existingRequest = getExistingVerificationRequest(otherUserId, transactionId)
        if (existingRequest != null) {
            // we need to send a ready event, with matching methods
            val transport = verificationTransportToDeviceFactory.createTransport(null)
            val computedMethods = computeReadyMethods(
                    transactionId,
                    otherUserId,
                    existingRequest.requestInfo?.fromDevice ?: "",
                    existingRequest.requestInfo?.methods,
                    methods) {
                verificationTransportToDeviceFactory.createTransport(it)
            }
            if (methods.isNullOrEmpty()) {
                Timber.i("Cannot ready this request, no common methods found txId:$transactionId")
                // TODO buttons should not be shown in  this case?
                return false
            }
            // TODO this is not yet related to a transaction, maybe we should use another method like for cancel?
            val readyMsg = transport.createReady(transactionId, deviceId ?: "", computedMethods)
            transport.sendVerificationReady(
                    readyMsg,
                    otherUserId,
                    existingRequest.requestInfo?.fromDevice ?: "",
                    null // TODO handle error?
            )
            updatePendingRequest(existingRequest.copy(readyInfo = readyMsg))
            return true
        } else {
            Timber.e("## SAS readyPendingVerification Verification not found")
            // :/ should not be possible... unless live observer very slow
            return false
        }
    }

    private fun computeReadyMethods(
            transactionId: String,
            otherUserId: String,
            otherDeviceId: String,
            otherUserMethods: List<String>?,
            methods: List<VerificationMethod>,
            transportCreator: (DefaultVerificationTransaction) -> VerificationTransport): List<String> {
        if (otherUserMethods.isNullOrEmpty()) {
            return emptyList()
        }

        val result = mutableSetOf<String>()

        if (VERIFICATION_METHOD_SAS in otherUserMethods && VerificationMethod.SAS in methods) {
            // Other can do SAS and so do I
            result.add(VERIFICATION_METHOD_SAS)
        }

        if (VERIFICATION_METHOD_QR_CODE_SCAN in otherUserMethods || VERIFICATION_METHOD_QR_CODE_SHOW in otherUserMethods) {
            // Other user wants to verify using QR code. Cross-signing has to be setup
            val qrCodeData = createQrCodeData(transactionId, otherUserId, otherDeviceId)

            if (qrCodeData != null) {
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

            if (VERIFICATION_METHOD_RECIPROCATE in result) {
                // Create the pending transaction
                val tx = DefaultQrCodeVerificationTransaction(
                        setDeviceVerificationAction,
                        transactionId,
                        otherUserId,
                        otherDeviceId,
                        crossSigningService,
                        cryptoStore,
                        qrCodeData,
                        userId,
                        deviceId ?: "",
                        false)

                tx.transport = transportCreator.invoke(tx)

                addTransaction(tx)
            }
        }

        return result.toList()
    }

    /**
     * This string must be unique for the pair of users performing verification for the duration that the transaction is valid
     */
    private fun createUniqueIDForTransaction(otherUserId: String, otherDeviceID: String): String {
        return buildString {
            append(userId).append("|")
            append(deviceId).append("|")
            append(otherUserId).append("|")
            append(otherDeviceID).append("|")
            append(UUID.randomUUID().toString())
        }
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        dispatchTxUpdated(tx)
        if (tx.state is VerificationTxState.TerminalTxState) {
            // remove
            this.removeTransaction(tx.otherUserId, tx.transactionId)
        }
    }
}
