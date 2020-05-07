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
) {
    enum class SharedState {
        SHARED,
        NOT_SHARED,
        NOT_VERIFIED_FOR_BIND,
        NOT_VERIFIED_FOR_UNBIND
    }
}

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
    data class FinalizeBind3pid(val threePid: ThreePid, val bind: Boolean) : DiscoverySettingsAction()
    data class SubmitMsisdnToken(val msisdn: String, val code: String, val bind: Boolean) : DiscoverySettingsAction()
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

        session.identityService().setNewIdentityServer(action.url, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                setState {
                    copy(
                            identityServer = Success(action.url)
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

    private fun shareThreePid(action: DiscoverySettingsAction.ShareThreePid) {
        when (action.threePid) {
            is ThreePid.Email  -> shareEmail(action.threePid.email)
            is ThreePid.Msisdn -> shareMsisdn(action.threePid.msisdn)
        }.exhaustive
    }

    private fun shareEmail(email: String) = withState { state ->
        if (state.identityServer() == null) return@withState
        changeMailState(email, Loading())

        identityService.startBindSession(ThreePid.Email(email), null,
                object : MatrixCallback<ThreePid> {
                    override fun onSuccess(data: ThreePid) {
                        changeMailState(email, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_BIND)/* TODO , data*/)
                    }

                    override fun onFailure(failure: Throwable) {
                        _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                        changeMailState(email, Fail(failure))
                    }
                })
    }

    private fun changeMailState(address: String, state: Async<PidInfo.SharedState>) {
        setState {
            val currentMails = emailList() ?: emptyList()
            copy(emailList = Success(
                    currentMails.map {
                        if (it.threePid.value == address) {
                            it.copy(isShared = state)
                        } else {
                            it
                        }
                    }
            ))
        }
    }

    private fun changeMsisdnState(address: String, state: Async<PidInfo.SharedState>) {
        setState {
            val phones = phoneNumbersList() ?: emptyList()
            copy(phoneNumbersList = Success(
                    phones.map {
                        if (it.threePid.value == address) {
                            it.copy(isShared = state)
                        } else {
                            it
                        }
                    }
            ))
        }
    }

    private fun revokeThreePid(action: DiscoverySettingsAction.RevokeThreePid) {
        when (action.threePid) {
            is ThreePid.Email  -> revokeEmail(action.threePid.email)
            is ThreePid.Msisdn -> revokeMsisdn(action.threePid.msisdn)
        }.exhaustive
    }

    private fun revokeEmail(email: String) = withState { state ->
        if (state.identityServer() == null) return@withState
        if (state.emailList() == null) return@withState
        changeMailState(email, Loading())

        identityService.startUnBindSession(ThreePid.Email(email), null, object : MatrixCallback<Pair<Boolean, ThreePid?>> {
            override fun onSuccess(data: Pair<Boolean, ThreePid?>) {
                if (data.first) {
                    // requires mail validation
                    changeMailState(email, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_UNBIND) /* TODO , data.second */)
                } else {
                    changeMailState(email, Success(PidInfo.SharedState.NOT_SHARED))
                }
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                changeMailState(email, Fail(failure))
            }
        })
    }

    private fun revokeMsisdn(msisdn: String) = withState { state ->
        if (state.identityServer() == null) return@withState
        if (state.emailList() == null) return@withState
        changeMsisdnState(msisdn, Loading())

        val phoneNumber = PhoneNumberUtil.getInstance()
                .parse("+$msisdn", null)
        val countryCode = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(phoneNumber.countryCode)

        identityService.startUnBindSession(ThreePid.Msisdn(msisdn, countryCode), null, object : MatrixCallback<Pair<Boolean, ThreePid?>> {
            override fun onSuccess(data: Pair<Boolean, ThreePid?>) {
                if (data.first /*requires mail validation */) {
                    changeMsisdnState(msisdn, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_UNBIND) /* TODO , data.second */)
                } else {
                    changeMsisdnState(msisdn, Success(PidInfo.SharedState.NOT_SHARED))
                }
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                changeMsisdnState(msisdn, Fail(failure))
            }
        })

    }

    private fun shareMsisdn(msisdn: String) = withState { state ->
        if (state.identityServer() == null) return@withState
        changeMsisdnState(msisdn, Loading())

        val phoneNumber = PhoneNumberUtil.getInstance()
                .parse("+$msisdn", null)
        val countryCode = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(phoneNumber.countryCode)


        identityService.startBindSession(ThreePid.Msisdn(msisdn, countryCode), null, object : MatrixCallback<ThreePid> {
            override fun onSuccess(data: ThreePid) {
                changeMsisdnState(msisdn, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_BIND) /* TODO , data */)
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                changeMsisdnState(msisdn, Fail(failure))
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
                    isShared = Success(PidInfo.SharedState.SHARED.takeIf { hasMatrixId } ?: PidInfo.SharedState.NOT_SHARED)
            )
        }
    }

    private fun submitMsisdnToken(action: DiscoverySettingsAction.SubmitMsisdnToken) = withState { state ->
        val pid = state.phoneNumbersList()?.find { it.threePid.value == action.msisdn }?.threePid ?: return@withState

        identityService.submitValidationToken(pid,
                action.code,
                object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(ThreePid.Msisdn(action.msisdn), action.bind))
                    }

                    override fun onFailure(failure: Throwable) {
                        _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))
                        changeMsisdnState(action.msisdn, Fail(failure))
                    }
                }
        )
    }

    private fun finalizeBind3pid(action: DiscoverySettingsAction.FinalizeBind3pid) = withState { state ->
        val _3pid = when (action.threePid) {
            is ThreePid.Email  -> {
                changeMailState(action.threePid.email, Loading())
                state.emailList()?.find { it.threePid.value == action.threePid.email }?.threePid ?: return@withState
            }
            is ThreePid.Msisdn -> {
                changeMsisdnState(action.threePid.msisdn, Loading())
                state.phoneNumbersList()?.find { it.threePid.value == action.threePid.msisdn }?.threePid ?: return@withState
            }
        }

        identityService.finalizeBindSessionFor3PID(_3pid, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                val sharedState = Success(if (action.bind) PidInfo.SharedState.SHARED else PidInfo.SharedState.NOT_SHARED)
                when (action.threePid) {
                    is ThreePid.Email  -> changeMailState(action.threePid.email, sharedState)
                    is ThreePid.Msisdn -> changeMsisdnState(action.threePid.msisdn, sharedState)
                }
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                // Restore previous state after an error
                val sharedState = Success(if (action.bind) PidInfo.SharedState.NOT_VERIFIED_FOR_BIND else PidInfo.SharedState.NOT_VERIFIED_FOR_UNBIND)
                when (action.threePid) {
                    is ThreePid.Email  -> changeMailState(action.threePid.email, sharedState)
                    is ThreePid.Msisdn -> changeMsisdnState(action.threePid.msisdn, sharedState)
                }
            }
        })

    }

    private fun refreshPendingEmailBindings() = withState { state ->
        state.emailList()?.forEach { info ->
            when (info.isShared()) {
                PidInfo.SharedState.NOT_VERIFIED_FOR_BIND   -> finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(info.threePid, true))
                PidInfo.SharedState.NOT_VERIFIED_FOR_UNBIND -> finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(info.threePid, false))
                else                                        -> Unit
            }
        }
    }
}
