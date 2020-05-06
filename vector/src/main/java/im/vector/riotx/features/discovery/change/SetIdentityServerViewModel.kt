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

import androidx.annotation.StringRes
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.R
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction
import im.vector.riotx.core.resources.StringProvider

data class SetIdentityServerState(
        val existingIdentityServer: String? = null,
        val newIdentityServer: String? = null,
        @StringRes val errorMessageId: Int? = null,
        val isVerifyingServer: Boolean = false
) : MvRxState

sealed class SetIdentityServerAction : VectorViewModelAction {
    data class UpdateServerName(val url: String) : SetIdentityServerAction()
    object DoChangeServerName : SetIdentityServerAction()
}

sealed class SetIdentityServerViewEvents : VectorViewEvents {
    data class ShowTerms(val newIdentityServer: String) : SetIdentityServerViewEvents()
    object NoTerms : SetIdentityServerViewEvents()
    object TermsAccepted : SetIdentityServerViewEvents()
}

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
            is SetIdentityServerAction.UpdateServerName -> updateServerName(action)
            SetIdentityServerAction.DoChangeServerName  -> doChangeServerName()
        }.exhaustive
    }

    private fun updateServerName(action: SetIdentityServerAction.UpdateServerName) {
        setState {
            copy(
                    newIdentityServer = action.url,
                    errorMessageId = null
            )
        }
    }

    private fun doChangeServerName() = withState {
        var baseUrl: String? = it.newIdentityServer
        if (baseUrl.isNullOrBlank()) {
            setState {
                copy(errorMessageId = R.string.settings_discovery_please_enter_server)
            }
            return@withState
        }
        // TODO baseUrl = sanitatizeBaseURL(baseUrl)
        setState {
            copy(isVerifyingServer = true)
        }

        /* TODO
        mxSession.termsManager.get(TermsManager.ServiceType.IdentityService,
                baseUrl,
                object : ApiCallback<GetTermsResponse> {
                    override fun onSuccess(info: GetTermsResponse) {
                        //has all been accepted?
                        setState {
                            copy(isVerifyingServer = false)
                        }
                        val resp = info.serverResponse
                        val tos = resp.getLocalizedTerms(userLanguage)
                        if (tos.isEmpty()) {
                            //prompt do not define policy
                            navigateEvent.value = LiveEvent(NavigateEvent.NoTerms)
                        } else {
                            val shouldPrompt = tos.any { !info.alreadyAcceptedTermUrls.contains(it.localizedUrl) }
                            if (shouldPrompt) {
                                navigateEvent.value = LiveEvent(NavigateEvent.ShowTerms(baseUrl))
                            } else {
                                navigateEvent.value = LiveEvent(NavigateEvent.TermsAccepted)
                            }
                        }
                    }

                    override fun onUnexpectedError(e: Exception) {
                        if (e is HttpException && e.httpError.httpCode == 404) {
                            setState {
                                copy(isVerifyingServer = false)
                            }
                            navigateEvent.value = LiveEvent(NavigateEvent.NoTerms)
                        } else {
                            setState {
                                copy(
                                        isVerifyingServer = false,
                                        errorMessageId = R.string.settings_discovery_bad_identity_server
                                )
                            }
                        }
                    }

                    override fun onNetworkError(e: Exception) {
                        setState {
                            copy(
                                    isVerifyingServer = false,
                                    errorMessageId = R.string.settings_discovery_bad_identity_server
                            )
                        }
                    }

                    override fun onMatrixError(e: MatrixError) {
                        setState {
                            copy(
                                    isVerifyingServer = false,
                                    errorMessageId = R.string.settings_discovery_bad_identity_server
                            )
                        }
                    }
                })

         */
    }
}
