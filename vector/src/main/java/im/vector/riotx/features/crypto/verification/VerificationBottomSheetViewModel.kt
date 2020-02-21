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
import im.vector.matrix.android.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.sas.IncomingSasVerificationTransaction
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
import im.vector.matrix.android.internal.crypto.crosssigning.fromBase64NoPadding
import im.vector.matrix.android.internal.crypto.crosssigning.isVerified
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel

data class VerificationBottomSheetViewState(
        val otherUserMxItem: MatrixItem? = null,
        val roomId: String? = null,
        val pendingRequest: Async<PendingVerificationRequest> = Uninitialized,
        val pendingLocalId: String? = null,
        val sasTransactionState: VerificationTxState? = null,
        val qrTransactionState: VerificationTxState? = null,
        val transactionId: String? = null,
        // true when we display the loading and we wait for the other (incoming request)
        val selfVerificationMode: Boolean = false,
        val verifiedFromPrivateKeys: Boolean = false,
        val isMe: Boolean = false
) : MvRxState

class VerificationBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: VerificationBottomSheetViewState,
                                                                   @Assisted args: VerificationBottomSheet.VerificationArgs,
                                                                   private val session: Session)
    : VectorViewModel<VerificationBottomSheetViewState, VerificationAction, VerificationBottomSheetViewEvents>(initialState),
        VerificationService.Listener {

    init {
        session.cryptoService().verificationService().addListener(this)

        val userItem = session.getUser(args.otherUserId)

        val selfVerificationMode = args.selfVerificationMode

        var autoReady = false
        val pr = if (selfVerificationMode) {
            // See if active tx for this user and take it

            session.cryptoService().verificationService().getExistingVerificationRequest(args.otherUserId)
                    ?.lastOrNull { !it.isFinished }
                    ?.also { verificationRequest ->
                        if (verificationRequest.isIncoming && !verificationRequest.isReady) {
                            // auto ready in this case, as we are waiting
                            autoReady = true
                        }
                    }
        } else {
            session.cryptoService().verificationService().getExistingVerificationRequest(args.otherUserId, args.verificationId)
        }

        val sasTx = (pr?.transactionId ?: args.verificationId)?.let {
            session.cryptoService().verificationService().getExistingTransaction(args.otherUserId, it) as? SasVerificationTransaction
        }

        val qrTx = (pr?.transactionId ?: args.verificationId)?.let {
            session.cryptoService().verificationService().getExistingTransaction(args.otherUserId, it) as? QrCodeVerificationTransaction
        }

        setState {
            copy(
                    otherUserMxItem = userItem?.toMatrixItem(),
                    sasTransactionState = sasTx?.state,
                    qrTransactionState = qrTx?.state,
                    transactionId = pr?.transactionId ?: args.verificationId,
                    pendingRequest = if (pr != null) Success(pr) else Uninitialized,
                    selfVerificationMode = selfVerificationMode,
                    roomId = args.roomId,
                    isMe = args.otherUserId == session.myUserId
            )
        }

        if (autoReady) {
            // TODO, can I be here in DM mode? in this case should test if roomID is null?
            session.cryptoService().verificationService()
                    .readyPendingVerification(supportedVerificationMethods,
                            pr!!.otherUserId,
                            pr.transactionId ?: "")
        }
    }

    override fun onCleared() {
        session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationBottomSheetViewState,
                   args: VerificationBottomSheet.VerificationArgs): VerificationBottomSheetViewModel
    }

    companion object : MvRxViewModelFactory<VerificationBottomSheetViewModel, VerificationBottomSheetViewState> {

        override fun create(viewModelContext: ViewModelContext, state: VerificationBottomSheetViewState): VerificationBottomSheetViewModel? {
            val fragment: VerificationBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            val args: VerificationBottomSheet.VerificationArgs = viewModelContext.args()

            return fragment.verificationViewModelFactory.create(state, args)
        }
    }

    override fun handle(action: VerificationAction) = withState { state ->
        val otherUserId = state.otherUserMxItem?.id ?: return@withState
        val roomId = state.roomId
                ?: session.getExistingDirectRoomWithUser(otherUserId)?.roomId

        when (action) {
            is VerificationAction.RequestVerificationByDM      -> {
                if (roomId == null) {
                    val localID = LocalEcho.createLocalEchoId()
                    setState {
                        copy(
                                pendingLocalId = localID,
                                pendingRequest = Loading()
                        )
                    }
                    val roomParams = CreateRoomParams(
                            invitedUserIds = listOf(otherUserId)
                    )
                            .setDirectMessage()
                            .enableEncryptionIfInvitedUsersSupportIt()

                    session.createRoom(roomParams, object : MatrixCallback<String> {
                        override fun onSuccess(data: String) {
                            setState {
                                copy(
                                        roomId = data,
                                        pendingRequest = Success(
                                                session
                                                        .cryptoService()
                                                        .verificationService()
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
                                        .cryptoService()
                                        .verificationService()
                                        .requestKeyVerificationInDMs(supportedVerificationMethods, otherUserId, roomId)
                                )
                        )
                    }
                }
                Unit
            }
            is VerificationAction.StartSASVerification         -> {
                val request = session.cryptoService().verificationService().getExistingVerificationRequest(otherUserId, action.pendingRequestTransactionId)
                        ?: return@withState
                val otherDevice = if (request.isIncoming) request.requestInfo?.fromDevice else request.readyInfo?.fromDevice
                if (roomId == null) {
                    session.cryptoService().verificationService().beginKeyVerification(
                            VerificationMethod.SAS,
                            otherUserId = request.otherUserId,
                            otherDeviceId = otherDevice ?: "",
                            transactionId = action.pendingRequestTransactionId
                    )
                } else {
                    session.cryptoService().verificationService().beginKeyVerificationInDMs(
                            VerificationMethod.SAS,
                            transactionId = action.pendingRequestTransactionId,
                            roomId = roomId,
                            otherUserId = request.otherUserId,
                            otherDeviceId = otherDevice ?: "",
                            callback = null
                    )
                }
                Unit
            }
            is VerificationAction.RemoteQrCodeScanned          -> {
                val existingTransaction = session.cryptoService().verificationService()
                        .getExistingTransaction(action.otherUserId, action.transactionId) as? QrCodeVerificationTransaction
                existingTransaction
                        ?.userHasScannedOtherQrCode(action.scannedData)
            }
            is VerificationAction.OtherUserScannedSuccessfully -> {
                val transactionId = state.transactionId ?: return@withState

                val existingTransaction = session.cryptoService().verificationService()
                        .getExistingTransaction(otherUserId, transactionId) as? QrCodeVerificationTransaction
                existingTransaction
                        ?.otherUserScannedMyQrCode()
            }
            is VerificationAction.OtherUserDidNotScanned       -> {
                val transactionId = state.transactionId ?: return@withState

                val existingTransaction = session.cryptoService().verificationService()
                        .getExistingTransaction(otherUserId, transactionId) as? QrCodeVerificationTransaction
                existingTransaction
                        ?.otherUserDidNotScannedMyQrCode()
            }
            is VerificationAction.SASMatchAction               -> {
                (session.cryptoService().verificationService()
                        .getExistingTransaction(action.otherUserId, action.sasTransactionId)
                        as? SasVerificationTransaction)?.userHasVerifiedShortCode()
            }
            is VerificationAction.SASDoNotMatchAction          -> {
                (session.cryptoService().verificationService()
                        .getExistingTransaction(action.otherUserId, action.sasTransactionId)
                        as? SasVerificationTransaction)
                        ?.shortCodeDoesNotMatch()
            }
            is VerificationAction.GotItConclusion              -> {
                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
            }
            is VerificationAction.SkipVerification             -> {
                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
            }
            is VerificationAction.VerifyFromPassphrase         -> {
                _viewEvents.post(VerificationBottomSheetViewEvents.AccessSecretStore)
            }
            is VerificationAction.GotResultFromSsss            -> {
                try {
                    action.cypherData.fromBase64NoPadding().inputStream().use { ins ->
                        val res = session.loadSecureSecret<Map<String, String>>(ins, action.alias)
                        val trustResult = session.getCrossSigningService().checkTrustFromPrivateKeys(
                                res?.get(MASTER_KEY_SSSS_NAME),
                                res?.get(USER_SIGNING_KEY_SSSS_NAME),
                                res?.get(SELF_SIGNING_KEY_SSSS_NAME)
                        )
                        if (trustResult.isVerified()) {
                            setState {
                                copy(verifiedFromPrivateKeys = true)
                            }
                        } else {
                            // POP UP something
                            _viewEvents.post(VerificationBottomSheetViewEvents.ModalError("Failed to import keys"))
                        }
                    }
                } catch (failure: Throwable) {
                    _viewEvents.post(VerificationBottomSheetViewEvents.ModalError(failure.localizedMessage))
                }

                Unit
            }
        }.exhaustive
    }

    override fun transactionCreated(tx: VerificationTransaction) {
        transactionUpdated(tx)
    }

    override fun transactionUpdated(tx: VerificationTransaction) = withState { state ->
        if (state.selfVerificationMode && state.transactionId == null) {
            // is this an incoming with that user
            if (tx.isIncoming && tx.otherUserId == state.otherUserMxItem?.id) {
                // Also auto accept incoming if needed!
                if (tx is IncomingSasVerificationTransaction) {
                    if (tx.uxState == IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT) {
                        tx.performAccept()
                    }
                }
                // Use this one!
                setState {
                    copy(
                            transactionId = tx.transactionId,
                            sasTransactionState = tx.state.takeIf { tx is SasVerificationTransaction },
                            qrTransactionState = tx.state.takeIf { tx is QrCodeVerificationTransaction }
                    )
                }
            }
        }

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

        if (state.selfVerificationMode && state.pendingRequest.invoke() == null && state.transactionId == null) {
            // is this an incoming with that user
            if (pr.isIncoming && pr.otherUserId == state.otherUserMxItem?.id) {
                if (!pr.isReady) {
                    // auto ready in this case, as we are waiting
                    // TODO, can I be here in DM mode? in this case should test if roomID is null?
                    session.cryptoService().verificationService()
                            .readyPendingVerification(supportedVerificationMethods,
                                    pr.otherUserId,
                                    pr.transactionId ?: "")
                }

                // Use this one!
                setState {
                    copy(
                            transactionId = pr.transactionId,
                            pendingRequest = Success(pr)
                    )
                }
                return@withState
            }
        }

        if (pr.localID == state.pendingLocalId
                || pr.localID == state.pendingRequest.invoke()?.localID
                || state.pendingRequest.invoke()?.transactionId == pr.transactionId) {
            setState {
                copy(pendingRequest = Success(pr))
            }
        }
    }
}
