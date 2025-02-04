/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.threepids

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.ReadOnceTrue
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import org.matrix.android.sdk.flow.flow
import kotlin.coroutines.Continuation

class ThreePidsSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: ThreePidsSettingsViewState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val pendingAuthHandler: PendingAuthHandler,
) : VectorViewModel<ThreePidsSettingsViewState, ThreePidsSettingsAction, ThreePidsSettingsViewEvents>(initialState) {

    // UIA session
    private var pendingThreePid: ThreePid? = null

    private suspend fun loadingSuspendable(block: suspend () -> Unit) {
        runCatching { block() }
                .fold(
                        {
                            pendingThreePid = null
                            isLoading(false)
                        },
                        {
                            isLoading(false)
                            _viewEvents.post(ThreePidsSettingsViewEvents.Failure(it))
                        }
                )
    }

    private fun isLoading(isLoading: Boolean) {
        setState {
            copy(
                    isLoading = isLoading
            )
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ThreePidsSettingsViewModel, ThreePidsSettingsViewState> {
        override fun create(initialState: ThreePidsSettingsViewState): ThreePidsSettingsViewModel
    }

    companion object : MavericksViewModelFactory<ThreePidsSettingsViewModel, ThreePidsSettingsViewState> by hiltMavericksViewModelFactory()

    init {
        observeThreePids()
        observePendingThreePids()
    }

    private fun observeThreePids() {
        session.flow()
                .liveThreePIds(true)
                .execute {
                    copy(
                            threePids = it
                    )
                }
    }

    private fun observePendingThreePids() {
        session.flow()
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
            ThreePidsSettingsAction.SsoAuthDone -> pendingAuthHandler.ssoAuthDone()
            is ThreePidsSettingsAction.PasswordAuthDone -> pendingAuthHandler.passwordAuthDone(action.password)
            ThreePidsSettingsAction.ReAuthCancelled -> pendingAuthHandler.reAuthCancelled()
        }
    }

    private val uiaInterceptor = object : UserInteractiveAuthInterceptor {
        override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
            _viewEvents.post(ThreePidsSettingsViewEvents.RequestReAuth(flowResponse, errCode))
            pendingAuthHandler.pendingAuth = DefaultBaseAuth(session = flowResponse.session)
            pendingAuthHandler.uiaContinuation = promise
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
            try {
                session.profileService().submitSmsCode(action.threePid, action.code)
            } catch (failure: Throwable) {
                isLoading(false)
                setState {
                    copy(
                            msisdnValidationRequests = msisdnValidationRequests.toMutableMap().apply {
                                put(action.threePid.value, Fail(failure))
                            }
                    )
                }
                return@launch
            }

            // then finalize
            pendingThreePid = action.threePid
            loadingSuspendable { session.profileService().finalizeAddingThreePid(action.threePid, uiaInterceptor) }
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
                _viewEvents.post(
                        ThreePidsSettingsViewEvents.Failure(
                                IllegalArgumentException(
                                        stringProvider.getString(
                                                when (action.threePid) {
                                                    is ThreePid.Email -> CommonStrings.auth_email_already_defined
                                                    is ThreePid.Msisdn -> CommonStrings.auth_msisdn_already_defined
                                                }
                                        )
                                )
                        )
                )
            } else {
                viewModelScope.launch {
                    loadingSuspendable {
                        session.profileService().addThreePid(action.threePid)
                        // Also reset the state
                        setState {
                            copy(
                                    uiState = ThreePidsSettingsUiState.Idle
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleContinueThreePid(action: ThreePidsSettingsAction.ContinueThreePid) {
        isLoading(true)
        pendingThreePid = action.threePid
        viewModelScope.launch {
            loadingSuspendable { session.profileService().finalizeAddingThreePid(action.threePid, uiaInterceptor) }
        }
    }

    private fun handleCancelThreePid(action: ThreePidsSettingsAction.CancelThreePid) {
        isLoading(true)
        viewModelScope.launch {
            loadingSuspendable { session.profileService().cancelAddingThreePid(action.threePid) }
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
            loadingSuspendable { session.profileService().deleteThreePid(action.threePid) }
        }
    }
}
