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
package im.vector.riotx.features.discovery.change

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.identity.IdentityServiceError
import im.vector.matrix.android.api.session.terms.GetTermsResponse
import im.vector.matrix.android.api.session.terms.TermsService
import im.vector.riotx.R
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider

class SetIdentityServerViewModel @AssistedInject constructor(
        @Assisted initialState: SetIdentityServerState,
        private val mxSession: Session,
        stringProvider: StringProvider)
    : VectorViewModel<SetIdentityServerState, SetIdentityServerAction, SetIdentityServerViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SetIdentityServerState): SetIdentityServerViewModel
    }

    companion object : MvRxViewModelFactory<SetIdentityServerViewModel, SetIdentityServerState> {

        override fun initialState(viewModelContext: ViewModelContext): SetIdentityServerState? {
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()

            return SetIdentityServerState(
                    newIdentityServerUrl = session.identityService().getDefaultIdentityServer()
            )
        }

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: SetIdentityServerState): SetIdentityServerViewModel? {
            val fragment: SetIdentityServerFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }

        fun sanitatizeBaseURL(baseUrl: String): String {
            var baseUrl1 = baseUrl
            if (!baseUrl1.startsWith("http://") && !baseUrl1.startsWith("https://")) {
                baseUrl1 = "https://$baseUrl1"
            }
            return baseUrl1
        }
    }

    val userLanguage = stringProvider.getString(R.string.resources_language)

    override fun handle(action: SetIdentityServerAction) {
        when (action) {
            is SetIdentityServerAction.UpdateIdentityServerUrl -> updateIdentityServerUrl(action)
            SetIdentityServerAction.DoChangeIdentityServerUrl  -> doChangeIdentityServerUrl()
        }.exhaustive
    }

    private fun updateIdentityServerUrl(action: SetIdentityServerAction.UpdateIdentityServerUrl) {
        setState {
            copy(
                    newIdentityServerUrl = action.url,
                    errorMessageId = null
            )
        }
    }

    private fun doChangeIdentityServerUrl() = withState {
        var baseUrl: String? = it.newIdentityServerUrl
        if (baseUrl.isNullOrBlank()) {
            setState {
                copy(errorMessageId = R.string.settings_discovery_please_enter_server)
            }
            return@withState
        }
        baseUrl = sanitatizeBaseURL(baseUrl)
        setState {
            copy(isVerifyingServer = true)
        }

        // First ping the identity server v2 API
        mxSession.identityService().isValidIdentityServer(baseUrl, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // Ok, next step
                checkTerms(baseUrl)
            }

            override fun onFailure(failure: Throwable) {
                setState {
                    copy(
                            isVerifyingServer = false,
                            errorMessageId = if (failure is IdentityServiceError.OutdatedIdentityServer) {
                                R.string.identity_server_error_outdated_identity_server
                            } else {
                                R.string.settings_discovery_bad_identity_server
                            }
                    )
                }
            }
        })
    }

    private fun checkTerms(baseUrl: String) {
        mxSession.getTerms(TermsService.ServiceType.IdentityService,
                baseUrl,
                object : MatrixCallback<GetTermsResponse> {
                    override fun onSuccess(data: GetTermsResponse) {
                        // has all been accepted?
                        setState {
                            copy(isVerifyingServer = false)
                        }
                        val resp = data.serverResponse
                        val tos = resp.getLocalizedTerms(userLanguage)
                        if (tos.isEmpty()) {
                            // prompt do not define policy
                            _viewEvents.post(SetIdentityServerViewEvents.NoTerms)
                        } else {
                            val shouldPrompt = tos.any { !data.alreadyAcceptedTermUrls.contains(it.localizedUrl) }
                            if (shouldPrompt) {
                                _viewEvents.post(SetIdentityServerViewEvents.ShowTerms(baseUrl))
                            } else {
                                _viewEvents.post(SetIdentityServerViewEvents.TermsAccepted)
                            }
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        if (failure is Failure.OtherServerError && failure.httpCode == 404) {
                            setState {
                                copy(isVerifyingServer = false)
                            }
                            // 404: Same as NoTerms
                            // TODO Handle the case where identity
                            _viewEvents.post(SetIdentityServerViewEvents.NoTerms)
                        } else {
                            setState {
                                copy(
                                        isVerifyingServer = false,
                                        errorMessageId = R.string.settings_discovery_bad_identity_server
                                )
                            }
                        }
                    }
                })
    }
}
