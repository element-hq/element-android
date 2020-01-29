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
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.QrCodeVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationMethod
import im.vector.matrix.android.api.session.crypto.sas.VerificationService
import im.vector.matrix.android.api.session.crypto.sas.VerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationTxState
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.LiveEvent
import timber.log.Timber

data class VerificationBottomSheetViewState(
        val otherUserMxItem: MatrixItem? = null,
        val roomId: String? = null,
        val pendingRequest: Async<PendingVerificationRequest> = Uninitialized,
        val pendingLocalId: String? = null,
        val sasTransactionState: VerificationTxState? = null,
        val qrTransactionState: VerificationTxState? = null,
        val transactionId: String? = null
) : MvRxState

class VerificationBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: VerificationBottomSheetViewState,
                                                                   private val session: Session)
    : VectorViewModel<VerificationBottomSheetViewState, VerificationAction, EmptyViewEvents>(initialState),
        VerificationService.VerificationListener {

    // Can be used for several actions, for a one shot result
    private val _requestLiveData = MutableLiveData<LiveEvent<Async<VerificationAction>>>()
    val requestLiveData: LiveData<LiveEvent<Async<VerificationAction>>>
        get() = _requestLiveData

    init {
        session.getVerificationService().addListener(this)
    }

    override fun onCleared() {
        session.getVerificationService().removeListener(this)
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

            val pr = session.getVerificationService().getExistingVerificationRequest(args.otherUserId, args.verificationId)

            val sasTx = (pr?.transactionId ?: args.verificationId)?.let {
                session.getVerificationService().getExistingTransaction(args.otherUserId, it) as? SasVerificationTransaction
            }

            val qrTx = (pr?.transactionId ?: args.verificationId)?.let {
                session.getVerificationService().getExistingTransaction(args.otherUserId, it) as? QrCodeVerificationTransaction
            }

            return fragment.verificationViewModelFactory.create(VerificationBottomSheetViewState(
                    otherUserMxItem = userItem?.toMatrixItem(),
                    sasTransactionState = sasTx?.state,
                    qrTransactionState = qrTx?.state,
                    transactionId = args.verificationId,
                    pendingRequest = if (pr != null) Success(pr) else Uninitialized,
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
                if (roomId == null) {
                    val localID = LocalEcho.createLocalEchoId()
                    setState {
                        copy(
                                pendingLocalId = localID,
                                pendingRequest = Loading()
                        )
                    }
                    val roomParams = CreateRoomParams(
                            invitedUserIds = listOf(otherUserId).toMutableList()
                    )
                            .setDirectMessage()
                    session.createRoom(roomParams, object : MatrixCallback<String> {
                        override fun onSuccess(data: String) {
                            setState {
                                copy(
                                        roomId = data,
                                        pendingRequest = Success(
                                                session
                                                        .getVerificationService()
                                                        .requestKeyVerificationInDMs(supportedVerificationMethods, otherUserId, data, pendingLocalId)
                                        )
                                )
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            setState {
                                copy(pendingRequest = Fail(failure))
                            }
                        }
                    })
                } else {
                    setState {
                        copy(
                                pendingRequest = Success(session
                                        .getVerificationService()
                                        .requestKeyVerificationInDMs(supportedVerificationMethods, otherUserId, roomId)
                                )
                        )
                    }
                }
            }
            is VerificationAction.StartSASVerification    -> {
                val request = session.getVerificationService().getExistingVerificationRequest(otherUserId, action.pendingRequestTransactionId)
                        ?: return@withState
                if (roomId == null) return@withState
                val otherDevice = if (request.isIncoming) request.requestInfo?.fromDevice else request.readyInfo?.fromDevice
                session.getVerificationService().beginKeyVerificationInDMs(
                        VerificationMethod.SAS,
                        transactionId = action.pendingRequestTransactionId,
                        roomId = roomId,
                        otherUserId = request.otherUserId,
                        otherDeviceId = otherDevice ?: "",
                        callback = null
                )
            }
            is VerificationAction.RemoteQrCodeScanned     -> {
                val existingTransaction = session.getVerificationService()
                        .getExistingTransaction(action.otherUserId, action.transactionId) as? QrCodeVerificationTransaction
                existingTransaction
                        ?.userHasScannedOtherQrCode(action.scannedData)
                        ?.let { cancelCode ->
                            // Something went wrong
                            Timber.w("## Something is not right: $cancelCode")
                            // TODO
                        }
            }
            is VerificationAction.SASMatchAction          -> {
                (session.getVerificationService()
                        .getExistingTransaction(action.otherUserId, action.sasTransactionId)
                        as? SasVerificationTransaction)?.userHasVerifiedShortCode()
            }
            is VerificationAction.SASDoNotMatchAction     -> {
                (session.getVerificationService()
                        .getExistingTransaction(action.otherUserId, action.sasTransactionId)
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
        when (tx) {
            is SasVerificationTransaction    -> {
                if (tx.transactionId == (state.pendingRequest.invoke()?.transactionId ?: state.transactionId)) {
                    // A SAS tx has been started following this request
                    setState {
                        copy(
                                sasTransactionState = tx.state
                        )
                    }
                }
            }
            is QrCodeVerificationTransaction -> {
                if (tx.transactionId == (state.pendingRequest.invoke()?.transactionId ?: state.transactionId)) {
                    // A QR tx has been started following this request
                    setState {
                        copy(
                                qrTransactionState = tx.state
                        )
                    }
                }
            }
        }
    }

    override fun verificationRequestCreated(pr: PendingVerificationRequest) {
        verificationRequestUpdated(pr)
    }

    override fun verificationRequestUpdated(pr: PendingVerificationRequest) = withState { state ->

        if (pr.localID == state.pendingLocalId
                || pr.localID == state.pendingRequest.invoke()?.localID
                || state.pendingRequest.invoke()?.transactionId == pr.transactionId) {
            setState {
                copy(pendingRequest = Success(pr))
            }
        }
    }
}
