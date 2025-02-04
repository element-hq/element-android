/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.nulabinc.zxcvbn.Zxcvbn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.raw.wellknown.SecureBackupMethod
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isSecureBackupRequired
import im.vector.app.features.raw.wellknown.secureBackupMethod
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.securestorage.RawBytesKeySpec
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import timber.log.Timber
import java.io.OutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

class BootstrapSharedViewModel @AssistedInject constructor(
        @Assisted initialState: BootstrapViewState,
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter,
        private val session: Session,
        private val rawService: RawService,
        private val bootstrapTask: BootstrapCrossSigningTask,
        private val migrationTask: BackupToQuadSMigrationTask,
        private val pendingAuthHandler: PendingAuthHandler,
) : VectorViewModel<BootstrapViewState, BootstrapActions, BootstrapViewEvents>(initialState) {

    private var doesKeyBackupExist: Boolean = false
    private var isBackupCreatedFromPassphrase: Boolean = false
    private val zxcvbn = Zxcvbn()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<BootstrapSharedViewModel, BootstrapViewState> {
        override fun create(initialState: BootstrapViewState): BootstrapSharedViewModel
    }

    companion object : MavericksViewModelFactory<BootstrapSharedViewModel, BootstrapViewState> by hiltMavericksViewModelFactory()

    init {

        setState {
            copy(step = BootstrapStep.CheckingMigration, isRecoverySetup = session.sharedSecretStorageService().isRecoverySetup())
        }

        // Refresh the well-known configuration
        viewModelScope.launch(Dispatchers.IO) {
            val wellKnown = rawService.getElementWellknown(session.sessionParams)
            setState {
                copy(
                        isSecureBackupRequired = wellKnown?.isSecureBackupRequired().orFalse(),
                        secureBackupMethod = wellKnown?.secureBackupMethod() ?: SecureBackupMethod.KEY_OR_PASSPHRASE,
                )
            }
        }

        when (initialState.setupMode) {
            SetupMode.PASSPHRASE_RESET,
            SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET,
            SetupMode.HARD_RESET -> {
                setState {
                    copy(
                            step = BootstrapStep.FirstForm(
                                    keyBackUpExist = false,
                                    reset = session.sharedSecretStorageService().isRecoverySetup(),
                                    methods = this.secureBackupMethod
                            )
                    )
                }
            }
            SetupMode.CROSS_SIGNING_ONLY -> {
                // Go straight to account password
                setState {
                    copy(step = BootstrapStep.AccountReAuth())
                }
            }
            SetupMode.NORMAL -> {
                checkMigration()
            }
        }
    }

    private fun checkMigration() {
        // need to check if user have an existing keybackup
        setState {
            copy(step = BootstrapStep.CheckingMigration)
        }

        // We need to check if there is an existing backup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val version = tryOrNull { session.cryptoService().keysBackupService().getCurrentVersion() }?.toKeysVersionResult()
                if (version == null) {
                    // we just resume plain bootstrap
                    doesKeyBackupExist = false
                    setState {
                        copy(step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist, methods = this.secureBackupMethod))
                    }
                } else {
                    // we need to get existing backup passphrase/key and convert to SSSS
                    val keyVersion = tryOrNull {
                        session.cryptoService().keysBackupService().getVersion(version.version)
                    }
                    if (keyVersion == null) {
                        // strange case... just finish?
                        _viewEvents.post(BootstrapViewEvents.Dismiss(false))
                    } else {
                        doesKeyBackupExist = true
                        isBackupCreatedFromPassphrase = keyVersion.getAuthDataAsMegolmBackupAuthData()?.privateKeySalt != null
                        setState {
                            copy(step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist, methods = this.secureBackupMethod))
                        }
                    }
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "Error while checking key backup")
                setState {
                    copy(step = BootstrapStep.Error(failure))
                }
            }
        }
    }

    private fun handleStartMigratingKeyBackup() {
        if (isBackupCreatedFromPassphrase) {
            setState {
                copy(step = BootstrapStep.GetBackupSecretPassForMigration(useKey = false))
            }
        } else {
            setState {
                copy(step = BootstrapStep.GetBackupSecretKeyForMigration)
            }
        }
    }

    override fun handle(action: BootstrapActions) = withState { state ->
        when (action) {
            is BootstrapActions.GoBack -> queryBack()
            BootstrapActions.StartKeyBackupMigration -> {
                handleStartMigratingKeyBackup()
            }
            is BootstrapActions.Start -> {
                handleStart(action)
            }
            is BootstrapActions.UpdateCandidatePassphrase -> {
                val strength = zxcvbn.measure(action.pass)
                setState {
                    copy(
                            passphrase = action.pass,
                            passphraseStrength = Success(strength)
                    )
                }
            }
            is BootstrapActions.GoToConfirmPassphrase -> {
                setState {
                    copy(
                            passphrase = action.passphrase,
                            step = BootstrapStep.ConfirmPassphrase
                    )
                }
            }
            is BootstrapActions.UpdateConfirmCandidatePassphrase -> {
                setState {
                    copy(
                            passphraseRepeat = action.pass
                    )
                }
            }
            is BootstrapActions.DoInitialize -> {
                if (state.passphrase == state.passphraseRepeat) {
                    startInitializeFlow(state)
                } else {
                    setState {
                        copy(
                                passphraseConfirmMatch = Fail(Throwable(stringProvider.getString(CommonStrings.passphrase_passphrase_does_not_match)))
                        )
                    }
                }
            }
            is BootstrapActions.DoInitializeGeneratedKey -> {
                startInitializeFlow(state)
            }
            BootstrapActions.RecoveryKeySaved -> {
                _viewEvents.post(BootstrapViewEvents.RecoveryKeySaved)
                setState {
                    copy(step = BootstrapStep.SaveRecoveryKey(true))
                }
            }
            BootstrapActions.Completed -> {
                _viewEvents.post(BootstrapViewEvents.Dismiss(true))
            }
            BootstrapActions.GoToCompleted -> {
                setState {
                    copy(step = BootstrapStep.DoneSuccess)
                }
            }
            BootstrapActions.SaveReqQueryStarted -> {
                setState {
                    copy(recoverySaveFileProcess = Loading())
                }
            }
            is BootstrapActions.SaveKeyToUri -> {
                saveRecoveryKeyToUri(action.os)
            }
            BootstrapActions.SaveReqFailed -> {
                setState {
                    copy(recoverySaveFileProcess = Uninitialized)
                }
            }
            BootstrapActions.GoToEnterAccountPassword -> {
                setState {
                    copy(step = BootstrapStep.AccountReAuth())
                }
            }
            BootstrapActions.HandleForgotBackupPassphrase -> {
                if (state.step is BootstrapStep.GetBackupSecretPassForMigration) {
                    setState {
                        copy(step = BootstrapStep.GetBackupSecretPassForMigration(true))
                    }
                } else return@withState
            }
//            is BootstrapActions.ReAuth                           -> {
//                startInitializeFlow(action.pass)
//            }
            is BootstrapActions.DoMigrateWithPassphrase -> {
                startMigrationFlow(state.step, action.passphrase, null)
            }
            is BootstrapActions.DoMigrateWithRecoveryKey -> {
                startMigrationFlow(state.step, null, action.recoveryKey)
            }
            BootstrapActions.SsoAuthDone -> pendingAuthHandler.ssoAuthDone()
            is BootstrapActions.PasswordAuthDone -> pendingAuthHandler.passwordAuthDone(action.password)
            BootstrapActions.ReAuthCancelled -> {
                pendingAuthHandler.reAuthCancelled()
                setState {
                    copy(step = BootstrapStep.AccountReAuth(stringProvider.getString(CommonStrings.authentication_error)))
                }
            }
            BootstrapActions.Retry -> {
                checkMigration()
            }
        }
    }

    private fun handleStart(action: BootstrapActions.Start) = withState {
        if (action.userWantsToEnterPassphrase) {
            setState {
                copy(
                        step = BootstrapStep.SetupPassphrase
                )
            }
        } else {
            startInitializeFlow(it)
        }
    }

    // =======================================
    // Business Logic
    // =======================================
    private fun saveRecoveryKeyToUri(os: OutputStream) = withState { state ->
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                os.use {
                    os.write((state.recoveryKeyCreationInfo?.recoveryKey?.formatRecoveryKey() ?: "").toByteArray())
                }
            }.fold({
                setState {
                    _viewEvents.post(BootstrapViewEvents.RecoveryKeySaved)
                    copy(
                            recoverySaveFileProcess = Success(Unit),
                            step = BootstrapStep.SaveRecoveryKey(isSaved = true)
                    )
                }
            }, {
                setState {
                    copy(recoverySaveFileProcess = Fail(it))
                }
            })
        }
    }

    private fun startMigrationFlow(previousStep: BootstrapStep, passphrase: String?, recoveryKey: String?) { // TODO Rename param
        setState {
            copy(step = BootstrapStep.Initializing)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val progressListener = object : BootstrapProgressListener {
                override fun onProgress(data: WaitingViewData) {
                    setState {
                        copy(
                                initializationWaitingViewData = data
                        )
                    }
                }
            }
            migrationTask.invoke(this, BackupToQuadSMigrationTask.Params(passphrase, recoveryKey, progressListener)) {
                when (it) {
                    is BackupToQuadSMigrationTask.Result.Success -> {
                        setState {
                            copy(
                                    passphrase = passphrase,
                                    passphraseRepeat = passphrase,
                                    migrationRecoveryKey = recoveryKey
                            )
                        }
//                        val userPassword = reAuthHelper.data
//                        if (userPassword == null) {
//                            setState {
//                                copy(
//                                        step = BootstrapStep.AccountPassword(false)
//                                )
//                            }
//                        } else {
                        withState { startInitializeFlow(it) }
//                        }
                    }
                    is BackupToQuadSMigrationTask.Result.Failure -> {
                        _viewEvents.post(
                                BootstrapViewEvents.ModalError(it.toHumanReadable())
                        )
                        setState {
                            copy(
                                    step = previousStep
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startInitializeFlow(state: BootstrapViewState) {
        val previousStep = state.step

        setState {
            copy(step = BootstrapStep.Initializing)
        }

        val progressListener = object : BootstrapProgressListener {
            override fun onProgress(data: WaitingViewData) {
                setState {
                    copy(
                            initializationWaitingViewData = data
                    )
                }
            }
        }

        val interceptor = object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                when (flowResponse.nextUncompletedStage()) {
                    LoginFlowTypes.PASSWORD -> {
                        pendingAuthHandler.pendingAuth = UserPasswordAuth(
                                // Note that _pendingSession may or may not be null, this is OK, it will be managed by the task
                                session = flowResponse.session,
                                user = session.myUserId,
                                password = null
                        )
                        pendingAuthHandler.uiaContinuation = promise
                        setState {
                            copy(
                                    step = BootstrapStep.AccountReAuth()
                            )
                        }
                        _viewEvents.post(BootstrapViewEvents.RequestReAuth(flowResponse, errCode))
                    }
                    LoginFlowTypes.SSO -> {
                        pendingAuthHandler.pendingAuth = DefaultBaseAuth(flowResponse.session)
                        pendingAuthHandler.uiaContinuation = promise
                        setState {
                            copy(
                                    step = BootstrapStep.AccountReAuth()
                            )
                        }
                        _viewEvents.post(BootstrapViewEvents.RequestReAuth(flowResponse, errCode))
                    }
                    else -> {
                        promise.resumeWithException(UnsupportedOperationException())
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            bootstrapTask.invoke(
                    this,
                    Params(
                            userInteractiveAuthInterceptor = interceptor,
                            progressListener = progressListener,
                            passphrase = state.passphrase,
                            keySpec = state.migrationRecoveryKey?.let { extractCurveKeyFromRecoveryKey(it)?.let { RawBytesKeySpec(it) } },
                            forceResetIfSomeSecretsAreMissing = state.isSecureBackupRequired,
                            setupMode = state.setupMode
                    )
            ) { bootstrapResult ->
                when (bootstrapResult) {
                    is BootstrapResult.SuccessCrossSigningOnly -> {
                        _viewEvents.post(BootstrapViewEvents.Dismiss(true))
                    }
                    is BootstrapResult.Success -> {
                        val isSecureBackupRequired = state.isSecureBackupRequired
                        val secureBackupMethod = state.secureBackupMethod

                        if (state.passphrase != null && isSecureBackupRequired && secureBackupMethod == SecureBackupMethod.PASSPHRASE) {
                            // Go straight to conclusion, skip the save key step
                            _viewEvents.post(BootstrapViewEvents.Dismiss(success = true))
                        } else {
                            setState {
                                copy(
                                        recoveryKeyCreationInfo = bootstrapResult.keyInfo,
                                        step = BootstrapStep.SaveRecoveryKey(
                                                // If a passphrase was used, saving key is optional
                                                state.passphrase != null
                                        )
                                )
                            }
                        }
                    }
                    is BootstrapResult.InvalidPasswordError -> {
                        // it's a bad password / auth
                        setState {
                            copy(
                                    step = BootstrapStep.AccountReAuth(stringProvider.getString(CommonStrings.auth_invalid_login_param))
                            )
                        }
                    }
                    is BootstrapResult.Failure -> {
                        if (bootstrapResult is BootstrapResult.GenericError &&
                                bootstrapResult.failure is Failure.OtherServerError &&
                                bootstrapResult.failure.httpCode == 401) {
                            // Ignore this error
                        } else {
                            _viewEvents.post(BootstrapViewEvents.ModalError(bootstrapResult.error ?: stringProvider.getString(CommonStrings.matrix_error)))
                            // Not sure
                            setState {
                                copy(
                                        step = previousStep
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =======================================
    // Fragment interaction
    // =======================================

    private fun queryBack() = withState { state ->
        when (state.step) {
            is BootstrapStep.GetBackupSecretPassForMigration -> {
                if (state.step.useKey) {
                    // go back to passphrase
                    setState {
                        copy(
                                step = BootstrapStep.GetBackupSecretPassForMigration(
                                        useKey = false
                                )
                        )
                    }
                } else {
                    setState {
                        copy(
                                step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist, methods = this.secureBackupMethod),
                                // Also reset the passphrase
                                passphrase = null,
                                passphraseRepeat = null,
                                // Also reset the key
                                migrationRecoveryKey = null
                        )
                    }
                }
            }
            is BootstrapStep.SetupPassphrase -> {
                setState {
                    copy(
                            step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist, methods = this.secureBackupMethod),
                            // Also reset the passphrase
                            passphrase = null,
                            passphraseRepeat = null
                    )
                }
            }
            is BootstrapStep.ConfirmPassphrase -> {
                setState {
                    copy(
                            step = BootstrapStep.SetupPassphrase
                    )
                }
            }
            is BootstrapStep.AccountReAuth -> {
                if (state.canLeave) {
                    _viewEvents.post(BootstrapViewEvents.SkipBootstrap(state.passphrase != null))
                } else {
                    // Go back to the first step
                    setState {
                        copy(
                                step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist, methods = this.secureBackupMethod),
                                // Also reset the passphrase
                                passphrase = null,
                                passphraseRepeat = null
                        )
                    }
                }
            }
            BootstrapStep.Initializing -> {
                // do we let you cancel from here?
                if (state.canLeave) {
                    _viewEvents.post(BootstrapViewEvents.SkipBootstrap(state.passphrase != null))
                }
            }
            is BootstrapStep.SaveRecoveryKey,
            BootstrapStep.DoneSuccess -> {
                // nop
            }
            BootstrapStep.CheckingMigration -> Unit
            is BootstrapStep.FirstForm -> {
                if (state.canLeave) {
                    _viewEvents.post(
                            when (state.setupMode) {
                                SetupMode.CROSS_SIGNING_ONLY,
                                SetupMode.NORMAL -> BootstrapViewEvents.SkipBootstrap()
                                else -> BootstrapViewEvents.Dismiss(success = false)
                            }
                    )
                }
            }
            is BootstrapStep.GetBackupSecretForMigration -> {
                setState {
                    copy(
                            step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist, methods = this.secureBackupMethod),
                            // Also reset the passphrase
                            passphrase = null,
                            passphraseRepeat = null,
                            // Also reset the key
                            migrationRecoveryKey = null
                    )
                }
            }
            is BootstrapStep.Error -> {
                // do we let you cancel from here?
                if (state.canLeave) {
                    _viewEvents.post(BootstrapViewEvents.SkipBootstrap(state.passphrase != null))
                }
            }
        }
    }

    private fun BackupToQuadSMigrationTask.Result.Failure.toHumanReadable(): String {
        return when (this) {
            is BackupToQuadSMigrationTask.Result.InvalidRecoverySecret -> stringProvider.getString(CommonStrings.keys_backup_passphrase_error_decrypt)
            is BackupToQuadSMigrationTask.Result.ErrorFailure -> errorFormatter.toHumanReadable(throwable)
            // is BackupToQuadSMigrationTask.Result.NoKeyBackupVersion,
            // is BackupToQuadSMigrationTask.Result.IllegalParams,
            else -> stringProvider.getString(CommonStrings.unexpected_error)
        }
    }
}

private val BootstrapViewState.canLeave: Boolean get() = !isSecureBackupRequired || isRecoverySetup
