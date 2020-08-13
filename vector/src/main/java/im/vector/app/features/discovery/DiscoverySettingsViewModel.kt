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
package im.vector.app.features.discovery

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.IdentityServiceListener
import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.util.awaitCallback
import kotlinx.coroutines.launch
import org.matrix.android.sdk.rx.rx

class DiscoverySettingsViewModel @AssistedInject constructor(
        @Assisted initialState: DiscoverySettingsState,
        private val session: Session)
    : VectorViewModel<DiscoverySettingsState, DiscoverySettingsAction, DiscoverySettingsViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DiscoverySettingsState): DiscoverySettingsViewModel
    }

    companion object : MvRxViewModelFactory<DiscoverySettingsViewModel, DiscoverySettingsState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DiscoverySettingsState): DiscoverySettingsViewModel? {
            val fragment: DiscoverySettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val identityService = session.identityService()

    private val identityServerManagerListener = object : IdentityServiceListener {
        override fun onIdentityServerChange() = withState { state ->
            val identityServerUrl = identityService.getCurrentIdentityServerUrl()
            val currentIS = state.identityServer()
            setState {
                copy(identityServer = Success(identityServerUrl))
            }
            if (currentIS != identityServerUrl) retrieveBinding()
        }
    }

    init {
        setState {
            copy(identityServer = Success(identityService.getCurrentIdentityServerUrl()))
        }
        startListenToIdentityManager()
        observeThreePids()
    }

    private fun observeThreePids() {
        session.rx()
                .liveThreePIds(true)
                .subscribe {
                    retrieveBinding(it)
                }
                .disposeOnClear()
    }

    override fun onCleared() {
        super.onCleared()
        stopListenToIdentityManager()
    }

    override fun handle(action: DiscoverySettingsAction) {
        when (action) {
            DiscoverySettingsAction.Refresh                  -> refreshPendingEmailBindings()
            DiscoverySettingsAction.RetrieveBinding          -> retrieveBinding()
            DiscoverySettingsAction.DisconnectIdentityServer -> disconnectIdentityServer()
            is DiscoverySettingsAction.ChangeIdentityServer  -> changeIdentityServer(action)
            is DiscoverySettingsAction.RevokeThreePid        -> revokeThreePid(action)
            is DiscoverySettingsAction.ShareThreePid         -> shareThreePid(action)
            is DiscoverySettingsAction.FinalizeBind3pid      -> finalizeBind3pid(action, true)
            is DiscoverySettingsAction.SubmitMsisdnToken     -> submitMsisdnToken(action)
            is DiscoverySettingsAction.CancelBinding         -> cancelBinding(action)
        }.exhaustive
    }

    private fun disconnectIdentityServer() {
        setState { copy(identityServer = Loading()) }

        viewModelScope.launch {
            try {
                awaitCallback<Unit> { session.identityService().disconnect(it) }
                setState { copy(identityServer = Success(null)) }
            } catch (failure: Throwable) {
                setState { copy(identityServer = Fail(failure)) }
            }
        }
    }

    private fun changeIdentityServer(action: DiscoverySettingsAction.ChangeIdentityServer) {
        setState { copy(identityServer = Loading()) }

        viewModelScope.launch {
            try {
                val data = awaitCallback<String?> {
                    session.identityService().setNewIdentityServer(action.url, it)
                }
                setState { copy(identityServer = Success(data)) }
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
                awaitCallback<Unit> { identityService.startBindThreePid(action.threePid, it) }
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
            is ThreePid.Email  -> revokeEmail(action.threePid)
            is ThreePid.Msisdn -> revokeMsisdn(action.threePid)
        }.exhaustive
    }

    private fun revokeEmail(threePid: ThreePid.Email) = withState { state ->
        if (state.identityServer() == null) return@withState
        if (state.emailList() == null) return@withState
        changeThreePidState(threePid, Loading())

        viewModelScope.launch {
            try {
                awaitCallback<Unit> { identityService.unbindThreePid(threePid, it) }
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
                awaitCallback<Unit> { identityService.unbindThreePid(threePid, it) }
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
                awaitCallback<Unit> { identityService.cancelBindThreePid(action.threePid, it) }
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
        retrieveBinding(session.getThreePids())
    }

    private fun retrieveBinding(threePids: List<ThreePid>) = withState { state ->
        if (state.identityServer().isNullOrBlank()) return@withState

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
                val data = awaitCallback<Map<ThreePid, SharedState>> {
                    identityService.getShareStatus(threePids, it)
                }
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
        if (state.identityServer().isNullOrBlank()) return@withState

        changeThreePidSubmitState(action.threePid, Loading())

        viewModelScope.launch {
            try {
                awaitCallback<Unit> {
                    identityService.submitValidationToken(action.threePid, action.code, it)
                }
                changeThreePidSubmitState(action.threePid, Uninitialized)
                finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(action.threePid), true)
            } catch (failure: Throwable) {
                changeThreePidSubmitState(action.threePid, Fail(failure))
            }
        }
    }

    private fun finalizeBind3pid(action: DiscoverySettingsAction.FinalizeBind3pid, fromUser: Boolean) = withState { state ->
        val threePid = when (action.threePid) {
            is ThreePid.Email  -> {
                state.emailList()?.find { it.threePid.value == action.threePid.email }?.threePid ?: return@withState
            }
            is ThreePid.Msisdn -> {
                state.phoneNumbersList()?.find { it.threePid.value == action.threePid.msisdn }?.threePid ?: return@withState
            }
        }

        changeThreePidSubmitState(action.threePid, Loading())

        viewModelScope.launch {
            try {
                awaitCallback<Unit> { identityService.finalizeBindThreePid(threePid, it) }
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

    private fun refreshPendingEmailBindings() = withState { state ->
        state.emailList()?.forEach { info ->
            when (info.isShared()) {
                SharedState.BINDING_IN_PROGRESS -> finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(info.threePid), false)
                else                            -> Unit
            }
        }
    }
}
