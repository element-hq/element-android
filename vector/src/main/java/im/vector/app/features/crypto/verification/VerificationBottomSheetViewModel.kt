// /*
// * Copyright 2019 New Vector Ltd
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
// package im.vector.app.features.crypto.verification
//
// import com.airbnb.mvrx.Async
// import com.airbnb.mvrx.Fail
// import com.airbnb.mvrx.Loading
// import com.airbnb.mvrx.MavericksState
// import com.airbnb.mvrx.MavericksViewModelFactory
// import com.airbnb.mvrx.Success
// import com.airbnb.mvrx.Uninitialized
// import dagger.assisted.Assisted
// import dagger.assisted.AssistedFactory
// import dagger.assisted.AssistedInject
// import im.vector.app.R
// import im.vector.app.core.di.MavericksAssistedViewModelFactory
// import im.vector.app.core.di.hiltMavericksViewModelFactory
// import im.vector.app.core.platform.VectorViewModel
// import im.vector.app.core.resources.StringProvider
// import im.vector.app.features.raw.wellknown.getElementWellknown
// import im.vector.app.features.raw.wellknown.isSecureBackupRequired
// import im.vector.app.features.session.coroutineScope
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.flow.collect
// import kotlinx.coroutines.flow.filter
// import kotlinx.coroutines.flow.launchIn
// import kotlinx.coroutines.flow.onEach
// import kotlinx.coroutines.launch
// import org.matrix.android.sdk.api.Matrix
// import org.matrix.android.sdk.api.extensions.orFalse
// import org.matrix.android.sdk.api.extensions.tryOrNull
// import org.matrix.android.sdk.api.raw.RawService
// import org.matrix.android.sdk.api.session.Session
// import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
// import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
// import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
// import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
// import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
// import org.matrix.android.sdk.api.session.crypto.keysbackup.BackupUtils
// import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
// import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
// import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
// import org.matrix.android.sdk.api.session.crypto.verification.IVerificationRequest
// import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
// import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
// import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
// import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
// import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
// import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
// import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
// import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
// import org.matrix.android.sdk.api.session.events.model.LocalEcho
// import org.matrix.android.sdk.api.session.getUser
// import org.matrix.android.sdk.api.util.MatrixItem
// import org.matrix.android.sdk.api.util.fromBase64
// import org.matrix.android.sdk.api.util.toMatrixItem
// import timber.log.Timber
//
// data class VerificationBottomSheetViewState(
//        val otherUserId: String,
//        val verificationId: String?,
//        val roomId: String?,
//        // true when we display the loading and we wait for the other (incoming request)
//        val selfVerificationMode: Boolean,
//        val otherUserMxItem: MatrixItem,
//        val pendingRequest: Async<PendingVerificationRequest> = Uninitialized,
//        val pendingLocalId: String? = null,
//        val sasTransactionState: VerificationTxState? = null,
//        val qrTransactionState: VerificationTxState? = null,
//        val transactionId: String? = null,
//        val verifiedFromPrivateKeys: Boolean = false,
//        val verifyingFrom4S: Boolean = false,
//        val isMe: Boolean = false,
//        val currentDeviceCanCrossSign: Boolean = false,
//        val userWantsToCancel: Boolean = false,
//        val userThinkItsNotHim: Boolean = false,
//        val quadSContainsSecrets: Boolean = true,
//        val isVerificationRequired: Boolean = false,
//        val quadSHasBeenReset: Boolean = false,
//        val hasAnyOtherSession: Boolean = false
// ) : MavericksState {
//
//    constructor(args: VerificationBottomSheet.VerificationArgs) : this(
//            otherUserId = args.otherUserId,
//            verificationId = args.verificationId,
//            selfVerificationMode = args.selfVerificationMode,
//            roomId = args.roomId,
//            otherUserMxItem = MatrixItem.UserItem(args.otherUserId),
//    )
// }
//
// class VerificationBottomSheetViewModel @AssistedInject constructor(
//        @Assisted initialState: VerificationBottomSheetViewState,
//        private val rawService: RawService,
//        private val session: Session,
//        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider,
//        private val stringProvider: StringProvider,
//        private val matrix: Matrix,
// ) :
//        VectorViewModel<VerificationBottomSheetViewState, VerificationAction, VerificationBottomSheetViewEvents>(initialState),
//        VerificationService.Listener {
//
//    @AssistedFactory
//    interface Factory : MavericksAssistedViewModelFactory<VerificationBottomSheetViewModel, VerificationBottomSheetViewState> {
//        override fun create(initialState: VerificationBottomSheetViewState): VerificationBottomSheetViewModel
//    }
//
//    companion object : MavericksViewModelFactory<VerificationBottomSheetViewModel, VerificationBottomSheetViewState> by hiltMavericksViewModelFactory()
//
//    init {
// //        session.cryptoService().verificationService().addListener(this)
//
//        // This is async, but at this point should be in cache
//        // so it's ok to not wait until result
//        viewModelScope.launch(Dispatchers.IO) {
//            val wellKnown = rawService.getElementWellknown(session.sessionParams)
//            setState {
//                copy(isVerificationRequired = wellKnown?.isSecureBackupRequired().orFalse())
//            }
//        }
//
//
// //        viewModelScope.launch {
// //
// //            var autoReady = false
// //            val pr = if (initialState.selfVerificationMode) {
// //                // See if active tx for this user and take it
// //
// //                session.cryptoService().verificationService().getExistingVerificationRequests(initialState.otherUserId)
// //                        .lastOrNull { !it.isFinished }
// //                        ?.also { verificationRequest ->
// //                            if (verificationRequest.isIncoming && !verificationRequest.isReady) {
// //                                // auto ready in this case, as we are waiting
// //                                autoReady = true
// //                            }
// //                        }
// //            } else {
// //                session.cryptoService().verificationService().getExistingVerificationRequest(initialState.otherUserId, initialState.transactionId)
// //            }
// //
// //            val sasTx = (pr?.transactionId ?: initialState.transactionId)?.let {
// //                session.cryptoService().verificationService().getExistingTransaction(initialState.otherUserId, it) as? SasVerificationTransaction
// //            }
// //
// //            val qrTx = (pr?.transactionId ?: initialState.transactionId)?.let {
// //                session.cryptoService().verificationService().getExistingTransaction(initialState.otherUserId, it) as? QrCodeVerificationTransaction
// //            }
// //
// //            setState {
// //                copy(
// //                        sasTransactionState = sasTx?.state,
// //                        qrTransactionState = qrTx?.state,
// //                        transactionId = pr?.transactionId ?: initialState.transactionId,
// //                        pendingRequest = if (pr != null) Success(pr) else Uninitialized,
// //                        isMe = initialState.otherUserId == session.myUserId,
// //                        currentDeviceCanCrossSign = session.cryptoService().crossSigningService().canCrossSign(),
// //                        quadSContainsSecrets = session.sharedSecretStorageService().isRecoverySetup(),
// //                        hasAnyOtherSession = hasAnyOtherSession
// //                )
// //            }
// //
// //            if (autoReady) {
// //                // TODO, can I be here in DM mode? in this case should test if roomID is null?
// //                session.cryptoService().verificationService()
// //                        .readyPendingVerification(
// //                                supportedVerificationMethodsProvider.provide(),
// //                                pr!!.otherUserId,
// //                                pr.transactionId ?: ""
// //                        )
// //            }
// //        }
//    }
//
//    private fun fetchOtherUserProfile(otherUserId: String) {
//        session.getUser(otherUserId)?.toMatrixItem()?.let {
//            setState {
//                copy(
//                        otherUserMxItem = it
//                )
//            }
//        }
//        // Always fetch the latest User data
//        viewModelScope.launch {
//            tryOrNull { session.userService().resolveUser(otherUserId) }
//                    ?.toMatrixItem()
//                    ?.let {
//                        setState {
//                            copy(
//                                    otherUserMxItem = it
//                            )
//                        }
//                    }
//        }
//    }
//
// //    override fun onCleared() {
// //        super.onCleared()
// //    }
//
//    fun queryCancel() = withState { state ->
//        if (state.userThinkItsNotHim) {
//            setState {
//                copy(userThinkItsNotHim = false)
//            }
//        } else {
//            // if the verification is already done you can't cancel anymore
//            if (state.pendingRequest.invoke()?.cancelConclusion != null ||
//                   // state.sasTransactionState is VerificationTxState.TerminalTxState ||
//                    state.verifyingFrom4S) {
//                // you cannot cancel anymore
//            } else {
//                if (!state.isVerificationRequired) {
//                    setState {
//                        copy(userWantsToCancel = true)
//                    }
//                }
//            }
//        }
//    }
//
//    fun confirmCancel() = withState { state ->
//        cancelAllPendingVerifications(state)
//        _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
//    }
//
//    private fun cancelAllPendingVerifications(state: VerificationBottomSheetViewState) {
//        viewModelScope.launch {
//            session.cryptoService()
//                    .verificationService().getExistingVerificationRequest(state.otherUserId, state.transactionId)?.let {
//                        session.cryptoService().verificationService().cancelVerificationRequest(it)
//                    }
//            session.cryptoService()
//                    .verificationService()
//                    .getExistingTransaction(state.otherUserId, state.transactionId ?: "")
//                    ?.cancel(CancelCode.User)
//        }
//    }
//
//    fun continueFromCancel() {
//        setState {
//            copy(userWantsToCancel = false)
//        }
//    }
//
//    fun continueFromWasNotMe() {
//        setState {
//            copy(userThinkItsNotHim = false)
//        }
//    }
//
//    fun itWasNotMe() {
//        setState {
//            copy(userThinkItsNotHim = true)
//        }
//    }
//
//    fun goToSettings() = withState { state ->
//        cancelAllPendingVerifications(state)
//        _viewEvents.post(VerificationBottomSheetViewEvents.GoToSettings)
//    }
//
//    override fun handle(action: VerificationAction) = withState { state ->
//        val otherUserId = state.otherUserId
//        val roomId = state.roomId
//                ?: session.roomService().getExistingDirectRoomWithUser(otherUserId)
//
//        when (action) {
//            is VerificationAction.RequestVerificationByDM -> {
//                setState {
//                    copy(
//                            pendingRequest = Loading()
//                    )
//                }
//
//                if (roomId == null) {
// //                    val localId = LocalEcho.createLocalEchoId()
//                    session.coroutineScope.launch {
//                        try {
//                            val roomId = session.roomService().createDirectRoom(otherUserId)
//                            val request = session
//                                    .cryptoService()
//                                    .verificationService()
//                                    .requestKeyVerificationInDMs(
//                                            supportedVerificationMethodsProvider.provide(),
//                                            otherUserId,
//                                            roomId,
//                                    )
//                            setState {
//                                copy(
//                                        roomId = roomId,
//                                        pendingRequest = Success(request),
//                                        transactionId = request.transactionId
//                                )
//                            }
//                        } catch (failure: Throwable) {
//                            setState {
//                                copy(pendingRequest = Fail(failure))
//                            }
//                        }
//                    }
//                } else {
//                    session.coroutineScope.launch {
//                        val request = session
//                                .cryptoService()
//                                .verificationService()
//                                .requestKeyVerificationInDMs(supportedVerificationMethodsProvider.provide(), otherUserId, roomId)
//                        setState {
//                            copy(
//                                    pendingRequest = Success(request),
//                                    transactionId = request.transactionId
//                            )
//                        }
//                    }
//                }
//                Unit
//            }
//            is VerificationAction.StartSASVerification -> {
//                viewModelScope.launch {
//                    val request = session.cryptoService().verificationService().getExistingVerificationRequest(otherUserId, state.transactionId)
//                            ?: return@launch
//                    val otherDevice = request.otherDeviceId
//                    if (roomId == null) {
//                        session.cryptoService().verificationService().requestSelfKeyVerification(
//                                listOf(VerificationMethod.SAS)
//                        )
//                    } else {
//                        session.cryptoService().verificationService().requestKeyVerificationInDMs(
//                                listOf(VerificationMethod.SAS),
//                                roomId = roomId,
//                                otherUserId = request.otherUserId,
//                        )
//                    }
//                }
//                Unit
//            }
//            is VerificationAction.RemoteQrCodeScanned -> {
//                viewModelScope.launch {
//                    val existingTransaction = session.cryptoService().verificationService()
//                            .getExistingTransaction(action.otherUserId, action.transactionId) as? QrCodeVerificationTransaction
//                    existingTransaction
//                            ?.userHasScannedOtherQrCode(action.scannedData)
//                }
//            }
//            is VerificationAction.OtherUserScannedSuccessfully -> {
//                viewModelScope.launch {
//                    val transactionId = state.transactionId ?: return@launch
//
//                    val existingTransaction = session.cryptoService().verificationService()
//                            .getExistingTransaction(otherUserId, transactionId) as? QrCodeVerificationTransaction
//                    existingTransaction
//                            ?.otherUserScannedMyQrCode()
//                }
//            }
//            is VerificationAction.OtherUserDidNotScanned -> {
//                val transactionId = state.transactionId ?: return@withState
//                viewModelScope.launch {
//                    val existingTransaction = session.cryptoService().verificationService()
//                            .getExistingTransaction(otherUserId, transactionId) as? QrCodeVerificationTransaction
//                    existingTransaction
//                            ?.otherUserDidNotScannedMyQrCode()
//                }
//            }
//            is VerificationAction.SASMatchAction -> {
//                val request = state.pendingRequest.invoke() ?: return@withState
//                viewModelScope.launch {
//                    (session.cryptoService().verificationService()
//                            .getExistingTransaction(request.otherUserId, request.transactionId)
//                            as? SasVerificationTransaction)?.userHasVerifiedShortCode()
//                }
//            }
//            is VerificationAction.SASDoNotMatchAction -> {
//                val request = state.pendingRequest.invoke() ?: return@withState
//                viewModelScope.launch {
//                    (session.cryptoService().verificationService()
//                            .getExistingTransaction(request.otherUserId, request.transactionId)
//                            as? SasVerificationTransaction)
//                            ?.shortCodeDoesNotMatch()
//                }
//            }
//            is VerificationAction.ReadyPendingVerification -> {
//                state.pendingRequest.invoke()?.let { request ->
//                    // will only be there for dm verif
//                    session.coroutineScope.launch {
//                        if (state.roomId != null) {
//                            session.cryptoService().verificationService()
//                                    .readyPendingVerification(
//                                            supportedVerificationMethodsProvider.provide(),
//                                            state.otherUserId,
//                                            request.transactionId
//                                    )
//                        }
//                    }
//                }
//            }
//            is VerificationAction.CancelPendingVerification -> {
//                state.pendingRequest.invoke()?.let {
//                    session.coroutineScope.launch {
//                        session.cryptoService().verificationService()
//                                .cancelVerificationRequest(it)
//                    }
//                }
//                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
//            }
//            is VerificationAction.GotItConclusion -> {
//                if (state.isVerificationRequired && !action.verified) {
//                    // we should go back to first screen
//                    setState {
//                        copy(
//                                pendingRequest = Uninitialized,
// //                                sasTransactionState = null,
// //                                qrTransactionState = null
//                        )
//                    }
//                } else {
//                    _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
//                }
//            }
//            is VerificationAction.SkipVerification -> {
//                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
//            }
//            is VerificationAction.VerifyFromPassphrase -> {
//                setState { copy(verifyingFrom4S = true) }
//                _viewEvents.post(VerificationBottomSheetViewEvents.AccessSecretStore)
//            }
//            is VerificationAction.GotResultFromSsss -> {
//                handleSecretBackFromSSSS(action)
//            }
//            VerificationAction.SecuredStorageHasBeenReset -> {
//                if (session.cryptoService().crossSigningService().allPrivateKeysKnown()) {
//                    setState {
//                        copy(quadSHasBeenReset = true, verifyingFrom4S = false)
//                    }
//                }
//                Unit
//            }
//            VerificationAction.CancelledFromSsss -> {
//                setState {
//                    copy(verifyingFrom4S = false)
//                }
//            }
//        }
//    }
//
//    private fun handleSecretBackFromSSSS(action: VerificationAction.GotResultFromSsss) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                action.cypherData.fromBase64().inputStream().use { ins ->
//                    val res = matrix.secureStorageService().loadSecureSecret<Map<String, String>>(ins, action.alias)
//                    val trustResult = session.cryptoService().crossSigningService().checkTrustFromPrivateKeys(
//                            res?.get(MASTER_KEY_SSSS_NAME),
//                            res?.get(USER_SIGNING_KEY_SSSS_NAME),
//                            res?.get(SELF_SIGNING_KEY_SSSS_NAME)
//                    )
//                    if (trustResult.isVerified()) {
//                        // Sign this device and upload the signature
//                        try {
//                            session.cryptoService().crossSigningService().trustDevice(session.sessionParams.deviceId)
//                        } catch (failure: Exception) {
//                            Timber.w(failure, "Failed to sign my device after recovery")
//                        }
//
//                        setState {
//                            copy(
//                                    verifyingFrom4S = false,
//                                    verifiedFromPrivateKeys = true
//                            )
//                        }
//
//                        // try the keybackup
//                        tentativeRestoreBackup(res)
//                    } else {
//                        setState {
//                            copy(
//                                    verifyingFrom4S = false
//                            )
//                        }
//                        // POP UP something
//                        _viewEvents.post(VerificationBottomSheetViewEvents.ModalError(stringProvider.getString(R.string.error_failed_to_import_keys)))
//                    }
//                }
//            } catch (failure: Throwable) {
//                setState {
//                    copy(
//                            verifyingFrom4S = false
//                    )
//                }
//                _viewEvents.post(
//                        VerificationBottomSheetViewEvents.ModalError(failure.localizedMessage ?: stringProvider.getString(R.string.unexpected_error))
//                )
//            }
//        }
//    }
//
//    private fun tentativeRestoreBackup(res: Map<String, String>?) {
//        // on session scope because will happen after viewmodel is cleared
//        session.coroutineScope.launch {
//            // It's not a good idea to download the full backup, it might take very long
//            // and use a lot of resources
//            // Just check that the key is valid and store it, the backup will be used megolm session per
//            // megolm session when an UISI is encountered
//            try {
//                val secret = res?.get(KEYBACKUP_SECRET_SSSS_NAME) ?: return@launch Unit.also {
//                    Timber.v("## Keybackup secret not restored from SSSS")
//                }
//
//                val version = session.cryptoService().keysBackupService().getCurrentVersion()?.toKeysVersionResult() ?: return@launch
//
//                val recoveryKey = computeRecoveryKey(secret.fromBase64())
//                val backupRecoveryKey = BackupUtils.recoveryKeyFromBase58(recoveryKey)
//                val isValid = backupRecoveryKey
//                        ?.let { session.cryptoService().keysBackupService().isValidRecoveryKeyForCurrentVersion(it) }
//                        ?: false
//                if (isValid) {
//                    session.cryptoService().keysBackupService().saveBackupRecoveryKey(backupRecoveryKey, version.version)
//                }
//                session.cryptoService().keysBackupService().trustKeysBackupVersion(version, true)
//            } catch (failure: Throwable) {
//                // Just ignore for now
//                Timber.e(failure, "## Failed to restore backup after SSSS recovery")
//            }
//        }
//    }
//
// //    override fun transactionCreated(tx: VerificationTransaction) {
// //        transactionUpdated(tx)
// //    }
// //
// //    override fun transactionUpdated(tx: VerificationTransaction) = withState { state ->
// //        if (state.selfVerificationMode && state.transactionId == null) {
// //            // is this an incoming with that user
// //            if (tx.isIncoming && tx.otherUserId == state.otherUserId) {
// //                // Also auto accept incoming if needed!
// //                if (tx is IncomingSasVerificationTransaction) {
// //                    if (tx.uxState == IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT) {
// //                        tx.performAccept()
// //                    }
// //                }
// //                // Use this one!
// //                setState {
// //                    copy(
// //                            transactionId = tx.transactionId,
// //                            sasTransactionState = tx.state.takeIf { tx is SasVerificationTransaction },
// //                            qrTransactionState = tx.state.takeIf { tx is QrCodeVerificationTransaction }
// //                    )
// //                }
// //            }
// //        }
// //
// //        when (tx) {
// //            is SasVerificationTransaction -> {
// //                if (tx.transactionId == (state.pendingRequest.invoke()?.transactionId ?: state.transactionId)) {
// //                    // A SAS tx has been started following this request
// //                    setState {
// //                        copy(
// //                                transactionId = tx.transactionId,
// //                                sasTransactionState = tx.state.takeIf { tx is SasVerificationTransaction },
// //                                qrTransactionState = tx.state.takeIf { tx is QrCodeVerificationTransaction }
// //                        )
// //                    }
// //                }
// //            }
// //
// //            when (tx) {
// //                is SasVerificationTransaction    -> {
// //                    if (tx.transactionId == (state.pendingRequest.invoke()?.transactionId ?: state.transactionId)) {
// //                        // A SAS tx has been started following this request
// //                        setState {
// //                            copy(
// //                                    sasTransactionState = tx.state
// //                            )
// //                        }
// //                    }
// //                }
// //                is QrCodeVerificationTransaction -> {
// //                    if (tx.transactionId == (state.pendingRequest.invoke()?.transactionId ?: state.transactionId)) {
// //                        // A QR tx has been started following this request
// //                        setState {
// //                            copy(
// //                                    qrTransactionState = tx.state
// //                            )
// //                        }
// //                    }
// //                }
// //            }
// //        }
// //    }
//
//    override fun transactionCreated(tx: VerificationTransaction) {
//        transactionUpdated(tx)
//    }
//
//    override fun transactionUpdated(tx: VerificationTransaction) = withState { state ->
//        Timber.v("transactionUpdated: $tx")
//        if (tx.transactionId != state.transactionId) return@withState
//        if (tx is SasVerificationTransaction) {
// //            setState {
// //                copy(
// //                        sasTransactionState = tx.state
// //                )
// //            }
//        } else if (tx is QrCodeVerificationTransaction) {
//
//        }
//        // handleTransactionUpdate(state, tx)
//    }
//
//     override fun verificationRequestCreated(pr: PendingVerificationRequest) {
//        verificationRequestUpdated(pr)
//    }
//
//     override fun verificationRequestUpdated(pr: PendingVerificationRequest) = withState { state ->
//        Timber.v("VerificationRequestUpdated: $pr")
//        if (pr.transactionId != state.pendingRequest.invoke()?.transactionId) return@withState
//        setState {
//            copy(pendingRequest = Success(pr))
//        }
// //        if (state.selfVerificationMode && state.pendingRequest.invoke() == null && state.transactionId == null) {
// //            // is this an incoming with that user
// //            if (pr.isIncoming && pr.otherUserId == state.otherUserId) {
// //                if (!pr.isReady) {
// //                    // auto ready in this case, as we are waiting
// //                    // TODO, can I be here in DM mode? in this case should test if roomID is null?
// //                    viewModelScope.launch {
// //                        session.cryptoService().verificationService()
// //                                .readyPendingVerification(
// //                                        supportedVerificationMethodsProvider.provide(),
// //                                        pr.otherUserId,
// //                                        pr.transactionId
// //                                )
// //                    }
// //                }
// //
// //                // Use this one!
// //                setState {
// //                    copy(
// //                            transactionId = pr.transactionId,
// //                            pendingRequest = Success(pr)
// //                    )
// //                }
// //                return@withState
// //            }
// //        }
// //
// //        if (state.transactionId == pr.transactionId) {
// //            setState {
// //                copy(
// //                        pendingRequest = Success(pr)
// //                )
// //            }
// //        }
//    }
// }
