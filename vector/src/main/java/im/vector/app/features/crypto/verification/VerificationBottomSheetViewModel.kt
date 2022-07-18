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
package im.vector.app.features.crypto.verification

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
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isSecureBackupRequired
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupLastVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.getUser
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber

data class VerificationBottomSheetViewState(
        val otherUserId: String,
        val verificationId: String?,
        val roomId: String?,
        // true when we display the loading and we wait for the other (incoming request)
        val selfVerificationMode: Boolean,
        val otherUserMxItem: MatrixItem? = null,
        val pendingRequest: Async<PendingVerificationRequest> = Uninitialized,
        val pendingLocalId: String? = null,
        val sasTransactionState: VerificationTxState? = null,
        val qrTransactionState: VerificationTxState? = null,
        val transactionId: String? = null,
        val verifiedFromPrivateKeys: Boolean = false,
        val verifyingFrom4S: Boolean = false,
        val isMe: Boolean = false,
        val currentDeviceCanCrossSign: Boolean = false,
        val userWantsToCancel: Boolean = false,
        val userThinkItsNotHim: Boolean = false,
        val quadSContainsSecrets: Boolean = true,
        val isVerificationRequired: Boolean = false,
        val quadSHasBeenReset: Boolean = false,
        val hasAnyOtherSession: Boolean = false
) : MavericksState {

    constructor(args: VerificationBottomSheet.VerificationArgs) : this(
            otherUserId = args.otherUserId,
            verificationId = args.verificationId,
            roomId = args.roomId,
            selfVerificationMode = args.selfVerificationMode
    )
}

class VerificationBottomSheetViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationBottomSheetViewState,
        private val rawService: RawService,
        private val session: Session,
        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider,
        private val stringProvider: StringProvider,
        private val matrix: Matrix,
) :
        VectorViewModel<VerificationBottomSheetViewState, VerificationAction, VerificationBottomSheetViewEvents>(initialState),
        VerificationService.Listener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VerificationBottomSheetViewModel, VerificationBottomSheetViewState> {
        override fun create(initialState: VerificationBottomSheetViewState): VerificationBottomSheetViewModel
    }

    companion object : MavericksViewModelFactory<VerificationBottomSheetViewModel, VerificationBottomSheetViewState> by hiltMavericksViewModelFactory()

    init {
        session.cryptoService().verificationService().addListener(this)

        // This is async, but at this point should be in cache
        // so it's ok to not wait until result
        viewModelScope.launch(Dispatchers.IO) {
            val wellKnown = rawService.getElementWellknown(session.sessionParams)
            setState {
                copy(isVerificationRequired = wellKnown?.isSecureBackupRequired().orFalse())
            }
        }

        val userItem = session.getUser(initialState.otherUserId)

        var autoReady = false
        val pr = if (initialState.selfVerificationMode) {
            // See if active tx for this user and take it

            session.cryptoService().verificationService().getExistingVerificationRequests(initialState.otherUserId)
                    .lastOrNull { !it.isFinished }
                    ?.also { verificationRequest ->
                        if (verificationRequest.isIncoming && !verificationRequest.isReady) {
                            // auto ready in this case, as we are waiting
                            autoReady = true
                        }
                    }
        } else {
            session.cryptoService().verificationService().getExistingVerificationRequest(initialState.otherUserId, initialState.verificationId)
        }

        val sasTx = (pr?.transactionId ?: initialState.verificationId)?.let {
            session.cryptoService().verificationService().getExistingTransaction(initialState.otherUserId, it) as? SasVerificationTransaction
        }

        val qrTx = (pr?.transactionId ?: initialState.verificationId)?.let {
            session.cryptoService().verificationService().getExistingTransaction(initialState.otherUserId, it) as? QrCodeVerificationTransaction
        }

        val hasAnyOtherSession = session.cryptoService()
                .getCryptoDeviceInfo(session.myUserId)
                .any {
                    it.deviceId != session.sessionParams.deviceId
                }

        setState {
            copy(
                    otherUserMxItem = userItem?.toMatrixItem(),
                    sasTransactionState = sasTx?.state,
                    qrTransactionState = qrTx?.state,
                    transactionId = pr?.transactionId ?: initialState.verificationId,
                    pendingRequest = if (pr != null) Success(pr) else Uninitialized,
                    isMe = initialState.otherUserId == session.myUserId,
                    currentDeviceCanCrossSign = session.cryptoService().crossSigningService().canCrossSign(),
                    quadSContainsSecrets = session.sharedSecretStorageService().isRecoverySetup(),
                    hasAnyOtherSession = hasAnyOtherSession
            )
        }

        if (autoReady) {
            // TODO, can I be here in DM mode? in this case should test if roomID is null?
            session.cryptoService().verificationService()
                    .readyPendingVerification(
                            supportedVerificationMethodsProvider.provide(),
                            pr!!.otherUserId,
                            pr.transactionId ?: ""
                    )
        }
    }

    override fun onCleared() {
        session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
    }

    fun queryCancel() = withState { state ->
        if (state.userThinkItsNotHim) {
            setState {
                copy(userThinkItsNotHim = false)
            }
        } else {
            // if the verification is already done you can't cancel anymore
            if (state.pendingRequest.invoke()?.cancelConclusion != null ||
                    state.sasTransactionState is VerificationTxState.TerminalTxState ||
                    state.verifyingFrom4S) {
                // you cannot cancel anymore
            } else {
                if (!state.isVerificationRequired) {
                    setState {
                        copy(userWantsToCancel = true)
                    }
                }
            }
        }
    }

    fun confirmCancel() = withState { state ->
        cancelAllPendingVerifications(state)
        _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
    }

    private fun cancelAllPendingVerifications(state: VerificationBottomSheetViewState) {
        session.cryptoService()
                .verificationService().getExistingVerificationRequest(state.otherUserMxItem?.id ?: "", state.transactionId)?.let {
                    session.cryptoService().verificationService().cancelVerificationRequest(it)
                }
        session.cryptoService()
                .verificationService()
                .getExistingTransaction(state.otherUserMxItem?.id ?: "", state.transactionId ?: "")
                ?.cancel(CancelCode.User)
    }

    fun continueFromCancel() {
        setState {
            copy(userWantsToCancel = false)
        }
    }

    fun continueFromWasNotMe() {
        setState {
            copy(userThinkItsNotHim = false)
        }
    }

    fun itWasNotMe() {
        setState {
            copy(userThinkItsNotHim = true)
        }
    }

    fun goToSettings() = withState { state ->
        cancelAllPendingVerifications(state)
        _viewEvents.post(VerificationBottomSheetViewEvents.GoToSettings)
    }

    override fun handle(action: VerificationAction) = withState { state ->
        val otherUserId = state.otherUserMxItem?.id ?: return@withState
        val roomId = state.roomId
                ?: session.roomService().getExistingDirectRoomWithUser(otherUserId)

        when (action) {
            is VerificationAction.RequestVerificationByDM -> {
                if (roomId == null) {
                    val localId = LocalEcho.createLocalEchoId()
                    setState {
                        copy(
                                pendingLocalId = localId,
                                pendingRequest = Loading()
                        )
                    }
                    viewModelScope.launch {
                        val result = runCatching { session.roomService().createDirectRoom(otherUserId) }
                        result.fold(
                                { data ->
                                    setState {
                                        copy(
                                                roomId = data,
                                                pendingRequest = Success(
                                                        session
                                                                .cryptoService()
                                                                .verificationService()
                                                                .requestKeyVerificationInDMs(
                                                                        supportedVerificationMethodsProvider.provide(),
                                                                        otherUserId,
                                                                        data,
                                                                        pendingLocalId
                                                                )
                                                )
                                        )
                                    }
                                },
                                { failure ->
                                    setState {
                                        copy(pendingRequest = Fail(failure))
                                    }
                                }
                        )
                    }
                } else {
                    setState {
                        copy(
                                pendingRequest = Success(
                                        session
                                                .cryptoService()
                                                .verificationService()
                                                .requestKeyVerificationInDMs(supportedVerificationMethodsProvider.provide(), otherUserId, roomId)
                                )
                        )
                    }
                }
                Unit
            }
            is VerificationAction.StartSASVerification -> {
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
                            otherDeviceId = otherDevice ?: ""
                    )
                }
                Unit
            }
            is VerificationAction.RemoteQrCodeScanned -> {
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
            is VerificationAction.OtherUserDidNotScanned -> {
                val transactionId = state.transactionId ?: return@withState

                val existingTransaction = session.cryptoService().verificationService()
                        .getExistingTransaction(otherUserId, transactionId) as? QrCodeVerificationTransaction
                existingTransaction
                        ?.otherUserDidNotScannedMyQrCode()
            }
            is VerificationAction.SASMatchAction -> {
                (session.cryptoService().verificationService()
                        .getExistingTransaction(action.otherUserId, action.sasTransactionId)
                        as? SasVerificationTransaction)?.userHasVerifiedShortCode()
            }
            is VerificationAction.SASDoNotMatchAction -> {
                (session.cryptoService().verificationService()
                        .getExistingTransaction(action.otherUserId, action.sasTransactionId)
                        as? SasVerificationTransaction)
                        ?.shortCodeDoesNotMatch()
            }
            is VerificationAction.ReadyPendingVerification -> {
                state.pendingRequest.invoke()?.let { request ->
                    // will only be there for dm verif
                    if (state.roomId != null) {
                        session.cryptoService().verificationService()
                                .readyPendingVerificationInDMs(
                                        supportedVerificationMethodsProvider.provide(),
                                        state.otherUserId,
                                        state.roomId,
                                        request.transactionId ?: ""
                                )
                    }
                }
            }
            is VerificationAction.CancelPendingVerification -> {
                state.pendingRequest.invoke()?.let {
                    session.cryptoService().verificationService()
                            .cancelVerificationRequest(it)
                }
                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
            }
            is VerificationAction.GotItConclusion -> {
                if (state.isVerificationRequired && !action.verified) {
                    // we should go back to first screen
                    setState {
                        copy(
                                pendingRequest = Uninitialized,
                                sasTransactionState = null,
                                qrTransactionState = null
                        )
                    }
                } else {
                    _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
                }
            }
            is VerificationAction.SkipVerification -> {
                _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
            }
            is VerificationAction.VerifyFromPassphrase -> {
                setState { copy(verifyingFrom4S = true) }
                _viewEvents.post(VerificationBottomSheetViewEvents.AccessSecretStore)
            }
            is VerificationAction.GotResultFromSsss -> {
                handleSecretBackFromSSSS(action)
            }
            VerificationAction.SecuredStorageHasBeenReset -> {
                if (session.cryptoService().crossSigningService().allPrivateKeysKnown()) {
                    setState {
                        copy(quadSHasBeenReset = true, verifyingFrom4S = false)
                    }
                }
                Unit
            }
            VerificationAction.CancelledFromSsss -> {
                setState {
                    copy(verifyingFrom4S = false)
                }
            }
        }
    }

    private fun handleSecretBackFromSSSS(action: VerificationAction.GotResultFromSsss) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action.cypherData.fromBase64().inputStream().use { ins ->
                    val res = matrix.secureStorageService().loadSecureSecret<Map<String, String>>(ins, action.alias)
                    val trustResult = session.cryptoService().crossSigningService().checkTrustFromPrivateKeys(
                            res?.get(MASTER_KEY_SSSS_NAME),
                            res?.get(USER_SIGNING_KEY_SSSS_NAME),
                            res?.get(SELF_SIGNING_KEY_SSSS_NAME)
                    )
                    if (trustResult.isVerified()) {
                        // Sign this device and upload the signature
                        session.sessionParams.deviceId?.let { deviceId ->
                            session.cryptoService()
                                    .crossSigningService().trustDevice(deviceId, object : MatrixCallback<Unit> {
                                        override fun onFailure(failure: Throwable) {
                                            Timber.w(failure, "Failed to sign my device after recovery")
                                        }
                                    })
                        }

                        setState {
                            copy(
                                    verifyingFrom4S = false,
                                    verifiedFromPrivateKeys = true
                            )
                        }

                        // try the keybackup
                        tentativeRestoreBackup(res)
                    } else {
                        setState {
                            copy(
                                    verifyingFrom4S = false
                            )
                        }
                        // POP UP something
                        _viewEvents.post(VerificationBottomSheetViewEvents.ModalError(stringProvider.getString(R.string.error_failed_to_import_keys)))
                    }
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            verifyingFrom4S = false
                    )
                }
                _viewEvents.post(
                        VerificationBottomSheetViewEvents.ModalError(failure.localizedMessage ?: stringProvider.getString(R.string.unexpected_error))
                )
            }
        }
    }

    private fun tentativeRestoreBackup(res: Map<String, String>?) {
        // It's not a good idea to download the full backup, it might take very long
        // and use a lot of resources
        // Just check that the key is valid and store it, the backup will be used megolm session per
        // megolm session when an UISI is encountered

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val secret = res?.get(KEYBACKUP_SECRET_SSSS_NAME) ?: return@launch Unit.also {
                    Timber.v("## Keybackup secret not restored from SSSS")
                }

                val version = awaitCallback<KeysBackupLastVersionResult> {
                    session.cryptoService().keysBackupService().getCurrentVersion(it)
                }.toKeysVersionResult() ?: return@launch

                val recoveryKey = computeRecoveryKey(secret.fromBase64())
                val isValid = awaitCallback<Boolean> {
                    session.cryptoService().keysBackupService().isValidRecoveryKeyForCurrentVersion(recoveryKey, it)
                }
                if (isValid) {
                    session.cryptoService().keysBackupService().saveBackupRecoveryKey(recoveryKey, version.version)
                }
                awaitCallback<Unit> {
                    session.cryptoService().keysBackupService().trustKeysBackupVersion(version, true, it)
                }
            } catch (failure: Throwable) {
                // Just ignore for now
                Timber.e(failure, "## Failed to restore backup after SSSS recovery")
            }
        }
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
            is SasVerificationTransaction -> {
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
                            .readyPendingVerification(
                                    supportedVerificationMethodsProvider.provide(),
                                    pr.otherUserId,
                                    pr.transactionId ?: ""
                            )
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

        if (pr.localId == state.pendingLocalId ||
                pr.localId == state.pendingRequest.invoke()?.localId ||
                state.pendingRequest.invoke()?.transactionId == pr.transactionId) {
            setState {
                copy(
                        transactionId = state.verificationId ?: pr.transactionId,
                        pendingRequest = Success(pr)
                )
            }
        }
    }
}
