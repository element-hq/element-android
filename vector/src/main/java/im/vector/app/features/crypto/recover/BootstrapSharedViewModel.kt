/*
 * Copyright (c) 2020 New Vector Ltd
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
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.auth.ReAuthActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.securestorage.RawBytesKeySpec
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.keysbackup.model.KeysBackupLastVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.toKeysVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.util.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.internal.crypto.model.rest.DefaultBaseAuth
import org.matrix.android.sdk.internal.util.awaitCallback
import java.io.OutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BootstrapSharedViewModel @AssistedInject constructor(
        @Assisted initialState: BootstrapViewState,
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter,
        private val session: Session,
        private val bootstrapTask: BootstrapCrossSigningTask,
        private val migrationTask: BackupToQuadSMigrationTask,
) : VectorViewModel<BootstrapViewState, BootstrapActions, BootstrapViewEvents>(initialState) {

    private var doesKeyBackupExist: Boolean = false
    private var isBackupCreatedFromPassphrase: Boolean = false
    private val zxcvbn = Zxcvbn()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<BootstrapSharedViewModel, BootstrapViewState> {
        override fun create(initialState: BootstrapViewState): BootstrapSharedViewModel
    }

    companion object : MavericksViewModelFactory<BootstrapSharedViewModel, BootstrapViewState> by hiltMavericksViewModelFactory()

//    private var _pendingSession: String? = null

    var uiaContinuation: Continuation<UIABaseAuth>? = null
    var pendingAuth: UIABaseAuth? = null

    init {

        when (initialState.setupMode) {
            SetupMode.PASSPHRASE_RESET,
            SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET,
            SetupMode.HARD_RESET         -> {
                setState {
                    copy(step = BootstrapStep.FirstForm(keyBackUpExist = false, reset = true))
                }
            }
            SetupMode.CROSS_SIGNING_ONLY -> {
                // Go straight to account password
                setState {
                    copy(step = BootstrapStep.AccountReAuth())
                }
            }
            SetupMode.NORMAL             -> {
                // need to check if user have an existing keybackup
                setState {
                    copy(step = BootstrapStep.CheckingMigration)
                }

                // We need to check if there is an existing backup
                viewModelScope.launch(Dispatchers.IO) {
                    val version = awaitCallback<KeysBackupLastVersionResult> {
                        session.cryptoService().keysBackupService().getCurrentVersion(it)
                    }.toKeysVersionResult()
                    if (version == null) {
                        // we just resume plain bootstrap
                        doesKeyBackupExist = false
                        setState {
                            copy(step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist))
                        }
                    } else {
                        // we need to get existing backup passphrase/key and convert to SSSS
                        val keyVersion = awaitCallback<KeysVersionResult?> {
                            session.cryptoService().keysBackupService().getVersion(version.version, it)
                        }
                        if (keyVersion == null) {
                            // strange case... just finish?
                            _viewEvents.post(BootstrapViewEvents.Dismiss(false))
                        } else {
                            doesKeyBackupExist = true
                            isBackupCreatedFromPassphrase = keyVersion.getAuthDataAsMegolmBackupAuthData()?.privateKeySalt != null
                            setState {
                                copy(step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist))
                            }
                        }
                    }
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
            is BootstrapActions.GoBack                           -> queryBack()
            BootstrapActions.StartKeyBackupMigration             -> {
                handleStartMigratingKeyBackup()
            }
            is BootstrapActions.Start                            -> {
                handleStart(action)
            }
            is BootstrapActions.UpdateCandidatePassphrase        -> {
                val strength = zxcvbn.measure(action.pass)
                setState {
                    copy(
                            passphrase = action.pass,
                            passphraseStrength = Success(strength)
                    )
                }
            }
            is BootstrapActions.GoToConfirmPassphrase            -> {
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
            is BootstrapActions.DoInitialize                     -> {
                if (state.passphrase == state.passphraseRepeat) {
                    startInitializeFlow(state)
                } else {
                    setState {
                        copy(
                                passphraseConfirmMatch = Fail(Throwable(stringProvider.getString(R.string.passphrase_passphrase_does_not_match)))
                        )
                    }
                }
            }
            is BootstrapActions.DoInitializeGeneratedKey         -> {
                startInitializeFlow(state)
            }
            BootstrapActions.RecoveryKeySaved                    -> {
                _viewEvents.post(BootstrapViewEvents.RecoveryKeySaved)
                setState {
                    copy(step = BootstrapStep.SaveRecoveryKey(true))
                }
            }
            BootstrapActions.Completed                           -> {
                _viewEvents.post(BootstrapViewEvents.Dismiss(true))
            }
            BootstrapActions.GoToCompleted                       -> {
                setState {
                    copy(step = BootstrapStep.DoneSuccess)
                }
            }
            BootstrapActions.SaveReqQueryStarted                 -> {
                setState {
                    copy(recoverySaveFileProcess = Loading())
                }
            }
            is BootstrapActions.SaveKeyToUri                     -> {
                saveRecoveryKeyToUri(action.os)
            }
            BootstrapActions.SaveReqFailed                       -> {
                setState {
                    copy(recoverySaveFileProcess = Uninitialized)
                }
            }
            BootstrapActions.GoToEnterAccountPassword            -> {
                setState {
                    copy(step = BootstrapStep.AccountReAuth())
                }
            }
            BootstrapActions.HandleForgotBackupPassphrase        -> {
                if (state.step is BootstrapStep.GetBackupSecretPassForMigration) {
                    setState {
                        copy(step = BootstrapStep.GetBackupSecretPassForMigration(true))
                    }
                } else return@withState
            }
//            is BootstrapActions.ReAuth                           -> {
//                startInitializeFlow(action.pass)
//            }
            is BootstrapActions.DoMigrateWithPassphrase          -> {
                startMigrationFlow(state.step, action.passphrase, null)
            }
            is BootstrapActions.DoMigrateWithRecoveryKey         -> {
                startMigrationFlow(state.step, null, action.recoveryKey)
            }
            BootstrapActions.SsoAuthDone                         -> {
                uiaContinuation?.resume(DefaultBaseAuth(session = pendingAuth?.session ?: ""))
            }
            is BootstrapActions.PasswordAuthDone                 -> {
                val decryptedPass = session.loadSecureSecret<String>(action.password.fromBase64().inputStream(), ReAuthActivity.DEFAULT_RESULT_KEYSTORE_ALIAS)
                uiaContinuation?.resume(
                        UserPasswordAuth(
                                session = pendingAuth?.session,
                                password = decryptedPass,
                                user = session.myUserId
                        )
                )
            }
            BootstrapActions.ReAuthCancelled                     -> {
                setState {
                    copy(step = BootstrapStep.AccountReAuth(stringProvider.getString(R.string.authentication_error)))
                }
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
                        pendingAuth = UserPasswordAuth(
                                // Note that _pendingSession may or may not be null, this is OK, it will be managed by the task
                                session = flowResponse.session,
                                user = session.myUserId,
                                password = null
                        )
                        uiaContinuation = promise
                        setState {
                            copy(
                                    step = BootstrapStep.AccountReAuth()
                            )
                        }
                        _viewEvents.post(BootstrapViewEvents.RequestReAuth(flowResponse, errCode))
                    }
                    LoginFlowTypes.SSO      -> {
                        pendingAuth = DefaultBaseAuth(flowResponse.session)
                        uiaContinuation = promise
                        setState {
                            copy(
                                    step = BootstrapStep.AccountReAuth()
                            )
                        }
                        _viewEvents.post(BootstrapViewEvents.RequestReAuth(flowResponse, errCode))
                    }
                    else                    -> {
                        promise.resumeWithException(UnsupportedOperationException())
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            bootstrapTask.invoke(this,
                    Params(
                            userInteractiveAuthInterceptor = interceptor,
                            progressListener = progressListener,
                            passphrase = state.passphrase,
                            keySpec = state.migrationRecoveryKey?.let { extractCurveKeyFromRecoveryKey(it)?.let { RawBytesKeySpec(it) } },
                            setupMode = state.setupMode
                    )
            ) { bootstrapResult ->
                when (bootstrapResult) {
                    is BootstrapResult.SuccessCrossSigningOnly -> {
                        _viewEvents.post(BootstrapViewEvents.Dismiss(true))
                    }
                    is BootstrapResult.Success                 -> {
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
                    is BootstrapResult.InvalidPasswordError    -> {
                        // it's a bad password / auth
                        setState {
                            copy(
                                    step = BootstrapStep.AccountReAuth(stringProvider.getString(R.string.auth_invalid_login_param))
                            )
                        }
                    }
                    is BootstrapResult.Failure                 -> {
                        if (bootstrapResult is BootstrapResult.GenericError &&
                                bootstrapResult.failure is Failure.OtherServerError &&
                                bootstrapResult.failure.httpCode == 401) {
                            // Ignore this error
                        } else {
                            _viewEvents.post(BootstrapViewEvents.ModalError(bootstrapResult.error ?: stringProvider.getString(R.string.matrix_error)))
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
                                step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist),
                                // Also reset the passphrase
                                passphrase = null,
                                passphraseRepeat = null,
                                // Also reset the key
                                migrationRecoveryKey = null
                        )
                    }
                }
            }
            is BootstrapStep.SetupPassphrase                 -> {
                setState {
                    copy(
                            step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist),
                            // Also reset the passphrase
                            passphrase = null,
                            passphraseRepeat = null
                    )
                }
            }
            is BootstrapStep.ConfirmPassphrase               -> {
                setState {
                    copy(
                            step = BootstrapStep.SetupPassphrase
                    )
                }
            }
            is BootstrapStep.AccountReAuth                   -> {
                _viewEvents.post(BootstrapViewEvents.SkipBootstrap(state.passphrase != null))
            }
            BootstrapStep.Initializing                       -> {
                // do we let you cancel from here?
                _viewEvents.post(BootstrapViewEvents.SkipBootstrap(state.passphrase != null))
            }
            is BootstrapStep.SaveRecoveryKey,
            BootstrapStep.DoneSuccess                        -> {
                // nop
            }
            BootstrapStep.CheckingMigration                  -> Unit
            is BootstrapStep.FirstForm                       -> {
                _viewEvents.post(
                        when (state.setupMode) {
                            SetupMode.CROSS_SIGNING_ONLY,
                            SetupMode.NORMAL -> BootstrapViewEvents.SkipBootstrap()
                            else             -> BootstrapViewEvents.Dismiss(success = false)
                        }
                )
            }
            is BootstrapStep.GetBackupSecretForMigration     -> {
                setState {
                    copy(
                            step = BootstrapStep.FirstForm(keyBackUpExist = doesKeyBackupExist),
                            // Also reset the passphrase
                            passphrase = null,
                            passphraseRepeat = null,
                            // Also reset the key
                            migrationRecoveryKey = null
                    )
                }
            }
        }
    }

    private fun BackupToQuadSMigrationTask.Result.Failure.toHumanReadable(): String {
        return when (this) {
            is BackupToQuadSMigrationTask.Result.InvalidRecoverySecret -> stringProvider.getString(R.string.keys_backup_passphrase_error_decrypt)
            is BackupToQuadSMigrationTask.Result.ErrorFailure          -> errorFormatter.toHumanReadable(throwable)
            // is BackupToQuadSMigrationTask.Result.NoKeyBackupVersion,
            // is BackupToQuadSMigrationTask.Result.IllegalParams,
            else                                                       -> stringProvider.getString(R.string.unexpected_error)
        }
    }
}
