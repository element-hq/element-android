/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.onboarding

import im.vector.app.R
import im.vector.app.core.extensions.andThen
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.onboarding.OnboardingAction.AuthenticateAction.LoginDirect
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class DirectLoginUseCase @Inject constructor(
        private val authenticationService: AuthenticationService,
        private val stringProvider: StringProvider,
        private val uriFactory: UriFactory
) {

    suspend fun execute(action: LoginDirect, homeServerConnectionConfig: HomeServerConnectionConfig?): Result<Session> {
        return fetchWellKnown(action.matrixId, homeServerConnectionConfig)
                .andThen { wellKnown -> createSessionFor(wellKnown, action, homeServerConnectionConfig) }
    }

    private suspend fun fetchWellKnown(matrixId: String, config: HomeServerConnectionConfig?) = runCatching {
        authenticationService.getWellKnownData(matrixId, config)
    }

    private suspend fun createSessionFor(data: WellknownResult, action: LoginDirect, config: HomeServerConnectionConfig?) = when (data) {
        is WellknownResult.Prompt -> loginDirect(action, data, config)
        is WellknownResult.FailPrompt -> handleFailPrompt(data, action, config)
        else -> onWellKnownError()
    }

    private suspend fun handleFailPrompt(data: WellknownResult.FailPrompt, action: LoginDirect, config: HomeServerConnectionConfig?): Result<Session> {
        // Relax on IS discovery if homeserver is valid
        val isMissingInformationToLogin = data.homeServerUrl == null || data.wellKnown == null
        return when {
            isMissingInformationToLogin -> onWellKnownError()
            else -> loginDirect(action, WellknownResult.Prompt(data.homeServerUrl!!, null, data.wellKnown!!), config)
        }
    }

    private suspend fun loginDirect(action: LoginDirect, wellKnownPrompt: WellknownResult.Prompt, config: HomeServerConnectionConfig?): Result<Session> {
        val alteredHomeServerConnectionConfig = config?.updateWith(wellKnownPrompt) ?: fallbackConfig(action, wellKnownPrompt)
        return runCatching {
            authenticationService.directAuthentication(
                    alteredHomeServerConnectionConfig,
                    action.matrixId,
                    action.password,
                    action.initialDeviceName
            )
        }
    }

    private fun HomeServerConnectionConfig.updateWith(wellKnownPrompt: WellknownResult.Prompt) = copy(
            homeServerUriBase = uriFactory.parse(wellKnownPrompt.homeServerUrl),
            identityServerUri = wellKnownPrompt.identityServerUrl?.let { uriFactory.parse(it) }
    )

    private fun fallbackConfig(action: LoginDirect, wellKnownPrompt: WellknownResult.Prompt) = HomeServerConnectionConfig(
            homeServerUri = uriFactory.parse("https://${action.matrixId.getServerName()}"),
            homeServerUriBase = uriFactory.parse(wellKnownPrompt.homeServerUrl),
            identityServerUri = wellKnownPrompt.identityServerUrl?.let { uriFactory.parse(it) }
    )

    private fun onWellKnownError() = Result.failure<Session>(Exception(stringProvider.getString(R.string.autodiscover_well_known_error)))
}
