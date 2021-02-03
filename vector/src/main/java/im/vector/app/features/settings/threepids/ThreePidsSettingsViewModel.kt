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

package im.vector.app.features.settings.threepids

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.ReadOnceTrue
import im.vector.app.features.auth.ReAuthActivity
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.model.rest.DefaultBaseAuth
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ThreePidsSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: ThreePidsSettingsViewState,
        private val session: Session,
        private val stringProvider: StringProvider
) : VectorViewModel<ThreePidsSettingsViewState, ThreePidsSettingsAction, ThreePidsSettingsViewEvents>(initialState) {

    // UIA session
    private var pendingThreePid: ThreePid? = null
//    private var pendingSession: String? = null

    private val loadingCallback: MatrixCallback<Unit> = object : MatrixCallback<Unit> {
        override fun onFailure(failure: Throwable) {
            isLoading(false)
            _viewEvents.post(ThreePidsSettingsViewEvents.Failure(failure))
        }

        override fun onSuccess(data: Unit) {
            pendingThreePid = null
            isLoading(false)
        }
    }

    private fun isLoading(isLoading: Boolean) {
        setState {
            copy(
                    isLoading = isLoading
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: ThreePidsSettingsViewState): ThreePidsSettingsViewModel
    }

    companion object : MvRxViewModelFactory<ThreePidsSettingsViewModel, ThreePidsSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ThreePidsSettingsViewState): ThreePidsSettingsViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        observeThreePids()
        observePendingThreePids()
    }

    private fun observeThreePids() {
        session.rx()
                .liveThreePIds(true)
                .execute {
                    copy(
                            threePids = it
                    )
                }
    }

    private fun observePendingThreePids() {
        session.rx()
                .livePendingThreePIds()
                .execute {
                    copy(
                            pendingThreePids = it,
                            // Ensure the editText for code will be reset
                            msisdnValidationReinitiator = msisdnValidationReinitiator.toMutableMap().apply {
                                it.invoke()
                                        ?.filterIsInstance(ThreePid.Msisdn::class.java)
                                        ?.forEach { threePid ->
                                            getOrPut(threePid) { ReadOnceTrue() }
                                        }
                            }
                    )
                }
    }

    override fun handle(action: ThreePidsSettingsAction) {
        when (action) {
            is ThreePidsSettingsAction.AddThreePid -> handleAddThreePid(action)
            is ThreePidsSettingsAction.ContinueThreePid -> handleContinueThreePid(action)
            is ThreePidsSettingsAction.SubmitCode -> handleSubmitCode(action)
            is ThreePidsSettingsAction.CancelThreePid -> handleCancelThreePid(action)
            is ThreePidsSettingsAction.DeleteThreePid -> handleDeleteThreePid(action)
            is ThreePidsSettingsAction.ChangeUiState -> handleChangeUiState(action)
            ThreePidsSettingsAction.SsoAuthDone -> {
                Timber.d("## UIA - FallBack success")
                if (pendingAuth != null) {
                    uiaContinuation?.resume(pendingAuth!!)
                } else {
                    uiaContinuation?.resumeWith(Result.failure((IllegalArgumentException())))
                }
            }
            is ThreePidsSettingsAction.PasswordAuthDone -> {
                val decryptedPass = session.loadSecureSecret<String>(action.password.fromBase64().inputStream(), ReAuthActivity.DEFAULT_RESULT_KEYSTORE_ALIAS)
                uiaContinuation?.resume(
                        UserPasswordAuth(
                                session = pendingAuth?.session,
                                password = decryptedPass,
                                user = session.myUserId
                        )
                )
            }
            ThreePidsSettingsAction.ReAuthCancelled -> {
                Timber.d("## UIA - Reauth cancelled")
                uiaContinuation?.resumeWith(Result.failure((Exception())))
                uiaContinuation = null
                pendingAuth = null
            }
        }.exhaustive
    }

    var uiaContinuation: Continuation<UIABaseAuth>? = null
    var pendingAuth: UIABaseAuth? = null

    private val uiaInterceptor = object : UserInteractiveAuthInterceptor {
        override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
            _viewEvents.post(ThreePidsSettingsViewEvents.RequestReAuth(flowResponse, errCode))
            pendingAuth = DefaultBaseAuth(session = flowResponse.session)
            uiaContinuation = promise
        }
    }

    private fun handleSubmitCode(action: ThreePidsSettingsAction.SubmitCode) {
        isLoading(true)
        setState {
            copy(
                    msisdnValidationRequests = msisdnValidationRequests.toMutableMap().apply {
                        put(action.threePid.value, Loading())
                    }
            )
        }

        viewModelScope.launch {
            // First submit the code
            session.submitSmsCode(action.threePid, action.code, object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    // then finalize
                    pendingThreePid = action.threePid
                    session.finalizeAddingThreePid(action.threePid, uiaInterceptor, loadingCallback)
                }

                override fun onFailure(failure: Throwable) {
                    isLoading(false)
                    setState {
                        copy(
                                msisdnValidationRequests = msisdnValidationRequests.toMutableMap().apply {
                                    put(action.threePid.value, Fail(failure))
                                }
                        )
                    }
                }
            })
        }
    }

    private fun handleChangeUiState(action: ThreePidsSettingsAction.ChangeUiState) {
        setState {
            copy(
                    uiState = action.newUiState,
                    editTextReinitiator = ReadOnceTrue()
            )
        }
    }

    private fun handleAddThreePid(action: ThreePidsSettingsAction.AddThreePid) {
        isLoading(true)

        withState { state ->
            val allThreePids = state.threePids.invoke().orEmpty() + state.pendingThreePids.invoke().orEmpty()
            if (allThreePids.any { it.value == action.threePid.value }) {
                _viewEvents.post(ThreePidsSettingsViewEvents.Failure(IllegalArgumentException(stringProvider.getString(
                        when (action.threePid) {
                            is ThreePid.Email  -> R.string.auth_email_already_defined
                            is ThreePid.Msisdn -> R.string.auth_msisdn_already_defined
                        }
                ))))
            } else {
                viewModelScope.launch {
                    session.addThreePid(action.threePid, object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            // Also reset the state
                            setState {
                                copy(
                                        uiState = ThreePidsSettingsUiState.Idle
                                )
                            }
                            loadingCallback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            loadingCallback.onFailure(failure)
                        }
                    })
                }
            }
        }
    }

    private fun handleContinueThreePid(action: ThreePidsSettingsAction.ContinueThreePid) {
        isLoading(true)
        pendingThreePid = action.threePid
        viewModelScope.launch {
            session.finalizeAddingThreePid(action.threePid, uiaInterceptor, loadingCallback)
        }
    }

    private fun handleCancelThreePid(action: ThreePidsSettingsAction.CancelThreePid) {
        isLoading(true)
        viewModelScope.launch {
            session.cancelAddingThreePid(action.threePid, loadingCallback)
        }
    }

//    private fun handleAccountPassword(action: ThreePidsSettingsAction.AccountPassword) {
//        val safeThreePid = pendingThreePid ?: return Unit
//                .also { _viewEvents.post(ThreePidsSettingsViewEvents.Failure(IllegalStateException("No pending threePid"))) }
//        isLoading(true)
//        viewModelScope.launch {
//            session.finalizeAddingThreePid(safeThreePid, uiaInterceptor, loadingCallback)
//        }
//    }

    private fun handleDeleteThreePid(action: ThreePidsSettingsAction.DeleteThreePid) {
        isLoading(true)
        viewModelScope.launch {
            session.deleteThreePid(action.threePid, loadingCallback)
        }
    }
}
