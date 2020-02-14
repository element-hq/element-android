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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.securestorage.Curve25519AesSha2KeySpec
import im.vector.matrix.android.api.session.securestorage.KeyInfoResult
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.WaitingViewData
import im.vector.riotx.core.resources.StringProvider

data class SharedSecureStorageViewState(
        val requestedSecrets: List<String> = emptyList(),
        val passphraseVisible: Boolean = false
) : MvRxState

class SharedSecureStorageViewModel @AssistedInject constructor(
        @Assisted initialState: SharedSecureStorageViewState,
        private val stringProvider: StringProvider,
        private val session: Session)
    : VectorViewModel<SharedSecureStorageViewState, SharedSecureStorageAction, SharedSecureStorageViewEvent>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SharedSecureStorageViewState): SharedSecureStorageViewModel
    }

    override fun handle(action: SharedSecureStorageAction) = withState { state ->
        when (action) {
            is SharedSecureStorageAction.TogglePasswordVisibility -> {
                setState {
                    copy(
                            passphraseVisible = !passphraseVisible
                    )
                }
            }
            is SharedSecureStorageAction.Cancel                   -> {
                _viewEvents.post(SharedSecureStorageViewEvent.Dismiss)
            }
            is SharedSecureStorageAction.SubmitPassphrase         -> {
                _viewEvents.post(SharedSecureStorageViewEvent.ShowModalLoading)
                val passphrase = action.passphrase
                val keyInfoResult = session.sharedSecretStorageService.getDefaultKey()
                if (!keyInfoResult.isSuccess()) {
                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                    _viewEvents.post(SharedSecureStorageViewEvent.Error("Cannot find ssss key"))
                    return@withState
                }
                val keyInfo = (keyInfoResult as KeyInfoResult.Success).keyInfo

                // TODO
//                val decryptedSecretMap = HashMap<String, String>()
//                val errors = ArrayList<Throwable>()
                _viewEvents.post(SharedSecureStorageViewEvent.UpdateLoadingState(
                        WaitingViewData(
                                message = stringProvider.getString(R.string.keys_backup_restoring_computing_key_waiting_message),
                                isIndeterminate = true
                        )
                ))
                state.requestedSecrets.forEach {
                    val keySpec = Curve25519AesSha2KeySpec.fromPassphrase(
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
                    session.sharedSecretStorageService.getSecret(
                            name = it,
                            keyId = keyInfo.id,
                            secretKey = keySpec,
                            callback = object : MatrixCallback<String> {
                                override fun onSuccess(data: String) {
                                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                                }

                                override fun onFailure(failure: Throwable) {
                                    _viewEvents.post(SharedSecureStorageViewEvent.HideModalLoading)
                                    _viewEvents.post(SharedSecureStorageViewEvent.InlineError(failure.localizedMessage))
                                }
                            })
                }
            }
        }
    }

    companion object : MvRxViewModelFactory<SharedSecureStorageViewModel, SharedSecureStorageViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: SharedSecureStorageViewState): SharedSecureStorageViewModel? {
            val activity: SharedSecureStorageActivity = viewModelContext.activity()
            val args: SharedSecureStorageActivity.Args = activity.intent.getParcelableExtra(MvRx.KEY_ARG)
            return activity.viewModelFactory.create(
                    SharedSecureStorageViewState(
                            requestedSecrets = args.requestedSecrets
                    )
            )
        }
    }
}
