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
package im.vector.riotx.features.crypto.verification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.IncomingSasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotx.core.utils.LiveEvent
import javax.inject.Inject

// TODO Deprecated("replaced by bottomsheet UX")
class SasVerificationViewModel @Inject constructor() : ViewModel(),
        SasVerificationService.SasVerificationListener {

    companion object {
        const val NAVIGATE_FINISH = "NAVIGATE_FINISH"
        const val NAVIGATE_FINISH_SUCCESS = "NAVIGATE_FINISH_SUCCESS"
        const val NAVIGATE_SAS_DISPLAY = "NAVIGATE_SAS_DISPLAY"
        const val NAVIGATE_SUCCESS = "NAVIGATE_SUCCESS"
        const val NAVIGATE_CANCELLED = "NAVIGATE_CANCELLED"
    }

    private lateinit var sasVerificationService: SasVerificationService

    var otherUserId: String? = null
    var otherDeviceId: String? = null
    var otherUser: User? = null
    var transaction: SasVerificationTransaction? = null

    var transactionState: MutableLiveData<SasVerificationTxState> = MutableLiveData()

    init {
        // Force a first observe
        transactionState.value = null
    }

    private var _navigateEvent: MutableLiveData<LiveEvent<String>> = MutableLiveData()
    val navigateEvent: LiveData<LiveEvent<String>>
        get() = _navigateEvent

    var loadingLiveEvent: MutableLiveData<Int> = MutableLiveData()

    var transactionID: String? = null
        set(value) {
            if (value != null) {
                transaction = sasVerificationService.getExistingTransaction(otherUserId!!, value)
                transactionState.value = transaction?.state
                otherDeviceId = transaction?.otherDeviceId
            }
            field = value
        }

    fun initIncoming(session: Session, otherUserId: String, transactionID: String?) {
        this.sasVerificationService = session.getSasVerificationService()
        this.otherUserId = otherUserId
        this.transactionID = transactionID
        this.sasVerificationService.addListener(this)
        this.otherUser = session.getUser(otherUserId)
        if (transactionID == null || transaction == null) {
            // sanity, this transaction is not known anymore
            _navigateEvent.value = LiveEvent(NAVIGATE_FINISH)
        }
    }

    fun initOutgoing(session: Session, otherUserId: String, otherDeviceId: String) {
        this.sasVerificationService = session.getSasVerificationService()
        this.otherUserId = otherUserId
        this.otherDeviceId = otherDeviceId
        this.sasVerificationService.addListener(this)
        this.otherUser = session.getUser(otherUserId)
    }

    fun beginSasKeyVerification() {
        val verificationSAS = sasVerificationService.beginKeyVerificationSAS(otherUserId!!, otherDeviceId!!)
        this.transactionID = verificationSAS
    }

    override fun transactionCreated(tx: SasVerificationTransaction) {
    }

    override fun transactionUpdated(tx: SasVerificationTransaction) {
        if (transactionID == tx.transactionId) {
            transactionState.value = tx.state
        }
    }

    override fun markedAsManuallyVerified(userId: String, deviceId: String) {
    }

    fun cancelTransaction() {
        transaction?.cancel()
        _navigateEvent.value = LiveEvent(NAVIGATE_FINISH)
    }

    fun finishSuccess() {
        _navigateEvent.value = LiveEvent(NAVIGATE_FINISH_SUCCESS)
    }

    fun manuallyVerified() {
        if (otherUserId != null && otherDeviceId != null) {
            sasVerificationService.markedLocallyAsManuallyVerified(otherUserId!!, otherDeviceId!!)
        }
        _navigateEvent.value = LiveEvent(NAVIGATE_FINISH_SUCCESS)
    }

    fun acceptTransaction() {
        (transaction as? IncomingSasVerificationTransaction)?.performAccept()
    }

    fun confirmEmojiSame() {
        transaction?.userHasVerifiedShortCode()
    }

    fun shortCodeReady() {
        loadingLiveEvent.value = null
        _navigateEvent.value = LiveEvent(NAVIGATE_SAS_DISPLAY)
    }

    fun deviceIsVerified() {
        loadingLiveEvent.value = null
        _navigateEvent.value = LiveEvent(NAVIGATE_SUCCESS)
    }

    fun navigateCancel() {
        _navigateEvent.value = LiveEvent(NAVIGATE_CANCELLED)
    }

    override fun onCleared() {
        super.onCleared()
        if (::sasVerificationService.isInitialized) {
            sasVerificationService.removeListener(this)
        }
    }
}
