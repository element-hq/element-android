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

import im.vector.app.core.extensions.containsAllItems
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.toSsoState
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import javax.inject.Inject

class StartAuthenticationFlowUseCase @Inject constructor(
        private val authenticationService: AuthenticationService,
) {

    suspend fun execute(config: HomeServerConnectionConfig): StartAuthenticationResult {
        authenticationService.cancelPendingLoginOrRegistration()
        val authFlow = authenticationService.getLoginFlow(config)
        val preferredLoginMode = authFlow.findPreferredLoginMode()
        val selection = createSelectedHomeserver(authFlow, config, preferredLoginMode)
        val isOutdated = (preferredLoginMode == LoginMode.Password && !authFlow.isLoginAndRegistrationSupported) || authFlow.isOutdatedHomeserver
        return StartAuthenticationResult(isOutdated, selection)
    }

    private fun createSelectedHomeserver(
            authFlow: LoginFlowResult,
            config: HomeServerConnectionConfig,
            preferredLoginMode: LoginMode
    ) = SelectedHomeserverState(
            userFacingUrl = config.homeServerUri.toString(),
            upstreamUrl = authFlow.homeServerUrl,
            preferredLoginMode = preferredLoginMode,
            supportedLoginTypes = authFlow.supportedLoginTypes,
            isLogoutDevicesSupported = authFlow.isLogoutDevicesSupported
    )

    private fun LoginFlowResult.findPreferredLoginMode() = when {
        supportedLoginTypes.containsAllItems(LoginFlowTypes.SSO, LoginFlowTypes.PASSWORD) -> LoginMode.SsoAndPassword(ssoIdentityProviders.toSsoState())
        supportedLoginTypes.contains(LoginFlowTypes.SSO) -> LoginMode.Sso(ssoIdentityProviders.toSsoState())
        supportedLoginTypes.contains(LoginFlowTypes.PASSWORD) -> LoginMode.Password
        else -> LoginMode.Unsupported
    }

    data class StartAuthenticationResult(
            val isHomeserverOutdated: Boolean,
            val selectedHomeserver: SelectedHomeserverState
    )
}
