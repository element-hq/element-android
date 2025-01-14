/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.self

import android.content.res.Resources.NotFoundException
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
import im.vector.app.features.crypto.verification.user.VerificationTransactionData
import im.vector.app.features.crypto.verification.user.toDataClass
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isSecureBackupRequired
import im.vector.app.features.session.coroutineScope
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
import org.matrix.android.sdk.api.session.crypto.keysbackup.BackupUtils
import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
import org.matrix.android.sdk.api.session.crypto.verification.getTransaction
import org.matrix.android.sdk.api.util.fromBase64
import timber.log.Timber

sealed class UserAction {
    object ConfirmCancel : UserAction()
    object None : UserAction()
}

data class SelfVerificationViewState(
        val activeAction: UserAction = UserAction.None,
        val pendingRequest: Async<PendingVerificationRequest> = Uninitialized,
        // need something immutable for state to work properly, VerificationTransaction is not
        val startedTransaction: Async<VerificationTransactionData> = Uninitialized,
        val verifyingFrom4SAction: Async<Boolean> = Uninitialized,
        val otherDeviceId: String? = null,
        val transactionId: String? = null,
        val currentDeviceCanCrossSign: Boolean = false,
        val userWantsToCancel: Boolean = false,
        val hasAnyOtherSession: Async<Boolean> = Uninitialized,
        val quadSContainsSecrets: Boolean = false,
        val isVerificationRequired: Boolean = false,
        val isThisSessionVerified: Boolean = false,
) : MavericksState {

    constructor(args: SelfVerificationBottomSheet.Args) : this(
            transactionId = args.transactionId,
            otherDeviceId = args.targetDevice,
    )
}

class SelfVerificationViewModel @AssistedInject constructor(
        @Assisted private val initialState: SelfVerificationViewState,
        private val session: Session,
        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider,
        private val rawService: RawService,
        private val stringProvider: StringProvider,
        private val matrix: Matrix,
) :
        VectorViewModel<SelfVerificationViewState, VerificationAction, VerificationBottomSheetViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SelfVerificationViewModel, SelfVerificationViewState> {
        override fun create(initialState: SelfVerificationViewState): SelfVerificationViewModel
    }

    companion object : MavericksViewModelFactory<SelfVerificationViewModel, SelfVerificationViewState> by hiltMavericksViewModelFactory()

    init {

        if (initialState.transactionId != null) {
            setState {
                copy(pendingRequest = Loading())
            }
            viewModelScope.launch {
                val matchingRequest = session.cryptoService().verificationService().getExistingVerificationRequest(session.myUserId, initialState.transactionId)
                if (matchingRequest == null) {
                    // it's unexpected for now dismiss.
                    // Could happen when you click on the popup for an incoming request
                    // that has been deleted by the time you clicked on it
                    // maybe give some feedback?
                    setState {
                        copy(pendingRequest = Fail(NotFoundException()))
                    }
                    _viewEvents.post(VerificationBottomSheetViewEvents.RequestNotFound(initialState.transactionId))
                } else {
                    setState {
                        copy(pendingRequest = Success(matchingRequest))
                    }
                }
            }
        }
        observeRequestsAndTransactions()
        // This is async, but at this point should be in cache
        // so it's ok to not wait until result
        viewModelScope.launch(Dispatchers.IO) {
            val wellKnown = rawService.getElementWellknown(session.sessionParams)
            setState {
                copy(isVerificationRequired = wellKnown?.isSecureBackupRequired().orFalse())
            }
        }

        setState { copy(hasAnyOtherSession = Loading()) }
        viewModelScope.launch {
            val hasAnyOtherSession = session.cryptoService()
                    .getCryptoDeviceInfo(session.myUserId)
                    .any {
                        it.deviceId != session.sessionParams.deviceId
                    }
            setState {
                copy(
                        hasAnyOtherSession = Success(hasAnyOtherSession)
                )
            }
        }

        setState {
            copy(
                    currentDeviceCanCrossSign = session.cryptoService().crossSigningService().canCrossSign(),
                    quadSContainsSecrets = session.sharedSecretStorageService().isRecoverySetup(),
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val isThisSessionVerified = session.cryptoService().crossSigningService().isCrossSigningVerified()
            setState {
                copy(
                        isThisSessionVerified = isThisSessionVerified,
                )
            }
        }
//
    }

    private fun observeRequestsAndTransactions() {
        session.cryptoService().verificationService()
                .requestEventFlow()
                .filter {
                    it.otherUserId == session.myUserId
                }
                .onEach {
                    it.getRequest()?.let {
                        setState {
                            copy(
                                    pendingRequest = Success(it),
                            )
                        }
                    }
                    it.getTransaction()?.let {
                        val dClass = it.toDataClass()
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
            VerificationAction.CancelledFromSsss -> {
                setState {
                    copy(verifyingFrom4SAction = Uninitialized)
                }
            }
            is VerificationAction.GotItConclusion -> {
                withState { state ->
                    if (action.verified || state.isThisSessionVerified) {
                        _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
                    } else {
                        queryCancel()
                    }
                }
            }
            is VerificationAction.GotResultFromSsss -> handleSecretBackFromSSSS(action)
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
            is VerificationAction.SASDoNotMatchAction -> {
                withState { state ->
                    viewModelScope.launch {
                        val transaction = session.cryptoService().verificationService()
                                .getExistingTransaction(session.myUserId, state.transactionId.orEmpty())
                        (transaction as? SasVerificationTransaction)?.shortCodeDoesNotMatch()
                    }
                }
            }
            is VerificationAction.SASMatchAction -> {
                withState { state ->
                    viewModelScope.launch {
                        val transaction = session.cryptoService().verificationService()
                                .getExistingTransaction(session.myUserId, state.transactionId.orEmpty())
                        (transaction as? SasVerificationTransaction)?.userHasVerifiedShortCode()
                    }
                }
            }
            VerificationAction.SecuredStorageHasBeenReset -> {
                if (session.cryptoService().crossSigningService().allPrivateKeysKnown()) {
                    _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
                }
            }
            VerificationAction.SkipVerification -> {
                queryCancel()
            }
            VerificationAction.ForgotResetAll -> {
                _viewEvents.post(VerificationBottomSheetViewEvents.ResetAll)
            }
            VerificationAction.StartSASVerification -> {
                withState { state ->
                    val request = state.pendingRequest.invoke() ?: return@withState
                    viewModelScope.launch {
                        session.cryptoService().verificationService()
                                .startKeyVerification(VerificationMethod.SAS, session.myUserId, request.transactionId)
                    }
                }
            }
            VerificationAction.VerifyFromPassphrase -> {
                setState { copy(verifyingFrom4SAction = Loading()) }
                _viewEvents.post(VerificationBottomSheetViewEvents.AccessSecretStore)
            }
            VerificationAction.FailedToGetKeysFrom4S -> {
                setState {
                    copy(
                            verifyingFrom4SAction = Uninitialized,
                            quadSContainsSecrets = false
                    )
                }
            }
            VerificationAction.RequestSelfVerification -> {
                handleRequestVerification()
            }
            VerificationAction.RequestVerificationByDM -> {
                // not applicable in self verification
            }
            VerificationAction.SelfVerificationWasNotMe -> {
                // cancel transaction then open settings
                withState { state ->
                    val request = state.pendingRequest.invoke() ?: return@withState
                    session.coroutineScope.launch {
                        tryOrNull {
                            session.cryptoService().verificationService().cancelVerificationRequest(request)
                        }
                    }
                    _viewEvents.post(VerificationBottomSheetViewEvents.DismissAndOpenDeviceSettings)
                }
            }
        }
    }

    fun queryCancel() = withState { state ->
        when (state.pendingRequest) {
            is Uninitialized -> {
                // No active request currently
                if (state.isVerificationRequired) {
                    // we can't let you dismiss :)
                } else {
                    // verification is not required so just dismiss
                    _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
                }
            }
            is Success -> {
                val activeRequest = state.pendingRequest.invoke()
                // there is an active request or transaction, we need confirmation to cancel it
                if (activeRequest.state == EVerificationState.Cancelled) {
                    // equivalent of got it?
                    if (state.isThisSessionVerified) {
                        // we can always dismiss??
                        _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
                    } else {
                        // go back to main verification screen
                        setState {
                            copy(pendingRequest = Uninitialized)
                        }
                    }
                    return@withState
                } else if (activeRequest.state == EVerificationState.Done) {
                    _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
                } else {
                    if (state.isThisSessionVerified) {
                        // we are the verified session, so just dismiss?
                        // do we want to prompt confirm??
                        _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
                    } else {
                        // cancel the active request and go back?
                        setState {
                            copy(
                                    transactionId = null,
                                    pendingRequest = Uninitialized,
                            )
                        }
                        viewModelScope.launch(Dispatchers.IO) {
                            session.cryptoService().verificationService().cancelVerificationRequest(
                                    activeRequest
                            )
                            setState {
                                copy(
                                        transactionId = null,
                                        pendingRequest = Uninitialized
                                )
                            }
                        }
                    }
                }
//                if (state.isThisSessionVerified) {
//                    // we can always dismiss??
//                    _viewEvents.post(VerificationBottomSheetViewEvents.Dismiss)
//                }
//                setState {
//                    copy(
//                            activeAction = UserAction.ConfirmCancel
//                    )
//                }
            }
            else -> {
                // just ignore?
            }
        }
    }

    private fun handleRequestVerification() {
        setState {
            copy(
                    pendingRequest = Loading()
            )
        }
        var targetDevice: String? = null
        withState { state ->
            targetDevice = state.otherDeviceId
        }
        viewModelScope.launch {
            try {
                val request = session.cryptoService().verificationService().requestDeviceVerification(
                        supportedVerificationMethodsProvider.provide(),
                        session.myUserId,
                        targetDevice
                )
                setState {
                    copy(
                            pendingRequest = Success(request),
                            transactionId = request.transactionId
                    )
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            pendingRequest = Loading(),
                    )
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
                        try {
                            session.cryptoService().crossSigningService().trustDevice(session.sessionParams.deviceId)
                        } catch (failure: Exception) {
                            Timber.w(failure, "Failed to sign my device after recovery")
                        }

                        setState {
                            copy(
                                    verifyingFrom4SAction = Success(true),
                            )
                        }
                        tentativeRestoreBackup(res)
                    } else {
                        setState {
                            copy(
                                    verifyingFrom4SAction = Success(false),
                            )
                        }
                    }
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            verifyingFrom4SAction = Fail(failure)
                    )
                }
                _viewEvents.post(
                        VerificationBottomSheetViewEvents.ModalError(failure.localizedMessage ?: stringProvider.getString(CommonStrings.unexpected_error))
                )
            }
        }
    }

    private fun tentativeRestoreBackup(res: Map<String, String>?) {
        // on session scope because will happen after viewmodel is cleared
        session.coroutineScope.launch {
            // It's not a good idea to download the full backup, it might take very long
            // and use a lot of resources
            // Just check that the key is valid and store it, the backup will be used megolm session per
            // megolm session when an UISI is encountered
            try {
                val secret = res?.get(KEYBACKUP_SECRET_SSSS_NAME) ?: return@launch Unit.also {
                    Timber.v("## Keybackup secret not restored from SSSS")
                }

                val version = session.cryptoService().keysBackupService().getCurrentVersion()?.toKeysVersionResult() ?: return@launch

                val recoveryKey = computeRecoveryKey(secret.fromBase64())
                val backupRecoveryKey = BackupUtils.recoveryKeyFromBase58(recoveryKey)
                val isValid = backupRecoveryKey
                        .let { session.cryptoService().keysBackupService().isValidRecoveryKeyForCurrentVersion(it) }
                if (isValid) {
                    session.cryptoService().keysBackupService().saveBackupRecoveryKey(backupRecoveryKey, version.version)
                    // session.cryptoService().keysBackupService().trustKeysBackupVersion(version, true)
                }
            } catch (failure: Throwable) {
                // Just ignore for now
                Timber.e(failure, "## Failed to restore backup after SSSS recovery")
            }
        }
    }
}
