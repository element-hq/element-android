/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
            hasOidcCompatibilityFlow = authFlow.hasOidcCompatibilityFlow,
            isLogoutDevicesSupported = authFlow.isLogoutDevicesSupported,
            isLoginWithQrSupported = authFlow.isLoginWithQrSupported
    )

    private fun LoginFlowResult.findPreferredLoginMode() = when {
        supportedLoginTypes.containsAllItems(LoginFlowTypes.SSO, LoginFlowTypes.PASSWORD) -> LoginMode.SsoAndPassword(
                ssoIdentityProviders.toSsoState(),
                hasOidcCompatibilityFlow
        )
        supportedLoginTypes.contains(LoginFlowTypes.SSO) -> LoginMode.Sso(ssoIdentityProviders.toSsoState(), hasOidcCompatibilityFlow)
        supportedLoginTypes.contains(LoginFlowTypes.PASSWORD) -> LoginMode.Password
        else -> LoginMode.Unsupported
    }

    data class StartAuthenticationResult(
            val isHomeserverOutdated: Boolean,
            val selectedHomeserver: SelectedHomeserverState
    )
}
