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
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationStart
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction
import im.vector.riotx.core.utils.LiveEvent

data class VerificationBottomSheetViewState(
        val otherUserMxItem: MatrixItem? = null,
        val roomId: String? = null,
        val pendingRequest: PendingVerificationRequest? = null,
        val sasTransactionState: SasVerificationTxState? = null,
        val cancelCode: CancelCode? = null
) : MvRxState

sealed class VerificationAction : VectorViewModelAction {
    data class RequestVerificationByDM(val userID: String, val roomId: String?) : VerificationAction()
    data class StartSASVerification(val userID: String, val pendingRequestTransactionId: String) : VerificationAction()
    data class SASMatchAction(val userID: String, val sasTransactionId: String) : VerificationAction()
    data class SASDoNotMatchAction(val userID: String, val sasTransactionId: String) : VerificationAction()
    object GotItConclusion : VerificationAction()
}

class VerificationBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: VerificationBottomSheetViewState,
                                                                   private val session: Session)
    : VectorViewModel<VerificationBottomSheetViewState, VerificationAction>(initialState),
        SasVerificationService.SasVerificationListener {

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

            val sasTx = state.pendingRequest?.transactionId?.let {
                session.getSasVerificationService().getExistingTransaction(args.otherUserId, it)
            }

            val pr = session.getSasVerificationService().getExistingVerificationRequest(args.otherUserId)
                    ?.firstOrNull { it.transactionId == args.verificationId }

            return fragment.verificationViewModelFactory.create(VerificationBottomSheetViewState(
                    otherUserMxItem = userItem?.toMatrixItem(),
                    sasTransactionState = sasTx?.state,
                    pendingRequest = pr,
                    roomId = args.roomId)
            )
        }
    }

    override fun handle(action: VerificationAction) = withState { state ->
        val otherUserId = state.otherUserMxItem?.id ?: return@withState
        val roomId = state.roomId
                ?: session.getExistingDirectRoomWithUser(otherUserId)?.roomId
                ?: return@withState
        when (action) {
            is VerificationAction.RequestVerificationByDM -> {
//                session
                setState {
                    copy(pendingRequest = session.getSasVerificationService().requestKeyVerificationInDMs(otherUserId, roomId))
                }
            }
            is VerificationAction.StartSASVerification    -> {
                val request = session.getSasVerificationService().getExistingVerificationRequest(otherUserId)
                        ?.firstOrNull { it.transactionId == action.pendingRequestTransactionId }
                        ?: return@withState

                val otherDevice = if (request.isIncoming) request.requestInfo?.fromDevice else request.readyInfo?.fromDevice
                session.getSasVerificationService().beginKeyVerificationInDMs(
                        KeyVerificationStart.VERIF_METHOD_SAS,
                        transactionId = action.pendingRequestTransactionId,
                        roomId = roomId,
                        otherUserId = request.otherUserId,
                        otherDeviceId = otherDevice ?: "",
                        callback = null
                )
            }
            is VerificationAction.SASMatchAction          -> {
                session.getSasVerificationService()
                        .getExistingTransaction(action.userID, action.sasTransactionId)
                        ?.userHasVerifiedShortCode()
            }
            is VerificationAction.SASDoNotMatchAction     -> {
                session.getSasVerificationService()
                        .getExistingTransaction(action.userID, action.sasTransactionId)
                        ?.shortCodeDoNotMatch()
            }
            is VerificationAction.GotItConclusion         -> {
                _requestLiveData.postValue(LiveEvent(Success(action)))
            }
        }
    }

    override fun transactionCreated(tx: SasVerificationTransaction) {
        transactionUpdated(tx)
    }

    override fun transactionUpdated(tx: SasVerificationTransaction) = withState { state ->
        if (tx.transactionId == state.pendingRequest?.transactionId) {
            // A SAS tx has been started following this request
            setState {
                copy(
                        sasTransactionState = tx.state,
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
