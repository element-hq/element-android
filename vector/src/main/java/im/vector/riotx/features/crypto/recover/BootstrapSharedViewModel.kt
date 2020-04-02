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

package im.vector.riotx.features.crypto.recover

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.nulabinc.zxcvbn.Strength
import com.nulabinc.zxcvbn.Zxcvbn
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.securestorage.SsssKeyCreationInfo
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.riotx.R
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.WaitingViewData
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.login.ReAuthHelper
import kotlinx.coroutines.launch
import java.io.OutputStream

data class BootstrapViewState(
        val step: BootstrapStep = BootstrapStep.SetupPassphrase(false),
        val passphrase: String? = null,
        val passphraseRepeat: String? = null,
        val crossSigningInitialization: Async<Unit> = Uninitialized,
        val passphraseStrength: Async<Strength> = Uninitialized,
        val passphraseConfirmMatch: Async<Unit> = Uninitialized,
        val recoveryKeyCreationInfo: SsssKeyCreationInfo? = null,
        val initializationWaitingViewData: WaitingViewData? = null,
        val currentReAuth: UserPasswordAuth? = null,
        val recoverySaveFileProcess: Async<Unit> = Uninitialized
) : MvRxState

sealed class BootstrapStep {
    data class SetupPassphrase(val isPasswordVisible: Boolean) : BootstrapStep()
    data class ConfirmPassphrase(val isPasswordVisible: Boolean) : BootstrapStep()
    data class AccountPassword(val isPasswordVisible: Boolean, val failure: String? = null) : BootstrapStep()
    object Initializing : BootstrapStep()
    data class SaveRecoveryKey(val isSaved: Boolean) : BootstrapStep()
    object DoneSuccess : BootstrapStep()
}

class BootstrapSharedViewModel @AssistedInject constructor(
        @Assisted initialState: BootstrapViewState,
        private val stringProvider: StringProvider,
        private val session: Session,
        private val bootstrapTask: BootstrapCrossSigningTask,
        private val reAuthHelper: ReAuthHelper
) : VectorViewModel<BootstrapViewState, BootstrapActions, BootstrapViewEvents>(initialState) {

    private val zxcvbn = Zxcvbn()

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: BootstrapViewState): BootstrapSharedViewModel
    }

    override fun handle(action: BootstrapActions) = withState { state ->
        when (action) {
            is BootstrapActions.GoBack                           -> queryBack()
            BootstrapActions.TogglePasswordVisibility            -> {
                when (state.step) {
                    is BootstrapStep.SetupPassphrase   -> {
                        setState {
                            copy(step = state.step.copy(isPasswordVisible = !state.step.isPasswordVisible))
                        }
                    }
                    is BootstrapStep.ConfirmPassphrase -> {
                        setState {
                            copy(step = state.step.copy(isPasswordVisible = !state.step.isPasswordVisible))
                        }
                    }

                    is BootstrapStep.AccountPassword   -> {
                        setState {
                            copy(step = state.step.copy(isPasswordVisible = !state.step.isPasswordVisible))
                        }
                    }
                    else                               -> {
                    }
                }
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
                            step = BootstrapStep.ConfirmPassphrase(
                                    isPasswordVisible = (state.step as? BootstrapStep.SetupPassphrase)?.isPasswordVisible ?: false
                            )
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
                    val auth = action.auth ?: reAuthHelper.rememberedAuth()
                    if (auth == null) {
                        setState {
                            copy(
                                    step = BootstrapStep.AccountPassword(false)
                            )
                        }
                    } else {
                        startInitializeFlow(action.auth)
                    }
                } else {
                    setState {
                        copy(
                                passphraseConfirmMatch = Fail(Throwable(stringProvider.getString(R.string.passphrase_passphrase_does_not_match)))
                        )
                    }
                }
            }
            BootstrapActions.RecoveryKeySaved                    -> {
                _viewEvents.post(BootstrapViewEvents.RecoveryKeySaved)
                setState {
                    copy(step = BootstrapStep.SaveRecoveryKey(true))
                }
            }
            BootstrapActions.Completed                           -> {
                _viewEvents.post(BootstrapViewEvents.Dismiss)
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
                    copy(step = BootstrapStep.AccountPassword(false))
                }
            }
            is BootstrapActions.ReAuth                           -> {
                startInitializeFlow(
                        state.currentReAuth?.copy(password = action.pass)
                                ?: UserPasswordAuth(user = session.myUserId, password = action.pass)
                )
            }
        }.exhaustive
    }

    // =======================================
    // Business Logic
    // =======================================
    private fun saveRecoveryKeyToUri(os: OutputStream) = withState { state ->
        viewModelScope.launch {
            kotlin.runCatching {
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

    private fun startInitializeFlow(auth: UserPasswordAuth?) {
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

        withState { state ->
            viewModelScope.launch {
                bootstrapTask.invoke(this, Params(
                        userPasswordAuth = auth ?: reAuthHelper.rememberedAuth(),
                        progressListener = progressListener,
                        passphrase = state.passphrase!!
                )) {
                    when (it) {
                        is BootstrapResult.Success                 -> {
                            setState {
                                copy(
                                        recoveryKeyCreationInfo = it.keyInfo,
                                        step = BootstrapStep.SaveRecoveryKey(false)
                                )
                            }
                        }
                        is BootstrapResult.PasswordAuthFlowMissing -> {
                            setState {
                                copy(
                                        currentReAuth = UserPasswordAuth(session = it.sessionId, user = it.userId),
                                        step = BootstrapStep.AccountPassword(false)
                                )
                            }
                        }
                        is BootstrapResult.UnsupportedAuthFlow     -> {
                            _viewEvents.post(BootstrapViewEvents.ModalError(stringProvider.getString(R.string.auth_flow_not_supported)))
                            _viewEvents.post(BootstrapViewEvents.Dismiss)
                        }
                        is BootstrapResult.InvalidPasswordError    -> {
                            // it's a bad password
                            setState {
                                copy(
                                        // We clear the auth session, to avoid 'Requested operation has changed during the UI authentication session' error
                                        currentReAuth = UserPasswordAuth(session = null, user = session.myUserId),
                                        step = BootstrapStep.AccountPassword(false, stringProvider.getString(R.string.auth_invalid_login_param))
                                )
                            }
                        }
                        is BootstrapResult.Failure                 -> {
                            if (it is BootstrapResult.GenericError
                                    && it.failure is im.vector.matrix.android.api.failure.Failure.OtherServerError
                                    && it.failure.httpCode == 401) {
                            } else {
                                _viewEvents.post(BootstrapViewEvents.ModalError(it.error ?: stringProvider.getString(R.string.matrix_error)))
                                setState {
                                    copy(
                                            step = BootstrapStep.ConfirmPassphrase(false)
                                    )
                                }
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
            is BootstrapStep.SetupPassphrase   -> {
                // do we let you cancel from here?
                _viewEvents.post(BootstrapViewEvents.SkipBootstrap)
            }
            is BootstrapStep.ConfirmPassphrase -> {
                setState {
                    copy(
                            step = BootstrapStep.SetupPassphrase(
                                    isPasswordVisible = (state.step as? BootstrapStep.ConfirmPassphrase)?.isPasswordVisible ?: false
                            )
                    )
                }
            }
            is BootstrapStep.AccountPassword   -> {
                _viewEvents.post(BootstrapViewEvents.SkipBootstrap)
            }
            BootstrapStep.Initializing         -> {
                // do we let you cancel from here?
                _viewEvents.post(BootstrapViewEvents.SkipBootstrap)
            }
            is BootstrapStep.SaveRecoveryKey,
            BootstrapStep.DoneSuccess          -> {
                // nop
            }
        }
    }

    // ======================================
    // Companion, view model assisted creation
    // ======================================

    companion object : MvRxViewModelFactory<BootstrapSharedViewModel, BootstrapViewState> {

        override fun create(viewModelContext: ViewModelContext, state: BootstrapViewState): BootstrapSharedViewModel? {
            val fragment: BootstrapBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.bootstrapViewModelFactory.create(state)
        }
    }
}
