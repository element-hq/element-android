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

package im.vector.riotx.features.crypto.quads

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.securestorage.IntegrityResult
import im.vector.matrix.android.api.session.securestorage.KeyInfoResult
import im.vector.matrix.android.api.session.securestorage.RawBytesKeySpec
import im.vector.matrix.android.internal.crypto.crosssigning.toBase64NoPadding
import im.vector.matrix.android.internal.util.awaitCallback
import im.vector.riotx.R
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.WaitingViewData
import im.vector.riotx.core.resources.StringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream

data class SharedSecureStorageViewState(
        val ready: Boolean = false,
        val hasPassphrase: Boolean = true,
        val useKey: Boolean = false,
        val passphraseVisible: Boolean = false,
        val checkingSSSSAction: Async<Unit> = Uninitialized
) : MvRxState

class SharedSecureStorageViewModel @AssistedInject constructor(
        @Assisted initialState: SharedSecureStorageViewState,
        @Assisted val args: SharedSecureStorageActivity.Args,
        private val stringProvider: StringProvider,
        private val session: Session)
    : VectorViewModel<SharedSecureStorageViewState, SharedSecureStorageAction, SharedSecureStorageViewEvent>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SharedSecureStorageViewState, args: SharedSecureStorageActivity.Args): SharedSecureStorageViewModel
    }

    init {
        val isValid = session.sharedSecretStorageService.checkShouldBeAbleToAccessSecrets(args.requestedSecrets, args.keyId) is IntegrityResult.Success
        if (!isValid) {
            _viewEvents.post(
                    SharedSecureStorageViewEvent.Error(
                            stringProvider.getString(R.string.enter_secret_storage_invalid),
                            true
                    )
            )
        }
        val keyResult = args.keyId?.let { session.sharedSecretStorageService.getKey(it) }
                ?: session.sharedSecretStorageService.getDefaultKey()

        if (!keyResult.isSuccess()) {
            _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
        } else {
            val info = (keyResult as KeyInfoResult.Success).keyInfo
            if (info.content.passphrase != null) {
                setState {
                    copy(
                            ready = true,
                            hasPassphrase = true,
                            useKey = false
                    )
                }
            } else {
                setState {
                    copy(
                            ready = true,
                            hasPassphrase = false
                    )
                }
            }
        }
    }

    override fun handle(action: SharedSecureStorageAction) = withState {
        when (action) {
            is SharedSecureStorageAction.TogglePasswordVisibility -> handleTogglePasswordVisibility()
            is SharedSecureStorageAction.Cancel                   -> handleCancel()
            is SharedSecureStorageAction.SubmitPassphrase         -> handleSubmitPassphrase(action)
            SharedSecureStorageAction.UseKey                      -> handleUseKey()
            is SharedSecureStorageAction.SubmitKey                -> handleSubmitKey(action)
            SharedSecureStorageAction.Back                        -> handleBack()
        }.exhaustive
    }

    private fun handleUseKey() {
        setState {
            copy(
                    useKey = true
            )
        }
    }

    private fun handleBack() = withState { state ->
        if (state.checkingSSSSAction is Loading) return@withState // ignore
        if (state.hasPassphrase && state.useKey) {
            setState {
                copy(
                        useKey = false
                )
            }
        } else {
            _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
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
                }

                withContext(Dispatchers.IO) {
                    args.requestedSecrets.forEach {
                        if (session.getAccountDataEvent(it) != null) {
                            val res = awaitCallback<String> { callback ->
                                session.sharedSecretStorageService.getSecret(
                                        name = it,
                                        keyId = keyInfo.id,
                                        secretKey = keySpec,
                                        callback = callback)
                            }
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
                        session.securelyStoreObject(decryptedSecretMap as Map<String, String>, args.resultKeyStoreAlias, it)
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
                    args.requestedSecrets.forEach {
                        if (session.getAccountDataEvent(it) != null) {
                            val res = awaitCallback<String> { callback ->
                                session.sharedSecretStorageService.getSecret(
                                        name = it,
                                        keyId = keyInfo.id,
                                        secretKey = keySpec,
                                        callback = callback)
                            }
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
                        session.securelyStoreObject(decryptedSecretMap as Map<String, String>, args.resultKeyStoreAlias, it)
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

    private fun handleTogglePasswordVisibility() {
        setState {
            copy(
                    passphraseVisible = !passphraseVisible
            )
        }
    }

    companion object : MvRxViewModelFactory<SharedSecureStorageViewModel, SharedSecureStorageViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: SharedSecureStorageViewState): SharedSecureStorageViewModel? {
            val activity: SharedSecureStorageActivity = viewModelContext.activity()
            val args: SharedSecureStorageActivity.Args = activity.intent.getParcelableExtra(MvRx.KEY_ARG) ?: error("Missing args")
            return activity.viewModelFactory.create(state, args)
        }
    }
}
