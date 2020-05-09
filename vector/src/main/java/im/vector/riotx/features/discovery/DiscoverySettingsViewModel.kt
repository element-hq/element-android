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
package im.vector.riotx.features.discovery

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.identity.FoundThreePid
import im.vector.matrix.android.api.session.identity.IdentityServiceError
import im.vector.matrix.android.api.session.identity.IdentityServiceListener
import im.vector.matrix.android.api.session.identity.SharedState
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.rx.rx
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction

data class PidInfo(
        // Retrieved from the homeserver
        val threePid: ThreePid,
        // Retrieved from IdentityServer, or transient state
        val isShared: Async<SharedState>
)

data class DiscoverySettingsState(
        val identityServer: Async<String?> = Uninitialized,
        val emailList: Async<List<PidInfo>> = Uninitialized,
        val phoneNumbersList: Async<List<PidInfo>> = Uninitialized,
        // TODO Use ViewEvents?
        val termsNotSigned: Boolean = false
) : MvRxState

sealed class DiscoverySettingsAction : VectorViewModelAction {
    object RetrieveBinding : DiscoverySettingsAction()
    object Refresh : DiscoverySettingsAction()

    data class ChangeIdentityServer(val url: String?) : DiscoverySettingsAction()
    data class RevokeThreePid(val threePid: ThreePid) : DiscoverySettingsAction()
    data class ShareThreePid(val threePid: ThreePid) : DiscoverySettingsAction()
    data class FinalizeBind3pid(val threePid: ThreePid) : DiscoverySettingsAction()
    data class SubmitMsisdnToken(val threePid: ThreePid.Msisdn, val code: String) : DiscoverySettingsAction()
}

sealed class DiscoverySettingsViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : DiscoverySettingsViewEvents()
}

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
            val identityServerUrl = identityService.getCurrentIdentityServer()
            val currentIS = state.identityServer()
            setState {
                copy(identityServer = Success(identityServerUrl))
            }
            if (currentIS != identityServerUrl) retrieveBinding()
        }
    }

    init {
        setState {
            copy(identityServer = Success(identityService.getCurrentIdentityServer()))
        }
        startListenToIdentityManager()
        observeThreePids()
    }

    private fun observeThreePids() {
        session.rx()
                .liveThreePIds()
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
            DiscoverySettingsAction.Refresh                 -> refreshPendingEmailBindings()
            DiscoverySettingsAction.RetrieveBinding         -> retrieveBinding()
            is DiscoverySettingsAction.ChangeIdentityServer -> changeIdentityServer(action)
            is DiscoverySettingsAction.RevokeThreePid       -> revokeThreePid(action)
            is DiscoverySettingsAction.ShareThreePid        -> shareThreePid(action)
            is DiscoverySettingsAction.FinalizeBind3pid     -> finalizeBind3pid(action)
            is DiscoverySettingsAction.SubmitMsisdnToken    -> submitMsisdnToken(action)
        }.exhaustive
    }

    private fun changeIdentityServer(action: DiscoverySettingsAction.ChangeIdentityServer) {
        setState {
            copy(
                    identityServer = Loading()
            )
        }

        session.identityService().setNewIdentityServer(action.url, object : MatrixCallback<String?> {
            override fun onSuccess(data: String?) {
                setState {
                    copy(
                            identityServer = Success(data)
                    )
                }
                retrieveBinding()
            }

            override fun onFailure(failure: Throwable) {
                setState {
                    copy(
                            identityServer = Fail(failure)
                    )
                }
            }
        })
    }

    private fun shareThreePid(action: DiscoverySettingsAction.ShareThreePid) = withState { state ->
        if (state.identityServer() == null) return@withState
        changeThreePidState(action.threePid, Loading())

        val threePid = if (action.threePid is ThreePid.Msisdn && action.threePid.countryCode == null) {
            // Ensure we have a country code

            val phoneNumber = PhoneNumberUtil.getInstance()
                    .parse("+${action.threePid.msisdn}", null)
            action.threePid.copy(countryCode = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(phoneNumber.countryCode)
            )
        } else {
            action.threePid
        }

        identityService.startBindThreePid(threePid, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                changeThreePidState(action.threePid, Success(SharedState.BINDING_IN_PROGRESS))
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                changeThreePidState(action.threePid, Fail(failure))
            }
        })
    }

    private fun changeThreePidState(threePid: ThreePid, state: Async<SharedState>) {
        setState {
            val currentMails = emailList() ?: emptyList()
            val phones = phoneNumbersList() ?: emptyList()
            copy(emailList = Success(
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

        identityService.unbindThreePid(threePid, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                changeThreePidState(threePid, Success(SharedState.NOT_SHARED))
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                changeThreePidState(threePid, Fail(failure))
            }
        })
    }

    private fun revokeMsisdn(threePid: ThreePid.Msisdn) = withState { state ->
        if (state.identityServer() == null) return@withState
        if (state.phoneNumbersList() == null) return@withState
        changeThreePidState(threePid, Loading())

        identityService.unbindThreePid(threePid, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                changeThreePidState(threePid, Success(SharedState.NOT_SHARED))
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                changeThreePidState(threePid, Fail(failure))
            }
        })
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

        identityService.lookUp(threePids,
                object : MatrixCallback<List<FoundThreePid>> {
                    override fun onSuccess(data: List<FoundThreePid>) {
                        setState {
                            copy(
                                    emailList = Success(toPidInfoList(emails, data)),
                                    phoneNumbersList = Success(toPidInfoList(msisdns, data))
                            )
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        if (failure is IdentityServiceError.TermsNotSignedException) {
                            setState {
                                // TODO Use ViewEvent ?
                                copy(termsNotSigned = true)
                            }
                        }

                        _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                        setState {
                            copy(
                                    emailList = Success(emails.map { PidInfo(it, Fail(failure)) }),
                                    phoneNumbersList = Success(msisdns.map { PidInfo(it, Fail(failure)) })
                            )
                        }
                    }
                })
    }

    private fun toPidInfoList(threePids: List<ThreePid>, foundThreePids: List<FoundThreePid>): List<PidInfo> {
        return threePids.map { threePid ->
            val hasMatrixId = foundThreePids.any { it.threePid == threePid }
            PidInfo(
                    threePid = threePid,
                    isShared = Success(SharedState.SHARED.takeIf { hasMatrixId } ?: SharedState.NOT_SHARED)
            )
        }
    }

    private fun submitMsisdnToken(action: DiscoverySettingsAction.SubmitMsisdnToken) = withState { state ->
        if (state.identityServer().isNullOrBlank()) return@withState

        identityService.submitValidationToken(action.threePid,
                action.code,
                object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        // TODO This should be done in the task
                        finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(action.threePid))
                    }

                    override fun onFailure(failure: Throwable) {
                        _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                        changeThreePidState(action.threePid, Fail(failure))
                    }
                }
        )
    }

    private fun finalizeBind3pid(action: DiscoverySettingsAction.FinalizeBind3pid) = withState { state ->
        val threePid = when (action.threePid) {
            is ThreePid.Email  -> {
                changeThreePidState(action.threePid, Loading())
                state.emailList()?.find { it.threePid.value == action.threePid.email }?.threePid ?: return@withState
            }
            is ThreePid.Msisdn -> {
                changeThreePidState(action.threePid, Loading())
                state.phoneNumbersList()?.find { it.threePid.value == action.threePid.msisdn }?.threePid ?: return@withState
            }
        }

        identityService.finalizeBindThreePid(threePid, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                changeThreePidState(action.threePid, Success(SharedState.SHARED))
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                // Restore previous state after an error
                changeThreePidState(action.threePid, Success(SharedState.BINDING_IN_PROGRESS))
            }
        })

    }

    private fun refreshPendingEmailBindings() = withState { state ->
        state.emailList()?.forEach { info ->
            when (info.isShared()) {
                SharedState.BINDING_IN_PROGRESS -> finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(info.threePid))
                else                            -> Unit
            }
        }
    }
}
