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

package im.vector.app.features.crypto.quads

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
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.securestorage.IntegrityResult
import org.matrix.android.sdk.api.session.securestorage.KeyInfoResult
import org.matrix.android.sdk.api.session.securestorage.RawBytesKeySpec
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.internal.crypto.crosssigning.toBase64NoPadding
import timber.log.Timber
import java.io.ByteArrayOutputStream

data class SharedSecureStorageViewState(
        val ready: Boolean = false,
        val hasPassphrase: Boolean = true,
        val checkingSSSSAction: Async<Unit> = Uninitialized,
        val step: Step = Step.EnterPassphrase,
        val activeDeviceCount: Int = 0,
        val showResetAllAction: Boolean = false,
        val userId: String = "",
        val keyId: String?,
        val requestedSecrets: List<String>,
        val resultKeyStoreAlias: String
) : MavericksState {

    constructor(args: SharedSecureStorageActivity.Args) : this(
            keyId = args.keyId,
            requestedSecrets = args.requestedSecrets,
            resultKeyStoreAlias = args.resultKeyStoreAlias
    )

    enum class Step {
        EnterPassphrase,
        EnterKey,
        ResetAll
    }
}

class SharedSecureStorageViewModel @AssistedInject constructor(
        @Assisted private val initialState: SharedSecureStorageViewState,
        private val stringProvider: StringProvider,
        private val session: Session) :
    VectorViewModel<SharedSecureStorageViewState, SharedSecureStorageAction, SharedSecureStorageViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SharedSecureStorageViewModel, SharedSecureStorageViewState> {
        override fun create(initialState: SharedSecureStorageViewState): SharedSecureStorageViewModel
    }

    init {
        setState {
            copy(userId = session.myUserId)
        }
        val integrityResult = session.sharedSecretStorageService.checkShouldBeAbleToAccessSecrets(initialState.requestedSecrets, initialState.keyId)
        if (integrityResult  !is IntegrityResult.Success) {
            _viewEvents.post(
                    SharedSecureStorageViewEvent.Error(
                            stringProvider.getString(R.string.enter_secret_storage_invalid),
                            true
                    )
            )
        }
        val keyResult = initialState.keyId?.let { session.sharedSecretStorageService.getKey(it) }
                ?: session.sharedSecretStorageService.getDefaultKey()

        if (!keyResult.isSuccess()) {
            _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
        } else {
            val info = (keyResult as KeyInfoResult.Success).keyInfo
            if (info.content.passphrase != null) {
                setState {
                    copy(
                            hasPassphrase = true,
                            ready = true,
                            step = SharedSecureStorageViewState.Step.EnterPassphrase
                    )
                }
            } else {
                setState {
                    copy(
                            hasPassphrase = false,
                            ready = true,
                            step = SharedSecureStorageViewState.Step.EnterKey
                    )
                }
            }
        }

        session.flow()
                .liveUserCryptoDevices(session.myUserId)
                .distinctUntilChanged()
                .execute {
                    copy(
                            activeDeviceCount = it.invoke()?.size ?: 0
                    )
                }
    }

    override fun handle(action: SharedSecureStorageAction) = withState {
        when (action) {
            is SharedSecureStorageAction.Cancel                   -> handleCancel()
            is SharedSecureStorageAction.SubmitPassphrase         -> handleSubmitPassphrase(action)
            SharedSecureStorageAction.UseKey                      -> handleUseKey()
            is SharedSecureStorageAction.SubmitKey                -> handleSubmitKey(action)
            SharedSecureStorageAction.Back                        -> handleBack()
            SharedSecureStorageAction.ForgotResetAll              -> handleResetAll()
            SharedSecureStorageAction.DoResetAll                  -> handleDoResetAll()
        }
    }

    private fun handleDoResetAll() {
        // as we are going to reset, we'd better cancel all outgoing requests
        // if not they could be accepted in the middle of the reset process
        // and cause strange use cases
        session.cryptoService().verificationService().getExistingVerificationRequests(session.myUserId).forEach {
            session.cryptoService().verificationService().cancelVerificationRequest(it)
        }
        _viewEvents.post(SharedSecureStorageViewEvent.ShowResetBottomSheet)
    }

    private fun handleResetAll() {
        setState {
            copy(
                    step = SharedSecureStorageViewState.Step.ResetAll
            )
        }
    }

    private fun handleUseKey() {
        setState {
            copy(
                    step = SharedSecureStorageViewState.Step.EnterKey
            )
        }
    }

    private fun handleBack() = withState { state ->
        if (state.checkingSSSSAction is Loading) return@withState // ignore
        when (state.step) {
            SharedSecureStorageViewState.Step.EnterKey -> {
                if (state.hasPassphrase) {
                    setState {
                        copy(
                                step = SharedSecureStorageViewState.Step.EnterPassphrase
                        )
                    }
                } else {
                    _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
                }
            }
            SharedSecureStorageViewState.Step.ResetAll -> {
                setState {
                    copy(
                            step = if (state.hasPassphrase) SharedSecureStorageViewState.Step.EnterPassphrase
                            else SharedSecureStorageViewState.Step.EnterKey
                    )
                }
            }
            else                                       -> {
                _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
            }
        }
    }

    private fun handleSubmitKey(action: SharedSecureStorageAction.SubmitKey) {
        _viewEvents.post(SharedSecureStorageViewEvent.ShowModalLoading)
        val decryptedSecretMap = HashMap<String, String>()
        setState { copy(checkingSSSSAction = Loading()) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val recoveryKey = action.recoveryKey
                val keyInfoResult = session.sharedSecretStorageService.getDefaultKey()
                if (!keyInfoResult.isSuccess()) {
                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                    _viewEvents.post(SharedSecureStorageViewEvent.Error(stringProvider.getString(R.string.failed_to_access_secure_storage)))
                    return@launch
                }
                val keyInfo = (keyInfoResult as KeyInfoResult.Success).keyInfo

                _viewEvents.post(SharedSecureStorageViewEvent.UpdateLoadingState(
                        WaitingViewData(
                                message = stringProvider.getString(R.string.keys_backup_restoring_computing_key_waiting_message),
                                isIndeterminate = true
                        )
                ))
                val keySpec = RawBytesKeySpec.fromRecoveryKey(recoveryKey) ?: return@launch Unit.also {
                    _viewEvents.post(SharedSecureStorageViewEvent.KeyInlineError(stringProvider.getString(R.string.bootstrap_invalid_recovery_key)))
                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                    setState { copy(checkingSSSSAction = Fail(IllegalArgumentException(stringProvider.getString(R.string.bootstrap_invalid_recovery_key)))) }
                }

                withContext(Dispatchers.IO) {
                    initialState.requestedSecrets.forEach {
                        if (session.accountDataService().getUserAccountDataEvent(it) != null) {
                            val res = session.sharedSecretStorageService.getSecret(
                                    name = it,
                                    keyId = keyInfo.id,
                                    secretKey = keySpec)
                            decryptedSecretMap[it] = res
                        } else {
                            Timber.w("## Cannot find secret $it in SSSS, skip")
                        }
                    }
                }
            }.fold({
                setState { copy(checkingSSSSAction = Success(Unit)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                val safeForIntentCypher = ByteArrayOutputStream().also {
                    it.use {
                        session.securelyStoreObject(decryptedSecretMap as Map<String, String>, initialState.resultKeyStoreAlias, it)
                    }
                }.toByteArray().toBase64NoPadding()
                _viewEvents.post(SharedSecureStorageViewEvent.FinishSuccess(safeForIntentCypher))
            }, {
                setState { copy(checkingSSSSAction = Fail(it)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                _viewEvents.post(SharedSecureStorageViewEvent.KeyInlineError(stringProvider.getString(R.string.keys_backup_passphrase_error_decrypt)))
            })
        }
    }

    private fun handleSubmitPassphrase(action: SharedSecureStorageAction.SubmitPassphrase) {
        _viewEvents.post(SharedSecureStorageViewEvent.ShowModalLoading)
        val decryptedSecretMap = HashMap<String, String>()
        setState { copy(checkingSSSSAction = Loading()) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val passphrase = action.passphrase
                val keyInfoResult = session.sharedSecretStorageService.getDefaultKey()
                if (!keyInfoResult.isSuccess()) {
                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                    _viewEvents.post(SharedSecureStorageViewEvent.Error("Cannot find ssss key"))
                    return@launch
                }
                val keyInfo = (keyInfoResult as KeyInfoResult.Success).keyInfo

                _viewEvents.post(SharedSecureStorageViewEvent.UpdateLoadingState(
                        WaitingViewData(
                                message = stringProvider.getString(R.string.keys_backup_restoring_computing_key_waiting_message),
                                isIndeterminate = true
                        )
                ))
                val keySpec = RawBytesKeySpec.fromPassphrase(
                        passphrase,
                        keyInfo.content.passphrase?.salt ?: "",
                        keyInfo.content.passphrase?.iterations ?: 0,
                        object : ProgressListener {
                            override fun onProgress(progress: Int, total: Int) {
                                _viewEvents.post(SharedSecureStorageViewEvent.UpdateLoadingState(
                                        WaitingViewData(
                                                message = stringProvider.getString(R.string.keys_backup_restoring_computing_key_waiting_message),
                                                isIndeterminate = false,
                                                progress = progress,
                                                progressTotal = total
                                        )
                                ))
                            }
                        }
                )

                withContext(Dispatchers.IO) {
                    initialState.requestedSecrets.forEach {
                        if (session.accountDataService().getUserAccountDataEvent(it) != null) {
                            val res = session.sharedSecretStorageService.getSecret(
                                    name = it,
                                    keyId = keyInfo.id,
                                    secretKey = keySpec)
                            decryptedSecretMap[it] = res
                        } else {
                            Timber.w("## Cannot find secret $it in SSSS, skip")
                        }
                    }
                }
            }.fold({
                setState { copy(checkingSSSSAction = Success(Unit)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                val safeForIntentCypher = ByteArrayOutputStream().also {
                    it.use {
                        session.securelyStoreObject(decryptedSecretMap as Map<String, String>, initialState.resultKeyStoreAlias, it)
                    }
                }.toByteArray().toBase64NoPadding()
                _viewEvents.post(SharedSecureStorageViewEvent.FinishSuccess(safeForIntentCypher))
            }, {
                setState { copy(checkingSSSSAction = Fail(it)) }
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                _viewEvents.post(SharedSecureStorageViewEvent.InlineError(stringProvider.getString(R.string.keys_backup_passphrase_error_decrypt)))
            })
        }
    }

    private fun handleCancel() {
        _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
    }

    companion object : MavericksViewModelFactory<SharedSecureStorageViewModel, SharedSecureStorageViewState> by hiltMavericksViewModelFactory()
}
