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

import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
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
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.WaitingViewData
import im.vector.riotx.core.resources.StringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class SharedSecureStorageViewState(
        val passphraseVisible: Boolean = false
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
    }

    override fun handle(action: SharedSecureStorageAction) = withState {
        when (action) {
            is SharedSecureStorageAction.TogglePasswordVisibility -> handleTogglePasswordVisibility()
            is SharedSecureStorageAction.Cancel                   -> handleCancel()
            is SharedSecureStorageAction.SubmitPassphrase         -> handleSubmitPassphrase(action)
        }
    }

    private fun handleSubmitPassphrase(action: SharedSecureStorageAction.SubmitPassphrase) {
        val decryptedSecretMap = HashMap<String, String>()
        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                _viewEvents.post(SharedSecureStorageViewEvent.ShowModalLoading)
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
                        // TODO
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
                        val res = awaitCallback<String> { callback ->
                            session.sharedSecretStorageService.getSecret(
                                    name = it,
                                    keyId = keyInfo.id,
                                    secretKey = keySpec,
                                    callback = callback)
                        }
                        decryptedSecretMap[it] = res
                    }
                }
            }.fold({
                _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                val safeForIntentCypher = ByteArrayOutputStream().also {
                    it.use {
                        session.securelyStoreObject(decryptedSecretMap as Map<String, String>, args.resultKeyStoreAlias, it)
                    }
                }.toByteArray().toBase64NoPadding()
                _viewEvents.post(SharedSecureStorageViewEvent.FinishSuccess(safeForIntentCypher))
            }, {
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
            val args: SharedSecureStorageActivity.Args? = activity.intent.getParcelableExtra(MvRx.KEY_ARG)
            return args?.let { activity.viewModelFactory.create(state, it) }
        }
    }
}
