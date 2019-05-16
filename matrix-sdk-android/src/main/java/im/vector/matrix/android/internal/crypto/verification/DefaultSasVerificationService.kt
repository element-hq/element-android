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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.session.crypto.sas.safeValueOf
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.CryptoAsyncHelper
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.*
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

/**
 * Manages all current verifications transactions with short codes.
 * Short codes interactive verification is a more user friendly way of verifying devices
 * that is still maintaining a good level of security (alternative to the 43-character strings compare method).
 */
internal class DefaultSasVerificationService(private val mCredentials: Credentials,
                                             private val mCryptoStore: IMXCryptoStore,
                                             private val deviceListManager: DeviceListManager,
                                             private val mSendToDeviceTask: SendToDeviceTask,
                                             private val mTaskExecutor: TaskExecutor)
    : VerificationTransaction.Listener, SasVerificationService {

    private val uiHandler = Handler(Looper.getMainLooper())

    // map [sender : [transaction]]
    private val txMap = HashMap<String, HashMap<String, VerificationTransaction>>()

    // Event received from the sync
    fun onToDeviceEvent(event: Event) {
        CryptoAsyncHelper.getDecryptBackgroundHandler().post { // TODO We are already in a BG thread
            when (event.type) {
                EventType.KEY_VERIFICATION_START -> {
                    onStartRequestReceived(event)
                }
                EventType.KEY_VERIFICATION_CANCEL -> {
                    onCancelReceived(event)
                }
                EventType.KEY_VERIFICATION_ACCEPT -> {
                    onAcceptReceived(event)
                }
                EventType.KEY_VERIFICATION_KEY -> {
                    onKeyReceived(event)
                }
                EventType.KEY_VERIFICATION_MAC -> {
                    onMacReceived(event)
                }
                else -> {
                    //ignore
                }
            }
        }
    }

    // Internal listener
    private lateinit var mCryptoListener: SasCryptoListener


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

    override fun markedLocallyAsManuallyVerified(userId: String, deviceID: String) {
        mCryptoListener.setDeviceVerification(MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED,
                deviceID,
                userId,
                object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        uiHandler.post {
                            listeners.forEach {
                                try {
                                    it.markedAsManuallyVerified(userId, deviceID)
                                } catch (e: Throwable) {
                                    Timber.e(e, "## Error while notifying listeners")
                                }
                            }
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## Manual verification failed in state")
                    }
                })
    }

    private fun onStartRequestReceived(event: Event) {
        val startReq = event.content.toModel<KeyVerificationStart>()!!

        val otherUserId = event.sender
        if (!startReq.isValid()) {
            Timber.e("## received invalid verification request")
            if (startReq.transactionID != null) {
                cancelTransaction(
                        startReq.transactionID!!,
                        otherUserId!!,
                        startReq?.fromDevice ?: event.getSenderKey()!!,
                        CancelCode.UnknownMethod
                )
            }
            return
        }
        //Download device keys prior to everything
        checkKeysAreDownloaded(
                otherUserId!!,
                startReq,
                success = {
                    Timber.d("## SAS onStartRequestReceived ${startReq.transactionID!!}")
                    val tid = startReq.transactionID!!
                    val existing = getExistingTransaction(otherUserId, tid)
                    val existingTxs = getExistingTransactionsForUser(otherUserId)
                    if (existing != null) {
                        //should cancel both!
                        Timber.d("## SAS onStartRequestReceived - Request exist with same if ${startReq.transactionID!!}")
                        existing.cancel(CancelCode.UnexpectedMessage)
                        cancelTransaction(tid, otherUserId, startReq.fromDevice!!, CancelCode.UnexpectedMessage)
                    } else if (existingTxs?.isEmpty() == false) {
                        Timber.d("## SAS onStartRequestReceived - There is already a transaction with this user ${startReq.transactionID!!}")
                        //Multiple keyshares between two devices: any two devices may only have at most one key verification in flight at a time.
                        existingTxs.forEach {
                            it.cancel(CancelCode.UnexpectedMessage)
                        }
                        cancelTransaction(tid, otherUserId, startReq.fromDevice!!, CancelCode.UnexpectedMessage)
                    } else {
                        //Ok we can create
                        if (KeyVerificationStart.VERIF_METHOD_SAS == startReq.method) {
                            Timber.d("## SAS onStartRequestReceived - request accepted ${startReq.transactionID!!}")
                            val tx = IncomingSASVerificationTransaction(
                                    this,
                                    mCredentials,
                                    mCryptoStore,
                                    mSendToDeviceTask,
                                    mTaskExecutor,
                                    mCryptoListener.getMyDevice().fingerprint()!!,
                                    startReq.transactionID!!,
                                    otherUserId)
                            addTransaction(tx)
                            tx.acceptToDeviceEvent(otherUserId, startReq)
                        } else {
                            Timber.e("## SAS onStartRequestReceived - unknown method ${startReq.method}")
                            cancelTransaction(tid, otherUserId, startReq.fromDevice
                                    ?: event.getSenderKey()!!, CancelCode.UnknownMethod)
                        }
                    }
                },
                error = {
                    cancelTransaction(startReq.transactionID!!, otherUserId, startReq.fromDevice!!, CancelCode.UnexpectedMessage)
                })
    }

    private fun checkKeysAreDownloaded(otherUserId: String,
                                       startReq: KeyVerificationStart,
                                       success: (MXUsersDevicesMap<MXDeviceInfo>) -> Unit,
                                       error: () -> Unit) {
        deviceListManager.downloadKeys(listOf(otherUserId), true, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {
            override fun onFailure(failure: Throwable) {
                CryptoAsyncHelper.getDecryptBackgroundHandler().post {
                    error()
                }
            }

            override fun onSuccess(data: MXUsersDevicesMap<MXDeviceInfo>) {
                CryptoAsyncHelper.getDecryptBackgroundHandler().post {
                    if (data.getUserDeviceIds(otherUserId).contains(startReq.fromDevice)) {
                        success(data)
                    } else {
                        error()
                    }
                }
            }
        })
    }

    private fun onCancelReceived(event: Event) {
        Timber.d("## SAS onCancelReceived")
        val cancelReq = event.content.toModel<KeyVerificationCancel>()!!

        if (!cancelReq.isValid()) {
            //ignore
            Timber.e("## Received invalid accept request")
            return
        }
        val otherUserId = event.sender!!

        Timber.d("## SAS onCancelReceived otherUser:$otherUserId reason:${cancelReq.reason}")
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

    private fun onAcceptReceived(event: Event) {
        val acceptReq = event.content.toModel<KeyVerificationAccept>()!!

        if (!acceptReq.isValid()) {
            //ignore
            Timber.e("## Received invalid accept request")
            return
        }
        val otherUserId = event.sender!!
        val existing = getExistingTransaction(otherUserId, acceptReq.transactionID!!)
        if (existing == null) {
            Timber.e("## Received invalid accept request")
            return

        }

        if (existing is SASVerificationTransaction) {
            existing.acceptToDeviceEvent(otherUserId, acceptReq)
        } else {
            //not other types now
        }
    }


    private fun onKeyReceived(event: Event) {
        val keyReq = event.content.toModel<KeyVerificationKey>()!!

        if (!keyReq.isValid()) {
            //ignore
            Timber.e("## Received invalid key request")
            return
        }
        val otherUserId = event.sender!!
        val existing = getExistingTransaction(otherUserId, keyReq.transactionID!!)
        if (existing == null) {
            Timber.e("## Received invalid accept request")
            return
        }
        if (existing is SASVerificationTransaction) {
            existing.acceptToDeviceEvent(otherUserId, keyReq)
        } else {
            //not other types now
        }
    }

    private fun onMacReceived(event: Event) {
        val macReq = event.content.toModel<KeyVerificationMac>()!!

        if (!macReq.isValid()) {
            //ignore
            Timber.e("## Received invalid key request")
            return
        }
        val otherUserId = event.sender!!
        val existing = getExistingTransaction(otherUserId, macReq.transactionID!!)
        if (existing == null) {
            Timber.e("## Received invalid accept request")
            return
        }
        if (existing is SASVerificationTransaction) {
            existing.acceptToDeviceEvent(otherUserId, macReq)
        } else {
            //not other types known for now
        }
    }

    override fun getExistingTransaction(otherUser: String, tid: String): VerificationTransaction? {
        synchronized(lock = txMap) {
            return txMap[otherUser]?.get(tid)
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
        tx.otherUserId.let { otherUserId ->
            synchronized(txMap) {
                if (txMap[otherUserId] == null) {
                    txMap[otherUserId] = HashMap()
                }
                txMap[otherUserId]?.set(tx.transactionId, tx)
                dispatchTxAdded(tx)
                tx.addListener(this)
            }
        }
    }

    override fun beginKeyVerificationSAS(userId: String, deviceID: String): String? {
        return beginKeyVerification(KeyVerificationStart.VERIF_METHOD_SAS, userId, deviceID)
    }

    override fun beginKeyVerification(method: String, userId: String, deviceID: String): String? {
        val txID = createUniqueIDForTransaction(userId, deviceID)
        //should check if already one (and cancel it)
        if (KeyVerificationStart.VERIF_METHOD_SAS == method) {
            val tx = OutgoingSASVerificationRequest(
                    this,
                    mCredentials,
                    mCryptoStore,
                    mSendToDeviceTask,
                    mTaskExecutor,
                    mCryptoListener.getMyDevice().fingerprint()!!,
                    txID,
                    userId,
                    deviceID)
            addTransaction(tx)
            CryptoAsyncHelper.getDecryptBackgroundHandler().post {
                tx.start()
            }
            return txID
        } else {
            throw IllegalArgumentException("Unknown verification method")
        }
    }

    /**
     * This string must be unique for the pair of users performing verification for the duration that the transaction is valid
     */
    private fun createUniqueIDForTransaction(userId: String, deviceID: String): String {
        val buff = StringBuffer()
        buff
                .append(mCredentials.userId).append("|")
                .append(mCredentials.deviceId).append("|")
                .append(userId).append("|")
                .append(deviceID).append("|")
                .append(UUID.randomUUID().toString())
        return buff.toString()
    }


    override fun transactionUpdated(tx: VerificationTransaction) {
        dispatchTxUpdated(tx)
        if (tx is SASVerificationTransaction
                && (tx.state == SasVerificationTxState.Cancelled
                        || tx.state == SasVerificationTxState.OnCancelled
                        || tx.state == SasVerificationTxState.Verified)
        ) {
            //remove
            this.removeTransaction(tx.otherUserId, tx.transactionId)
        }
    }

    fun cancelTransaction(transactionId: String, userId: String, userDevice: String, code: CancelCode) {
        val cancelMessage = KeyVerificationCancel.create(transactionId, code)
        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(cancelMessage, userId, userDevice)

        mSendToDeviceTask.configureWith(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_CANCEL, contentMap, transactionId))
                .dispatchTo(object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        Timber.d("## SAS verification [$transactionId] canceled for reason ${code.value}")
                    }

                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## SAS verification [$transactionId] failed to cancel.")
                    }
                })
                .executeBy(mTaskExecutor)
    }

    fun setCryptoInternalListener(listener: SasCryptoListener) {
        mCryptoListener = listener
    }

    fun setDeviceVerification(verificationStatus: Int, deviceId: String, userId: String, callback: MatrixCallback<Unit>) {
        mCryptoListener.setDeviceVerification(verificationStatus, deviceId, userId, callback)
    }

    interface SasCryptoListener {
        fun setDeviceVerification(verificationStatus: Int, deviceId: String, userId: String, callback: MatrixCallback<Unit>)
        fun getMyDevice(): MXDeviceInfo
    }
}