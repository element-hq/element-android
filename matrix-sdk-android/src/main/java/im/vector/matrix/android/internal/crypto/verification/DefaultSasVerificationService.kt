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
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.session.crypto.sas.safeValueOf
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MyDeviceInfoHolder
import im.vector.matrix.android.internal.crypto.actions.SetDeviceVerificationAction
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.*
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.DefaultRequestVerificationDMTask
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.send.SendResponse
import im.vector.matrix.android.internal.task.TaskConstraints
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

@SessionScope
internal class DefaultSasVerificationService @Inject constructor(
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val myDeviceInfoHolder: Lazy<MyDeviceInfoHolder>,
        private val deviceListManager: DeviceListManager,
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        private val requestVerificationDMTask: DefaultRequestVerificationDMTask,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sasTransportRoomMessageFactory: SasTransportRoomMessageFactory,
        private val sasTransportToDeviceFactory: SasTransportToDeviceFactory,
        private val taskExecutor: TaskExecutor
) : VerificationTransaction.Listener, SasVerificationService {

    private val uiHandler = Handler(Looper.getMainLooper())

    // Cannot be injected in constructor as it creates a dependency cycle
    lateinit var cryptoService: CryptoService

    // map [sender : [transaction]]
    private val txMap = HashMap<String, HashMap<String, VerificationTransaction>>()

    /**
     * Map [sender: [PendingVerificationRequest]]
     */
    private val pendingRequests = HashMap<String, ArrayList<PendingVerificationRequest>>()


    // Event received from the sync
    fun onToDeviceEvent(event: Event) {
        GlobalScope.launch(coroutineDispatchers.crypto) {
            when (event.getClearType()) {
                EventType.KEY_VERIFICATION_START  -> {
                    onStartRequestReceived(event)
                }
                EventType.KEY_VERIFICATION_CANCEL -> {
                    onCancelReceived(event)
                }
                EventType.KEY_VERIFICATION_ACCEPT -> {
                    onAcceptReceived(event)
                }
                EventType.KEY_VERIFICATION_KEY    -> {
                    onKeyReceived(event)
                }
                EventType.KEY_VERIFICATION_MAC    -> {
                    onMacReceived(event)
                }
                else                              -> {
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
                    // TODO?
                }
                EventType.MESSAGE                 -> {
                    if (MessageType.MSGTYPE_VERIFICATION_REQUEST == event.getClearContent().toModel<MessageContent>()?.type) {
                        onRoomRequestReceived(event)
                    }
                }
                else                              -> {
                    // ignore
                }
            }
        }
    }

    private var listeners = ArrayList<SasVerificationService.SasVerificationListener>()

    override fun addListener(listener: SasVerificationService.SasVerificationListener) {
        uiHandler.post {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    override fun removeListener(listener: SasVerificationService.SasVerificationListener) {
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
        setDeviceVerificationAction.handle(MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED,
                deviceID,
                userId)

        listeners.forEach {
            try {
                it.markedAsManuallyVerified(userId, deviceID)
            } catch (e: Throwable) {
                Timber.e(e, "## Error while notifying listeners")
            }
        }
    }

    suspend fun onRoomRequestReceived(event: Event) {
        Timber.v("## SAS Verification request from ${event.senderId} in room ${event.roomId}")
        val requestInfo = event.getClearContent().toModel<MessageVerificationRequestContent>()
                ?: return
        val senderId = event.senderId ?: return

        if (requestInfo.toUserId != credentials.userId) {
            //I should ignore this, it's not for me
            Timber.w("## SAS Verification ignoring request from ${event.senderId}, not sent to me")
            return
        }

        if(checkKeysAreDownloaded(senderId, requestInfo.fromDevice) == null) {
            //I should ignore this, it's not for me
            Timber.e("## SAS Verification device ${requestInfo.fromDevice} is not knwon")
            // TODO cancel?
            return
        }

        // Remember this request
        val requestsForUser = pendingRequests[senderId]
                ?: ArrayList<PendingVerificationRequest>().also {
                    pendingRequests[event.senderId] = it
                }

        val pendingVerificationRequest = PendingVerificationRequest(
                isIncoming = true,
                otherUserId = senderId,//requestInfo.toUserId,
                transactionId = event.eventId,
                requestInfo = requestInfo
        )
        requestsForUser.add(pendingVerificationRequest)

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
                sasTransportRoomMessageFactory.createTransport(event.roomId
                        ?: "", cryptoService, null).cancelTransaction(
                        startReq.transactionID ?: "",
                        otherUserId!!,
                        startReq.fromDevice ?: event.getSenderKey()!!,
                        CancelCode.UnknownMethod
                )
            }
            return
        }

        handleStart(otherUserId, startReq as VerificationInfoStart) {
            it.transport = sasTransportRoomMessageFactory.createTransport(event.roomId
                    ?: "", cryptoService, it)
        }?.let {
            sasTransportRoomMessageFactory.createTransport(event.roomId
                    ?: "", cryptoService, null).cancelTransaction(
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
                sasTransportToDeviceFactory.createTransport(null).cancelTransaction(
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
            it.transport = sasTransportToDeviceFactory.createTransport(it)
        }?.let {
            sasTransportToDeviceFactory.createTransport(null).cancelTransaction(
                    startReq.transactionID ?: "",
                    otherUserId!!,
                    startReq.fromDevice ?: event.getSenderKey()!!,
                    it
            )
        }
    }

    private suspend fun handleStart(otherUserId: String?, startReq: VerificationInfoStart, txConfigure: (SASVerificationTransaction) -> Unit): CancelCode? {
        Timber.d("## SAS onStartRequestReceived ${startReq.transactionID!!}")
        if (checkKeysAreDownloaded(otherUserId!!, startReq.fromDevice ?: "") != null) {
            Timber.v("## SAS onStartRequestReceived $startReq")
            val tid = startReq.transactionID!!
            val existing = getExistingTransaction(otherUserId, tid)
            val existingTxs = getExistingTransactionsForUser(otherUserId)
            if (existing != null) {
                // should cancel both!
                Timber.v("## SAS onStartRequestReceived - Request exist with same if ${startReq.transactionID!!}")
                existing.cancel(CancelCode.UnexpectedMessage)
                return CancelCode.UnexpectedMessage
                // cancelTransaction(tid, otherUserId, startReq.fromDevice!!, CancelCode.UnexpectedMessage)
            } else if (existingTxs?.isEmpty() == false) {
                Timber.v("## SAS onStartRequestReceived - There is already a transaction with this user ${startReq.transactionID!!}")
                // Multiple keyshares between two devices: any two devices may only have at most one key verification in flight at a time.
                existingTxs.forEach {
                    it.cancel(CancelCode.UnexpectedMessage)
                }
                return CancelCode.UnexpectedMessage
                // cancelTransaction(tid, otherUserId, startReq.fromDevice!!, CancelCode.UnexpectedMessage)
            } else {
                // Ok we can create
                if (KeyVerificationStart.VERIF_METHOD_SAS == startReq.method) {
                    Timber.v("## SAS onStartRequestReceived - request accepted ${startReq.transactionID!!}")
                    // If there is a corresponding request, we can auto accept
                    // as we are the one requesting in first place (or we accepted the request)
                    val autoAccept = getExistingVerificationRequest(otherUserId)?.any { it.transactionId == startReq.transactionID }
                            ?: false
                    val tx = DefaultIncomingSASVerificationTransaction(
//                            this,
                            setDeviceVerificationAction,
                            credentials,
                            cryptoStore,
                            myDeviceInfoHolder.get().myDevice.fingerprint()!!,
                            startReq.transactionID!!,
                            otherUserId,
                            autoAccept).also { txConfigure(it) }
                    addTransaction(tx)
                    tx.acceptVerificationEvent(otherUserId, startReq)
                } else {
                    Timber.e("## SAS onStartRequestReceived - unknown method ${startReq.method}")
                    return CancelCode.UnknownMethod
                    // cancelTransaction(tid, otherUserId, startReq.fromDevice
//                            ?: event.getSenderKey()!!, CancelCode.UnknownMethod)
                }
            }
        } else {
            return CancelCode.UnexpectedMessage
//            cancelTransaction(startReq.transactionID!!, otherUserId, startReq.fromDevice!!, CancelCode.UnexpectedMessage)
        }
        return null
    }

    private suspend fun checkKeysAreDownloaded(otherUserId: String,
                                               fromDevice: String): MXUsersDevicesMap<MXDeviceInfo>? {
        return try {
            val keys = deviceListManager.downloadKeys(listOf(otherUserId), true)
            val deviceIds = keys.getUserDeviceIds(otherUserId) ?: return null
            keys.takeIf { deviceIds.contains(fromDevice) }
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
            updateOutgoingPendingRequest(it.copy(cancelConclusion = safeValueOf(cancelReq.code)))
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
        val existing = getExistingTransaction(otherUserId, cancelReq.transactionID!!)
        if (existing == null) {
            Timber.e("## Received invalid cancel request")
            return
        }
        if (existing is SASVerificationTransaction) {
            existing.cancelledReason = safeValueOf(cancelReq.code)
            existing.state = SasVerificationTxState.OnCancelled
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

        if (existing is SASVerificationTransaction) {
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
        if (existing is SASVerificationTransaction) {
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
        if(checkKeysAreDownloaded(event.senderId, readyReq.fromDevice ?: "") == null) {
            Timber.e("## SAS Verification device ${readyReq.fromDevice} is not knwown")
            // TODO cancel?
            return
        }


        handleReadyReceived(event.senderId, readyReq)
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
        if (existing is SASVerificationTransaction) {
            existing.acceptVerificationEvent(senderId, macReq)
        } else {
            // not other types known for now
        }
    }

    private fun handleReadyReceived(senderId: String, readyReq: VerificationInfoReady) {
        val existingRequest = getExistingVerificationRequest(senderId)?.find { it.transactionId == readyReq.transactionID }
        if (existingRequest == null) {
            Timber.e("## SAS Received Ready for unknown request txId:${readyReq.transactionID} fromDevice ${readyReq.fromDevice}")
            return
        }
        updateOutgoingPendingRequest(existingRequest.copy(readyInfo = readyReq))
    }

    override fun getExistingTransaction(otherUser: String, tid: String): VerificationTransaction? {
        synchronized(lock = txMap) {
            return txMap[otherUser]?.get(tid)
        }
    }

    override fun getExistingVerificationRequest(otherUser: String): List<PendingVerificationRequest>? {
        synchronized(lock = pendingRequests) {
            return pendingRequests[otherUser]
        }
    }

    override fun getExistingVerificationRequest(otherUser: String, tid: String?): PendingVerificationRequest? {
        synchronized(lock = pendingRequests) {
            return tid?.let { tid -> pendingRequests[otherUser]?.firstOrNull { it.transactionId == tid } }
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

    private fun addTransaction(tx: VerificationTransaction) {
        synchronized(txMap) {
            val txInnerMap = txMap.getOrPut(tx.otherUserId) { HashMap() }
            txInnerMap[tx.transactionId] = tx
            dispatchTxAdded(tx)
            tx.addListener(this)
        }
    }

    override fun beginKeyVerificationSAS(userId: String, deviceID: String): String? {
        return beginKeyVerification(KeyVerificationStart.VERIF_METHOD_SAS, userId, deviceID)
    }

    override fun beginKeyVerification(method: String, userId: String, deviceID: String): String? {
        val txID = createUniqueIDForTransaction(userId, deviceID)
        // should check if already one (and cancel it)
        if (KeyVerificationStart.VERIF_METHOD_SAS == method) {
            val tx = DefaultOutgoingSASVerificationRequest(
                    setDeviceVerificationAction,
                    credentials,
                    cryptoStore,
                    myDeviceInfoHolder.get().myDevice.fingerprint()!!,
                    txID,
                    userId,
                    deviceID)
            tx.transport = sasTransportToDeviceFactory.createTransport(tx)
            addTransaction(tx)

            tx.start()
            return txID
        } else {
            throw IllegalArgumentException("Unknown verification method")
        }
    }

    override fun requestKeyVerificationInDMs(userId: String, roomId: String, callback: MatrixCallback<String>?)
            : PendingVerificationRequest {

        Timber.i("## SAS Requesting verification to user: $userId in room ${roomId}")
        val requestsForUser = pendingRequests[userId]
                ?: ArrayList<PendingVerificationRequest>().also {
                    pendingRequests[userId] = it
                }

        val params = requestVerificationDMTask.createParamsAndLocalEcho(
                roomId = roomId,
                from = credentials.deviceId ?: "",
                methods = listOf(KeyVerificationStart.VERIF_METHOD_SAS),
                to = userId,
                cryptoService = cryptoService
        )
        val verificationRequest = PendingVerificationRequest(
                isIncoming = false,
                localID = params.event.eventId ?: "",
                otherUserId = userId
        )
        requestsForUser.add(verificationRequest)
        dispatchRequestAdded(verificationRequest)

        requestVerificationDMTask.configureWith(
                params
        ) {
            this.callback = object : MatrixCallback<SendResponse> {
                override fun onSuccess(data: SendResponse) {
                    params.event.getClearContent().toModel<MessageVerificationRequestContent>()?.let {
                        updateOutgoingPendingRequest(verificationRequest.copy(
                                transactionId = data.eventId,
                                requestInfo = it
                        ))
                    }
                    callback?.onSuccess(data.eventId)
                }

                override fun onFailure(failure: Throwable) {
                    callback?.onFailure(failure)
                }
            }
            constraints = TaskConstraints(true)
            retryCount = 3
        }.executeBy(taskExecutor)

        return verificationRequest
    }

    private fun updateOutgoingPendingRequest(updated: PendingVerificationRequest) {
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

    override fun beginKeyVerificationInDMs(method: String, transactionId: String, roomId: String,
                                           otherUserId: String, otherDeviceId: String,
                                           callback: MatrixCallback<String>?): String? {
        if (KeyVerificationStart.VERIF_METHOD_SAS == method) {
            val tx = DefaultOutgoingSASVerificationRequest(
                    setDeviceVerificationAction,
                    credentials,
                    cryptoStore,
                    myDeviceInfoHolder.get().myDevice.fingerprint()!!,
                    transactionId,
                    otherUserId,
                    otherDeviceId)
            tx.transport = sasTransportRoomMessageFactory.createTransport(roomId, cryptoService, tx)
            addTransaction(tx)

            tx.start()
            return transactionId
        } else {
            throw IllegalArgumentException("Unknown verification method")
        }
    }

    override fun readyPendingVerificationInDMs(otherUserId: String, roomId: String, transactionId: String) {
        // Let's find the related request
        getExistingVerificationRequest(otherUserId)?.find { it.transactionId == transactionId }?.let {
            //we need to send a ready event, with matching methods
            val transport = sasTransportRoomMessageFactory.createTransport(roomId, cryptoService, null)
            val methods = it.requestInfo?.methods?.intersect(listOf(KeyVerificationStart.VERIF_METHOD_SAS))?.toList()
            if (methods.isNullOrEmpty()) {
                Timber.i("Cannot ready this request, no common methods found txId:$transactionId")
                return@let
            }
            //TODO this is not yet related to a transaction, maybe we should use another method like for cancel?
            val readyMsg = transport.createReady(transactionId, credentials.deviceId ?: "", methods)
            transport.sendToOther(EventType.KEY_VERIFICATION_READY, readyMsg,
                    SasVerificationTxState.None,
                    CancelCode.User,
                    null // TODO handle error?
            )
            updateOutgoingPendingRequest(it.copy(readyInfo = readyMsg))
        }
    }

    /**
     * This string must be unique for the pair of users performing verification for the duration that the transaction is valid
     */
    private fun createUniqueIDForTransaction(userId: String, deviceID: String): String {
        return buildString {
            append(credentials.userId).append("|")
            append(credentials.deviceId).append("|")
            append(userId).append("|")
            append(deviceID).append("|")
            append(UUID.randomUUID().toString())
        }
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        dispatchTxUpdated(tx)
        if (tx is SASVerificationTransaction
                && (tx.state == SasVerificationTxState.Cancelled
                        || tx.state == SasVerificationTxState.OnCancelled
                        || tx.state == SasVerificationTxState.Verified)
        ) {
            // remove
            this.removeTransaction(tx.otherUserId, tx.transactionId)
        }
    }
//
//    fun cancelTransaction(transactionId: String, userId: String, userDevice: String, code: CancelCode, roomId: String? = null) {
//        val cancelMessage = KeyVerificationCancel.create(transactionId, code)
//        val contentMap = MXUsersDevicesMap<Any>()
//        contentMap.setObject(userId, userDevice, cancelMessage)
//
//        if (roomId != null) {
//
//        } else {
//            sendToDeviceTask
//                    .configureWith(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_CANCEL, contentMap, transactionId)) {
//                        this.callback = object : MatrixCallback<Unit> {
//                            override fun onSuccess(data: Unit) {
//                                Timber.v("## SAS verification [$transactionId] canceled for reason ${code.value}")
//                            }
//
//                            override fun onFailure(failure: Throwable) {
//                                Timber.e(failure, "## SAS verification [$transactionId] failed to cancel.")
//                            }
//                        }
//                    }
//                    .executeBy(taskExecutor)
//        }
//    }
}
