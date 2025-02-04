/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.user

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.SupportedVerificationMethodsProvider
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewEvents
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.QRCodeVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
import org.matrix.android.sdk.api.session.crypto.verification.getTransaction
import org.matrix.android.sdk.api.session.getUser
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber

data class UserVerificationViewState(
        val pendingRequest: Async<PendingVerificationRequest> = Uninitialized,
        // need something immutable for state to work properly, VerificationTransaction is not
        val startedTransaction: Async<VerificationTransactionData> = Uninitialized,
        val otherUserMxItem: MatrixItem,
        val otherUserId: String,
        val otherDeviceId: String? = null,
//        val roomId: String,
        val transactionId: String?,
        val otherUserIsTrusted: Boolean = false,
//        val currentDeviceCanCrossSign: Boolean = false,
//        val userWantsToCancel: Boolean = false,
) : MavericksState {

    constructor(args: UserVerificationBottomSheet.Args) : this(
            otherUserId = args.otherUserId,
            transactionId = args.verificationId,
//            roomId = args.roomId,
            otherUserMxItem = MatrixItem.UserItem(args.otherUserId),
    )
}

// We need immutable objects to use properly in MvrxState
sealed class VerificationTransactionData(
        open val transactionId: String,
        open val otherUserId: String,
) {

    data class SasTransactionData(
            override val transactionId: String,
            val state: SasTransactionState,
            override val otherUserId: String,
            val otherDeviceId: String?,
            val isIncoming: Boolean,
            val emojiCodeRepresentation: List<EmojiRepresentation>?
    ) : VerificationTransactionData(transactionId, otherUserId)

    data class QrTransactionData(
            override val transactionId: String,
            val state: QRCodeVerificationState,
            override val otherUserId: String,
            val otherDeviceId: String?,
            val isIncoming: Boolean,
    ) : VerificationTransactionData(transactionId, otherUserId)
}

fun VerificationTransaction.toDataClass(): VerificationTransactionData? {
    return when (this) {
        is SasVerificationTransaction -> {
            VerificationTransactionData.SasTransactionData(
                    transactionId = this.transactionId,
                    state = this.state(),
                    otherUserId = this.otherUserId,
                    otherDeviceId = this.otherDeviceId,
                    isIncoming = this.isIncoming,
                    emojiCodeRepresentation = this.getEmojiCodeRepresentation()
            )
        }
        is QrCodeVerificationTransaction -> {
            VerificationTransactionData.QrTransactionData(
                    transactionId = this.transactionId,
                    state = this.state(),
                    otherUserId = this.otherUserId,
                    otherDeviceId = this.otherDeviceId,
                    isIncoming = this.isIncoming,
            )
        }
        else -> null
    }
}

class UserVerificationViewModel @AssistedInject constructor(
        @Assisted private val initialState: UserVerificationViewState,
        private val session: Session,
        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider,
        private val stringProvider: StringProvider,
        private val matrix: Matrix,
) :
        VectorViewModel<UserVerificationViewState, VerificationAction, VerificationBottomSheetViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<UserVerificationViewModel, UserVerificationViewState> {
        override fun create(initialState: UserVerificationViewState): UserVerificationViewModel
    }

    companion object : MavericksViewModelFactory<UserVerificationViewModel, UserVerificationViewState> by hiltMavericksViewModelFactory()

    var currentTransactionId: String? = null

    init {
        currentTransactionId = initialState.transactionId

        session.cryptoService().verificationService()
                .requestEventFlow()
                .filter {
                    it.transactionId == currentTransactionId ||
                            currentTransactionId == null && initialState.otherUserId == it.getRequest()?.otherUserId
                }
                .onEach {
//                    Timber.w("VALR update event ${it.getRequest()} ")
                    it.getRequest()?.let {
//                        Timber.w("VALR state updated request to $it")
                        setState {
                            copy(
                                    pendingRequest = Success(it),
                            )
                        }
                    }
                    it.getTransaction()?.let { transaction ->
                        val dClass = transaction.toDataClass()
                        if (dClass != null) {
                            setState {
                                copy(
                                        startedTransaction = Success(dClass),
                                )
                            }
                        } else {
                            setState {
                                copy(
                                        startedTransaction = Fail(IllegalArgumentException("Unsupported Transaction")),
                                )
                            }
                        }
                    }
                }
                .launchIn(viewModelScope)

        fetchOtherUserProfile(initialState.otherUserId)

        session.cryptoService().crossSigningService()
                .getLiveCrossSigningKeys(initialState.otherUserId)
                .asFlow()
                .execute {
                    copy(otherUserIsTrusted = it.invoke()?.getOrNull()?.isTrusted().orFalse())
                }

        if (initialState.transactionId != null) {
            setState {
                copy(pendingRequest = Loading())
            }
            viewModelScope.launch {
                val request = session.cryptoService().verificationService().getExistingVerificationRequest(
                        initialState.otherUserId,
                        initialState.transactionId
                )
                if (request != null) {
                    setState {
                        copy(
                                pendingRequest = Success(request),
                        )
                    }
                } else {
                    setState {
                        copy(pendingRequest = Fail(IllegalStateException("Verification request not found")))
                    }
                }
            }
        }
    }

    private fun onRequestUpdateAndNoTransactionId(request: PendingVerificationRequest, state: UserVerificationViewState) {
        if (request.otherUserId == state.otherUserId) {
            if (state.otherDeviceId != null) {
                if (request.otherDeviceId == state.otherDeviceId) {
                    setState {
                        copy(
                                pendingRequest = Success(request),
                                transactionId = request.transactionId
                        )
                    }
                }
            } else {
                // This request is ok for us
//                Timber.w("VALR state updated request to $request")
                setState {
                    copy(

                            pendingRequest = Success(request),
                            transactionId = request.transactionId
                    )
                }
            }
        }
    }

    fun queryCancel() = withState { state ->
        // check if there is an existing request
        val request = state.pendingRequest.invoke()
        when (request?.state) {
            EVerificationState.WaitingForReady,
            EVerificationState.Requested,
            EVerificationState.Ready,
            EVerificationState.Started,
            EVerificationState.WeStarted,
            EVerificationState.WaitingForDone -> {
                // query confirmation
                _viewEvents.post(VerificationBottomSheetViewEvents.ConfirmCancel(request.otherUserId, request.otherDeviceId))
            }
//            EVerificationState.Done,
//            EVerificationState.Cancelled,
//            EVerificationState.HandledByOtherSession,
            else -> {
                // we can just dismiss?
                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
            }
        }
    }

    override fun handle(action: VerificationAction) {
        when (action) {
            VerificationAction.CancelPendingVerification -> {
                withState { state ->
                    state.pendingRequest.invoke()?.let {
                        viewModelScope.launch {
                            session.cryptoService().verificationService()
                                    .cancelVerificationRequest(it)
                        }
                    }
                }
            }
            is VerificationAction.GotItConclusion -> {
                // just dismiss
                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
            }
            is VerificationAction.GotResultFromSsss -> {
                // not applicable, only for self verification
            }
            VerificationAction.OtherUserDidNotScanned -> {
                withState { state ->
                    state.startedTransaction.invoke()?.let {
                        viewModelScope.launch {
                            val tx = session.cryptoService().verificationService()
                                    .getExistingTransaction(it.otherUserId, it.transactionId)
                                    as? QrCodeVerificationTransaction
                            tx?.otherUserDidNotScannedMyQrCode()
                        }
                    }
                }
            }
            VerificationAction.OtherUserScannedSuccessfully -> {
                withState { state ->
                    state.startedTransaction.invoke()?.let {
                        viewModelScope.launch {
                            val tx = session.cryptoService().verificationService()
                                    .getExistingTransaction(it.otherUserId, it.transactionId)
                                    as? QrCodeVerificationTransaction
                            tx?.otherUserScannedMyQrCode()
                        }
                    }
                }
            }
            VerificationAction.ReadyPendingVerification -> {
                withState { state ->
                    state.pendingRequest.invoke()?.let {
                        viewModelScope.launch {
                            session.cryptoService().verificationService()
                                    .readyPendingVerification(
                                            supportedVerificationMethodsProvider.provide(),
                                            it.otherUserId, it.transactionId
                                    )
                        }
                    }
                }
            }
            is VerificationAction.RemoteQrCodeScanned -> {
                setState {
                    copy(startedTransaction = Loading())
                }
                withState { state ->
                    val request = state.pendingRequest.invoke() ?: return@withState
                    viewModelScope.launch {
                        try {
                            session.cryptoService().verificationService()
                                    .reciprocateQRVerification(
                                            request.otherUserId,
                                            request.transactionId,
                                            action.scannedData
                                    )
                        } catch (failure: Throwable) {
                            Timber.w(failure, "Failed to reciprocated")
                            setState {
                                copy(startedTransaction = Fail(failure))
                            }
                        }
                    }
                }
            }
            is VerificationAction.RequestVerificationByDM -> {
                setState {
                    copy(pendingRequest = Loading())
                }
                viewModelScope.launch {
                    // TODO if self verif we should do via DM
                    val roomId = session.roomService().getExistingDirectRoomWithUser(initialState.otherUserId)
                            ?: session.roomService().createDirectRoom(initialState.otherUserId)

                    try {
                        val request = session.cryptoService().verificationService()
                                .requestKeyVerificationInDMs(
                                        methods = supportedVerificationMethodsProvider.provide(),
                                        otherUserId = initialState.otherUserId,
                                        roomId = roomId,
                                )

                        currentTransactionId = request.transactionId

                        setState {
                            copy(
                                    pendingRequest = Success(request),
                                    transactionId = request.transactionId
                            )
                        }
                    } catch (failure: Throwable) {
                        setState {
                            copy(
                                    pendingRequest = Fail(failure),
                            )
                        }
                    }
                }
            }
            is VerificationAction.SASDoNotMatchAction -> {
                withState { state ->
                    viewModelScope.launch {
                        val transaction = session.cryptoService().verificationService()
                                .getExistingTransaction(state.otherUserId, state.transactionId.orEmpty())
                        (transaction as? SasVerificationTransaction)?.shortCodeDoesNotMatch()
                    }
                }
            }
            is VerificationAction.SASMatchAction -> {
                withState { state ->
                    viewModelScope.launch {
                        val transaction = session.cryptoService().verificationService()
                                .getExistingTransaction(state.otherUserId, state.transactionId.orEmpty())
                        (transaction as? SasVerificationTransaction)?.userHasVerifiedShortCode()
                    }
                }
            }
            is VerificationAction.StartSASVerification -> {
                withState { state ->
                    val request = state.pendingRequest.invoke() ?: return@withState
                    viewModelScope.launch {
                        session.cryptoService().verificationService()
                                .startKeyVerification(VerificationMethod.SAS, state.otherUserId, request.transactionId)
                    }
                }
            }
            VerificationAction.CancelledFromSsss,
            VerificationAction.SkipVerification,
            VerificationAction.VerifyFromPassphrase,
            VerificationAction.SecuredStorageHasBeenReset,
            VerificationAction.FailedToGetKeysFrom4S,
            VerificationAction.RequestSelfVerification,
            VerificationAction.SelfVerificationWasNotMe,
            VerificationAction.ForgotResetAll -> {
                // Not applicable for user verification
            }
        }
    }

    private fun fetchOtherUserProfile(otherUserId: String) {
        session.getUser(otherUserId)?.toMatrixItem()?.let {
            setState {
                copy(
                        otherUserMxItem = it
                )
            }
        }
        // Always fetch the latest User data
        viewModelScope.launch {
            tryOrNull { session.userService().resolveUser(otherUserId) }
                    ?.toMatrixItem()
                    ?.let {
                        setState {
                            copy(
                                    otherUserMxItem = it
                            )
                        }
                    }
        }
    }
}
