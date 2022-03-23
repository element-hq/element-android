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
package im.vector.app.features.discovery.change

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoints
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.SingletonEntryPoint
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.ensureProtocol
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.terms.TermsService
import java.net.UnknownHostException

class SetIdentityServerViewModel @AssistedInject constructor(
        @Assisted initialState: SetIdentityServerState,
        private val mxSession: Session,
        stringProvider: StringProvider) :
    VectorViewModel<SetIdentityServerState, SetIdentityServerAction, SetIdentityServerViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SetIdentityServerViewModel, SetIdentityServerState> {
        override fun create(initialState: SetIdentityServerState): SetIdentityServerViewModel
    }

    companion object : MavericksViewModelFactory<SetIdentityServerViewModel, SetIdentityServerState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): SetIdentityServerState {
            val session = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java).activeSessionHolder().getActiveSession()
            return SetIdentityServerState(
                    homeServerUrl = session.sessionParams.homeServerUrl,
                    defaultIdentityServerUrl = session.identityService().getDefaultIdentityServer()
            )
        }
    }

    var currentWantedUrl: String? = null
        private set

    private val userLanguage = stringProvider.getString(R.string.resources_language)

    override fun handle(action: SetIdentityServerAction) {
        when (action) {
            SetIdentityServerAction.UseDefaultIdentityServer   -> useDefault()
            is SetIdentityServerAction.UseCustomIdentityServer -> usedCustomIdentityServerUrl(action)
        }
    }

    private fun useDefault() = withState { state ->
        state.defaultIdentityServerUrl?.let { doChangeIdentityServerUrl(it, true) }
    }

    private fun usedCustomIdentityServerUrl(action: SetIdentityServerAction.UseCustomIdentityServer) {
        doChangeIdentityServerUrl(action.url, false)
    }

    private fun doChangeIdentityServerUrl(url: String, isDefault: Boolean) {
        if (url.isEmpty()) {
            _viewEvents.post(SetIdentityServerViewEvents.Failure(R.string.settings_discovery_please_enter_server, isDefault))
            return
        }
        val baseUrl = url.ensureProtocol().also { currentWantedUrl = it }

        _viewEvents.post(SetIdentityServerViewEvents.Loading())

        viewModelScope.launch {
            try {
                // First ping the identity server v2 API
                mxSession.identityService().isValidIdentityServer(baseUrl)
                // Ok, next step
                checkTerms(baseUrl)
            } catch (failure: Throwable) {
                when {
                    failure is IdentityServiceError.OutdatedIdentityServer                              ->
                        _viewEvents.post(SetIdentityServerViewEvents.Failure(R.string.identity_server_error_outdated_identity_server, isDefault))
                    failure is Failure.NetworkConnection && failure.ioException is UnknownHostException ->
                        _viewEvents.post(SetIdentityServerViewEvents.Failure(R.string.settings_discovery_bad_identity_server, isDefault))
                    else                                                                                ->
                        _viewEvents.post(SetIdentityServerViewEvents.OtherFailure(failure))
                }
            }
        }
    }

    private suspend fun checkTerms(baseUrl: String) {
        try {
            val data = mxSession.getTerms(TermsService.ServiceType.IdentityService, baseUrl)

            // has all been accepted?
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
        } catch (failure: Throwable) {
            if (failure is Failure.OtherServerError && failure.httpCode == 404) {
                // 404: Same as NoTerms
                _viewEvents.post(SetIdentityServerViewEvents.NoTerms)
            } else {
                _viewEvents.post(SetIdentityServerViewEvents.OtherFailure(failure))
            }
        }
    }
}
