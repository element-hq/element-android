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
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.QRVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationMethod
import im.vector.matrix.android.api.session.crypto.sas.VerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationTxState
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.LiveEvent

data class VerificationBottomSheetViewState(
        val otherUserMxItem: MatrixItem? = null,
        val roomId: String? = null,
        val pendingRequest: PendingVerificationRequest? = null,
        val transactionState: VerificationTxState? = null,
        val transactionId: String? = null,
        val cancelCode: CancelCode? = null
) : MvRxState

class VerificationBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: VerificationBottomSheetViewState,
                                                                   private val session: Session)
    : VectorViewModel<VerificationBottomSheetViewState, VerificationAction>(initialState),
        VerificationService.VerificationListener {

    // Can be used for several actions, for a one shot result
    private val _requestLiveData = MutableLiveData<LiveEvent<Async<VerificationAction>>>()
    val requestLiveData: LiveData<LiveEvent<Async<VerificationAction>>>
        get() = _requestLiveData

    init {
        session.getSasVerificationService().addListener(this)
    }

    override fun onCleared() {
        session.getSasVerificationService().removeListener(this)
        super.onCleared()
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationBottomSheetViewState): VerificationBottomSheetViewModel
    }

    companion object : MvRxViewModelFactory<VerificationBottomSheetViewModel, VerificationBottomSheetViewState> {

        override fun create(viewModelContext: ViewModelContext, state: VerificationBottomSheetViewState): VerificationBottomSheetViewModel? {
            val fragment: VerificationBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            val args: VerificationBottomSheet.VerificationArgs = viewModelContext.args()

            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()

            val userItem = session.getUser(args.otherUserId)

            val pr = session.getSasVerificationService().getExistingVerificationRequest(args.otherUserId, args.verificationId)

            val sasTx = (pr?.transactionId ?: args.verificationId)?.let {
                session.getSasVerificationService().getExistingTransaction(args.otherUserId, it)
            }

            return fragment.verificationViewModelFactory.create(VerificationBottomSheetViewState(
                    otherUserMxItem = userItem?.toMatrixItem(),
                    transactionState = sasTx?.state,
                    transactionId = args.verificationId,
                    pendingRequest = pr,
                    roomId = args.roomId)
            )
        }
    }

    override fun handle(action: VerificationAction) = withState { state ->
        val otherUserId = state.otherUserMxItem?.id ?: return@withState
        val roomId = state.roomId
                ?: session.getExistingDirectRoomWithUser(otherUserId)?.roomId

        when (action) {
            is VerificationAction.RequestVerificationByDM -> {
                if (roomId == null) return@withState
                setState {
                    copy(pendingRequest = session.getSasVerificationService().requestKeyVerificationInDMs(supportedVerificationMethods, otherUserId, roomId))
                }
            }
            is VerificationAction.StartSASVerification    -> {
                val request = session.getSasVerificationService().getExistingVerificationRequest(otherUserId, action.pendingRequestTransactionId)
                        ?: return@withState
                if (roomId == null) return@withState
                val otherDevice = if (request.isIncoming) request.requestInfo?.fromDevice else request.readyInfo?.fromDevice
                session.getSasVerificationService().beginKeyVerificationInDMs(
                        VerificationMethod.SAS,
                        transactionId = action.pendingRequestTransactionId,
                        roomId = roomId,
                        otherUserId = request.otherUserId,
                        otherDeviceId = otherDevice ?: "",
                        callback = null
                )
            }
            is VerificationAction.RemoteQrCodeScanned     -> {
                // TODO Use session.getCrossSigningService()?
                val existingTransaction = session.getSasVerificationService()
                        .getExistingTransaction(action.userID, action.transactionId) as? QRVerificationTransaction
                existingTransaction
                        ?.userHasScannedRemoteQrCode(action.scannedData)
            }
            is VerificationAction.SASMatchAction          -> {
                (session.getSasVerificationService()
                        .getExistingTransaction(action.userID, action.sasTransactionId)
                        as? SasVerificationTransaction)?.userHasVerifiedShortCode()
            }
            is VerificationAction.SASDoNotMatchAction     -> {
                (session.getSasVerificationService()
                        .getExistingTransaction(action.userID, action.sasTransactionId)
                        as? SasVerificationTransaction)
                        ?.shortCodeDoesNotMatch()
            }
            is VerificationAction.GotItConclusion         -> {
                _requestLiveData.postValue(LiveEvent(Success(action)))
            }
        }
    }

    override fun transactionCreated(tx: VerificationTransaction) {
        transactionUpdated(tx)
    }

    override fun transactionUpdated(tx: VerificationTransaction) = withState { state ->
        if (tx.transactionId == (state.pendingRequest?.transactionId ?: state.transactionId)) {
            // A SAS tx has been started following this request
            setState {
                copy(
                        transactionState = tx.state,
                        cancelCode = tx.cancelledReason
                )
            }
        }
    }

    override fun verificationRequestCreated(pr: PendingVerificationRequest) {
        verificationRequestUpdated(pr)
    }

    override fun verificationRequestUpdated(pr: PendingVerificationRequest) = withState { state ->

        if (pr.localID == state.pendingRequest?.localID || state.pendingRequest?.transactionId == pr.transactionId) {
            setState {
                copy(pendingRequest = pr)
            }
        }
    }
}
