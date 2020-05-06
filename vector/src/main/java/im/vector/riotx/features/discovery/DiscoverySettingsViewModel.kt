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
import im.vector.matrix.android.api.session.identity.IdentityServiceListener
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction

data class PidInfo(
        val value: String,
        val isShared: Async<SharedState>,
        val _3pid: ThreePid? = null
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
        // TODO Use ViewEvents
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
            if (currentIS != identityServerUrl) refreshModel()
        }
    }

    init {
        startListenToIdentityManager()
        refreshModel()
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
                refreshModel()
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
        changeMailState(email, Loading(), null)

        identityService.startBindSession(ThreePid.Email(email), null,
                object : MatrixCallback<ThreePid> {
                    override fun onSuccess(data: ThreePid) {
                        changeMailState(email, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_BIND), data)
                    }

                    override fun onFailure(failure: Throwable) {
                        _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                        changeMailState(email, Fail(failure))
                    }
                })
    }

    private fun changeMailState(address: String, state: Async<PidInfo.SharedState>, threePid: ThreePid?) {
        setState {
            val currentMails = emailList() ?: emptyList()
            copy(emailList = Success(
                    currentMails.map {
                        if (it.value == address) {
                            it.copy(
                                    _3pid = threePid,
                                    isShared = state
                            )
                        } else {
                            it
                        }
                    }
            ))
        }
    }

    private fun changeMailState(address: String, state: Async<PidInfo.SharedState>) {
        setState {
            val currentMails = emailList() ?: emptyList()
            copy(emailList = Success(
                    currentMails.map {
                        if (it.value == address) {
                            it.copy(isShared = state)
                        } else {
                            it
                        }
                    }
            ))
        }
    }

    private fun changeMsisdnState(address: String, state: Async<PidInfo.SharedState>, threePid: ThreePid?) {
        setState {
            val phones = phoneNumbersList() ?: emptyList()
            copy(phoneNumbersList = Success(
                    phones.map {
                        if (it.value == address) {
                            it.copy(
                                    _3pid = threePid,
                                    isShared = state
                            )
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
                    changeMailState(email, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_UNBIND), data.second)
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
                    changeMsisdnState(msisdn, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_UNBIND), data.second)
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
                changeMsisdnState(msisdn, Success(PidInfo.SharedState.NOT_VERIFIED_FOR_BIND), data)
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(DiscoverySettingsViewEvents.Failure(failure))

                changeMsisdnState(msisdn, Fail(failure))
            }
        })
    }

    private fun changeMsisdnState(msisdn: String, sharedState: Async<PidInfo.SharedState>) {
        setState {
            val currentMsisdns = phoneNumbersList()!!
            copy(phoneNumbersList = Success(
                    currentMsisdns.map {
                        if (it.value == msisdn) {
                            it.copy(isShared = sharedState)
                        } else {
                            it
                        }
                    })
            )
        }
    }

    private fun startListenToIdentityManager() {
        identityService.addListener(identityServerManagerListener)
    }

    private fun stopListenToIdentityManager() {
        identityService.addListener(identityServerManagerListener)
    }

    private fun refreshModel() = withState { state ->
        if (state.identityServer().isNullOrBlank()) return@withState

        setState {
            copy(
                    emailList = Loading(),
                    phoneNumbersList = Loading()
            )
        }

        /* TODO
        session.refreshThirdPartyIdentifiers(object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                _errorLiveEvent.postValue(LiveEvent(failure))

                setState {
                    copy(
                            emailList = Fail(failure),
                            phoneNumbersList = Fail(failure)
                    )
                }
            }

            override fun onSuccess(data: Unit) {
                setState {
                    copy(termsNotSigned = false)
                }

                retrieveBinding()
            }
        })
        */
    }

    private fun retrieveBinding() {
        /* TODO
            val linkedMailsInfo = session.myUser.getlinkedEmails()
            val knownEmails = linkedMailsInfo.map { it.address }
            // Note: it will be a list of "email"
            val knownEmailMedium = linkedMailsInfo.map { it.medium }

            val linkedMsisdnsInfo = session.myUser.getlinkedPhoneNumbers()
            val knownMsisdns = linkedMsisdnsInfo.map { it.address }
            // Note: it will be a list of "msisdn"
            val knownMsisdnMedium = linkedMsisdnsInfo.map { it.medium }

            setState {
                copy(
                        emailList = Success(knownEmails.map { PidInfo(it, Loading()) }),
                        phoneNumbersList = Success(knownMsisdns.map { PidInfo(it, Loading()) })
                )
            }

            identityService.lookup(knownEmails + knownMsisdns,
                    knownEmailMedium + knownMsisdnMedium,
                    object : MatrixCallback<List<FoundThreePid>> {
                        override fun onSuccess(data: List<FoundThreePid>) {
                            setState {
                                copy(
                                        emailList = Success(toPidInfoList(knownEmails, data.take(knownEmails.size))),
                                        phoneNumbersList = Success(toPidInfoList(knownMsisdns, data.takeLast(knownMsisdns.size)))
                                )
                            }
                        }

                        override fun onUnexpectedError(e: Exception) {
                            if (e is TermsNotSignedException) {
                                setState {
                                    // TODO Use ViewEvent
                                    copy(termsNotSigned = true)
                                }
                            }
                            onError(e)
                        }

                        override fun onNetworkError(e: Exception) {
                            onError(e)
                        }

                        override fun onMatrixError(e: MatrixError) {
                            onError(Throwable(e.message))
                        }

                        private fun onError(e: Throwable) {
                            _errorLiveEvent.postValue(LiveEvent(e))

                            setState {
                                copy(
                                        emailList = Success(knownEmails.map { PidInfo(it, Fail(e)) }),
                                        phoneNumbersList = Success(knownMsisdns.map { PidInfo(it, Fail(e)) })
                                )
                            }
                        }
                    })
         */
    }

    private fun toPidInfoList(addressList: List<String>, matrixIds: List<String>): List<PidInfo> {
        return addressList.map {
            val hasMatrixId = matrixIds[addressList.indexOf(it)].isNotBlank()
            PidInfo(
                    value = it,
                    isShared = Success(PidInfo.SharedState.SHARED.takeIf { hasMatrixId } ?: PidInfo.SharedState.NOT_SHARED)
            )
        }
    }

    private fun submitMsisdnToken(action: DiscoverySettingsAction.SubmitMsisdnToken) = withState { state ->
        val pid = state.phoneNumbersList()?.find { it.value == action.msisdn }?._3pid ?: return@withState

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
                state.emailList()?.find { it.value == action.threePid.email }?._3pid ?: return@withState
            }
            is ThreePid.Msisdn -> {
                changeMsisdnState(action.threePid.msisdn, Loading())
                state.phoneNumbersList()?.find { it.value == action.threePid.msisdn }?._3pid ?: return@withState
            }
        }

        identityService.finalizeBindSessionFor3PID(_3pid, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                val sharedState = Success(if (action.bind) PidInfo.SharedState.SHARED else PidInfo.SharedState.NOT_SHARED)
                when (action.threePid) {
                    is ThreePid.Email  -> changeMailState(action.threePid.email, sharedState, null)
                    is ThreePid.Msisdn -> changeMsisdnState(action.threePid.msisdn, sharedState, null)
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
                PidInfo.SharedState.NOT_VERIFIED_FOR_BIND   -> finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(ThreePid.Email(info.value), true))
                PidInfo.SharedState.NOT_VERIFIED_FOR_UNBIND -> finalizeBind3pid(DiscoverySettingsAction.FinalizeBind3pid(ThreePid.Email(info.value), false))
                else                                        -> Unit
            }
        }
    }
}
