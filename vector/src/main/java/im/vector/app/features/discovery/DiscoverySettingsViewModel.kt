/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.discovery

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
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
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.IdentityServiceListener
import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.flow.flow

class DiscoverySettingsViewModel @AssistedInject constructor(
        @Assisted initialState: DiscoverySettingsState,
        private val session: Session,
        private val stringProvider: StringProvider
) : VectorViewModel<DiscoverySettingsState, DiscoverySettingsAction, DiscoverySettingsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DiscoverySettingsViewModel, DiscoverySettingsState> {
        override fun create(initialState: DiscoverySettingsState): DiscoverySettingsViewModel
    }

    companion object : MavericksViewModelFactory<DiscoverySettingsViewModel, DiscoverySettingsState> by hiltMavericksViewModelFactory()

    private val identityService = session.identityService()

    private val identityServerManagerListener = object : IdentityServiceListener {
        override fun onIdentityServerChange() = withState { state ->
            viewModelScope.launch {
                runCatching { fetchIdentityServerWithTerms() }.fold(
                        onSuccess = {
                            val currentIS = state.identityServer()
                            setState {
                                copy(
                                        identityServer = Success(it),
                                        userConsent = identityService.getUserConsent()
                                )
                            }
                            if (currentIS != it) retrieveBinding()
                        },
                        onFailure = { _viewEvents.post(DiscoverySettingsViewEvents.Failure(it)) }
                )
            }
        }
    }

    init {
        setState {
            copy(
                    identityServer = Success(identityService.getCurrentIdentityServerUrl()?.let { ServerAndPolicies(it, emptyList()) }),
                    userConsent = identityService.getUserConsent()
            )
        }
        startListenToIdentityManager()
        observeThreePids()
    }

    private fun observeThreePids() {
        session.flow()
                .liveThreePIds(true)
                .onEach {
                    retrieveBinding(it)
                }
                .launchIn(viewModelScope)
    }

    override fun onCleared() {
        stopListenToIdentityManager()
        super.onCleared()
    }

    override fun handle(action: DiscoverySettingsAction) {
        when (action) {
            DiscoverySettingsAction.Refresh -> fetchContent()
            DiscoverySettingsAction.RetrieveBinding -> retrieveBinding()
            DiscoverySettingsAction.DisconnectIdentityServer -> disconnectIdentityServer()
            is DiscoverySettingsAction.SetPoliciesExpandState -> updatePolicyUrlsExpandedState(action.expanded)
            is DiscoverySettingsAction.ChangeIdentityServer -> changeIdentityServer(action)
            is DiscoverySettingsAction.UpdateUserConsent -> handleUpdateUserConsent(action)
            is DiscoverySettingsAction.RevokeThreePid -> revokeThreePid(action)
            is DiscoverySettingsAction.ShareThreePid -> shareThreePid(action)
            is DiscoverySettingsAction.FinalizeBind3pid -> finalizeBind3pid(action, true)
            is DiscoverySettingsAction.SubmitMsisdnToken -> submitMsisdnToken(action)
            is DiscoverySettingsAction.CancelBinding -> cancelBinding(action)
        }
    }

    private fun handleUpdateUserConsent(action: DiscoverySettingsAction.UpdateUserConsent) {
        identityService.setUserConsent(action.newConsent)
        setState { copy(userConsent = action.newConsent) }
    }

    private fun disconnectIdentityServer() {
        setState { copy(identityServer = Loading()) }

        viewModelScope.launch {
            try {
                session.identityService().disconnect()
                setState {
                    copy(
                            identityServer = Success(null),
                            userConsent = false
                    )
                }
            } catch (failure: Throwable) {
                setState { copy(identityServer = Fail(failure)) }
            }
        }
    }

    private fun updatePolicyUrlsExpandedState(isExpanded: Boolean) {
        setState { copy(isIdentityPolicyUrlsExpanded = isExpanded) }
    }

    private fun changeIdentityServer(action: DiscoverySettingsAction.ChangeIdentityServer) {
        setState { copy(identityServer = Loading()) }

        viewModelScope.launch {
            try {
                val data = session.identityService().setNewIdentityServer(action.url)
                setState {
                    copy(
                            identityServer = Success(ServerAndPolicies(data, emptyList())),
                            userConsent = false
                    )
                }
                retrieveBinding()
            } catch (failure: Throwable) {
                setState { copy(identityServer = Fail(failure)) }
            }
        }
    }

    private fun shareThreePid(action: DiscoverySettingsAction.ShareThreePid) = withState { state ->
        if (state.identityServer() == null) return@withState
        changeThreePidState(action.threePid, Loading())

        viewModelScope.launch {
            try {
                identityService.startBindThreePid(action.threePid)
                changeThreePidState(action.threePid, Success(SharedState.BINDING_IN_PROGRESS))
            } catch (failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                changeThreePidState(action.threePid, Fail(failure))
            }
        }
    }

    private fun changeThreePidState(threePid: ThreePid, state: Async<SharedState>) {
        setState {
            val currentMails = emailList().orEmpty()
            val phones = phoneNumbersList().orEmpty()
            copy(
                    emailList = Success(
                            currentMails.map {
                                if (it.threePid == threePid) {
                                    it.copy(isShared = state)
                                } else {
                                    it
                                }
                            }
                    ),
                    phoneNumbersList = Success(
                            phones.map {
                                if (it.threePid == threePid) {
                                    it.copy(isShared = state)
                                } else {
                                    it
                                }
                            }
                    )
            )
        }
    }

    private fun changeThreePidSubmitState(threePid: ThreePid, submitState: Async<Unit>) {
        setState {
            val currentMails = emailList().orEmpty()
            val phones = phoneNumbersList().orEmpty()
            copy(
                    emailList = Success(
                            currentMails.map {
                                if (it.threePid == threePid) {
                                    it.copy(finalRequest = submitState)
                                } else {
                                    it
                                }
                            }
                    ),
                    phoneNumbersList = Success(
                            phones.map {
                                if (it.threePid == threePid) {
                                    it.copy(finalRequest = submitState)
                                } else {
                                    it
                                }
                            }
                    )
            )
        }
    }

    private fun revokeThreePid(action: DiscoverySettingsAction.RevokeThreePid) {
        when (action.threePid) {
            is ThreePid.Email -> revokeEmail(action.threePid)
            is ThreePid.Msisdn -> revokeMsisdn(action.threePid)
        }
    }

    private fun revokeEmail(threePid: ThreePid.Email) = withState { state ->
        if (state.identityServer() == null) return@withState
        if (state.emailList() == null) return@withState
        changeThreePidState(threePid, Loading())

        viewModelScope.launch {
            try {
                identityService.unbindThreePid(threePid)
                changeThreePidState(threePid, Success(SharedState.NOT_SHARED))
            } catch (failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                changeThreePidState(threePid, Fail(failure))
            }
        }
    }

    private fun revokeMsisdn(threePid: ThreePid.Msisdn) = withState { state ->
        if (state.identityServer() == null) return@withState
        if (state.phoneNumbersList() == null) return@withState
        changeThreePidState(threePid, Loading())

        viewModelScope.launch {
            try {
                identityService.unbindThreePid(threePid)
                changeThreePidState(threePid, Success(SharedState.NOT_SHARED))
            } catch (failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                changeThreePidState(threePid, Fail(failure))
            }
        }
    }

    private fun cancelBinding(action: DiscoverySettingsAction.CancelBinding) {
        viewModelScope.launch {
            try {
                identityService.cancelBindThreePid(action.threePid)
                changeThreePidState(action.threePid, Success(SharedState.NOT_SHARED))
                changeThreePidSubmitState(action.threePid, Uninitialized)
            } catch (failure: Throwable) {
                // This could never fail
            }
        }
    }

    private fun startListenToIdentityManager() {
        identityService.addListener(identityServerManagerListener)
    }

    private fun stopListenToIdentityManager() {
        identityService.addListener(identityServerManagerListener)
    }

    private fun retrieveBinding() {
        retrieveBinding(session.profileService().getThreePids())
    }

    private fun retrieveBinding(threePids: List<ThreePid>) = withState { state ->
        if (state.identityServer()?.serverUrl.isNullOrBlank()) return@withState

        val emails = threePids.filterIsInstance<ThreePid.Email>()
        val msisdns = threePids.filterIsInstance<ThreePid.Msisdn>()

        setState {
            copy(
                    emailList = Success(emails.map { PidInfo(it, Loading()) }),
                    phoneNumbersList = Success(msisdns.map { PidInfo(it, Loading()) })
            )
        }

        viewModelScope.launch {
            try {
                val data = identityService.getShareStatus(threePids)
                setState {
                    copy(
                            emailList = Success(data.filter { it.key is ThreePid.Email }.toPidInfoList()),
                            phoneNumbersList = Success(data.filter { it.key is ThreePid.Msisdn }.toPidInfoList()),
                            termsNotSigned = false
                    )
                }
            } catch (failure: Throwable) {
                if (failure !is IdentityServiceError.TermsNotSignedException) {
                    _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                }

                setState {
                    copy(
                            emailList = Success(emails.map { PidInfo(it, Fail(failure)) }),
                            phoneNumbersList = Success(msisdns.map { PidInfo(it, Fail(failure)) }),
                            termsNotSigned = failure is IdentityServiceError.TermsNotSignedException
                    )
                }
            }
        }
    }

    private fun Map<ThreePid, SharedState>.toPidInfoList(): List<PidInfo> {
        return map { threePidStatus ->
            PidInfo(
                    threePid = threePidStatus.key,
                    isShared = Success(threePidStatus.value)
            )
        }
    }

    private fun submitMsisdnToken(action: DiscoverySettingsAction.SubmitMsisdnToken) = withState { state ->
        if (state.identityServer()?.serverUrl.isNullOrBlank()) return@withState

        changeThreePidSubmitState(action.threePid, Loading())

        viewModelScope.launch {
            try {
                identityService.submitValidationToken(action.threePid, action.code)
                changeThreePidSubmitState(action.threePid, Uninitialized)
                finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(action.threePid), true)
            } catch (failure: Throwable) {
                changeThreePidSubmitState(action.threePid, Fail(failure))
            }
        }
    }

    private fun finalizeBind3pid(action: DiscoverySettingsAction.FinalizeBind3pid, fromUser: Boolean) = withState { state ->
        val threePid = when (action.threePid) {
            is ThreePid.Email -> {
                state.emailList()?.find { it.threePid.value == action.threePid.email }?.threePid ?: return@withState
            }
            is ThreePid.Msisdn -> {
                state.phoneNumbersList()?.find { it.threePid.value == action.threePid.msisdn }?.threePid ?: return@withState
            }
        }

        changeThreePidSubmitState(action.threePid, Loading())

        viewModelScope.launch {
            try {
                identityService.finalizeBindThreePid(threePid)
                changeThreePidSubmitState(action.threePid, Uninitialized)
                changeThreePidState(action.threePid, Success(SharedState.SHARED))
            } catch (failure: Throwable) {
                // If this is not from user (user did not click to "Continue", but this is a refresh when Fragment is resumed), do no display the error
                if (fromUser) {
                    changeThreePidSubmitState(action.threePid, Fail(failure))
                } else {
                    changeThreePidSubmitState(action.threePid, Uninitialized)
                }
            }
        }
    }

    private fun fetchContent() = withState { state ->
        state.emailList()?.forEach { info ->
            when (info.isShared()) {
                SharedState.BINDING_IN_PROGRESS -> finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(info.threePid), false)
                else -> Unit
            }
        }
        viewModelScope.launch {
            runCatching { session.fetchIdentityServerWithTerms(stringProvider.getString(CommonStrings.resources_language)) }.fold(
                    onSuccess = { setState { copy(identityServer = Success(it)) } },
                    onFailure = { _viewEvents.post(DiscoverySettingsViewEvents.Failure(it)) }
            )
        }
    }

    private suspend fun fetchIdentityServerWithTerms(): ServerAndPolicies? {
        return session.fetchIdentityServerWithTerms(stringProvider.getString(CommonStrings.resources_language))
    }
}
