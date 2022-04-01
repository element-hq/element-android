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
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.ensureTrailingSlash
import im.vector.app.features.login.LoginMode
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import javax.inject.Inject

class StartAuthenticationFlowUseCase @Inject constructor(
        private val authenticationService: AuthenticationService,
        private val stringProvider: StringProvider
) {

    data class Bar(
            val isHomeserverOutdated: Boolean,
            val serverSelectionState: SelectedHomeserverState,
            val loginMode: LoginMode,
            val supportedLoginTypes: List<String>,
    )

    suspend fun execute(config: HomeServerConnectionConfig): Bar {
        authenticationService.cancelPendingLoginOrRegistration()
        val authFlow = authenticationService.getLoginFlow(config)

        val loginMode = when {
            // SSO login is taken first
            authFlow.supportedLoginTypes.contains(LoginFlowTypes.SSO) &&
                    authFlow.supportedLoginTypes.contains(LoginFlowTypes.PASSWORD) -> LoginMode.SsoAndPassword(authFlow.ssoIdentityProviders)
            authFlow.supportedLoginTypes.contains(LoginFlowTypes.SSO)              -> LoginMode.Sso(authFlow.ssoIdentityProviders)
            authFlow.supportedLoginTypes.contains(LoginFlowTypes.PASSWORD)         -> LoginMode.Password
            else                                                                   -> LoginMode.Unsupported
        }

        val matrixOrgUrl = stringProvider.getString(R.string.matrix_org_server_url).ensureTrailingSlash()
        val selection = SelectedHomeserverState(
                description = when (authFlow.homeServerUrl) {
                    matrixOrgUrl -> stringProvider.getString(R.string.ftue_auth_create_account_matrix_dot_org_server_description)
                    else         -> null
                },
                sourceUrl = config.homeServerUri.toString(),
                declaredUrl = authFlow.homeServerUrl,
                preferredLoginMode = loginMode,
                loginModeSupportedTypes = authFlow.supportedLoginTypes
        )
        val isOutdated = (loginMode == LoginMode.Password && !authFlow.isLoginAndRegistrationSupported) || authFlow.isOutdatedHomeserver
        return Bar(isOutdated, selection, loginMode, authFlow.supportedLoginTypes.toList())
    }
}
