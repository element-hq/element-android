/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SsoState
import im.vector.app.features.onboarding.StartAuthenticationFlowUseCase.StartAuthenticationResult
import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeUri
import io.mockk.coVerifyOrder
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

private const val A_DECLARED_HOMESERVER_URL = "https://foo.bar"
private val A_HOMESERVER_CONFIG = HomeServerConnectionConfig(homeServerUri = FakeUri().instance)
private val FALLBACK_SSO_IDENTITY_PROVIDERS = emptyList<SsoIdentityProvider>()
private val SSO_IDENTITY_PROVIDERS = listOf(SsoIdentityProvider(id = "id", "name", null, "sso-brand"))
private val SSO_LOGIN_TYPE = listOf(LoginFlowTypes.SSO)
private val SSO_AND_PASSWORD_LOGIN_TYPES = listOf(LoginFlowTypes.SSO, LoginFlowTypes.PASSWORD)
private val PASSWORD_LOGIN_TYPE = listOf(LoginFlowTypes.PASSWORD)

class StartAuthenticationFlowUseCaseTest {

    private val fakeAuthenticationService = FakeAuthenticationService()

    private val useCase = StartAuthenticationFlowUseCase(fakeAuthenticationService)

    @Before
    fun setUp() {
        fakeAuthenticationService.expectedCancelsPendingLogin()
    }

    @Test
    fun `given empty login result when starting authentication flow then returns empty result`() = runTest {
        val loginResult = aLoginResult(supportedLoginTypes = emptyList())
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult()
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given empty sso providers and login supports SSO and Password when starting authentication flow then prefers fallback SsoAndPassword`() = runTest {
        val loginResult = aLoginResult(supportedLoginTypes = SSO_AND_PASSWORD_LOGIN_TYPES, ssoProviders = emptyList())
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = SSO_AND_PASSWORD_LOGIN_TYPES,
                preferredLoginMode = LoginMode.SsoAndPassword(SsoState.Fallback, false),
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given sso providers and login supports SSO and Password when starting authentication flow then prefers SsoAndPassword`() = runTest {
        val loginResult = aLoginResult(supportedLoginTypes = SSO_AND_PASSWORD_LOGIN_TYPES, ssoProviders = SSO_IDENTITY_PROVIDERS)
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = SSO_AND_PASSWORD_LOGIN_TYPES,
                preferredLoginMode = LoginMode.SsoAndPassword(SsoState.IdentityProviders(SSO_IDENTITY_PROVIDERS), false),
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given empty sso providers and login supports SSO when starting authentication flow then prefers fallback Sso`() = runTest {
        val loginResult = aLoginResult(supportedLoginTypes = SSO_LOGIN_TYPE, ssoProviders = emptyList())
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = SSO_LOGIN_TYPE,
                preferredLoginMode = LoginMode.Sso(SsoState.Fallback, false),
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given identity providers and login supports SSO when starting authentication flow then prefers Sso`() = runTest {
        val loginResult = aLoginResult(supportedLoginTypes = SSO_LOGIN_TYPE, ssoProviders = SSO_IDENTITY_PROVIDERS)
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = SSO_LOGIN_TYPE,
                preferredLoginMode = LoginMode.Sso(SsoState.IdentityProviders(SSO_IDENTITY_PROVIDERS), false),
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given login supports Password when starting authentication flow then prefers Password`() = runTest {
        val loginResult = aLoginResult(supportedLoginTypes = PASSWORD_LOGIN_TYPE)
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = PASSWORD_LOGIN_TYPE,
                preferredLoginMode = LoginMode.Password,
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given identity providers and login supports SSO with OIDC compatibility then prefers Sso for compatibility`() = runTest {
        val loginResult = aLoginResult(supportedLoginTypes = SSO_LOGIN_TYPE, ssoProviders = SSO_IDENTITY_PROVIDERS, hasOidcCompatibilityFlow = true)
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = SSO_LOGIN_TYPE,
                preferredLoginMode = LoginMode.Sso(SsoState.IdentityProviders(SSO_IDENTITY_PROVIDERS), hasOidcCompatibilityFlow = true),
                hasOidcCompatibilityFlow = true
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    private fun aLoginResult(
            supportedLoginTypes: List<String>,
            ssoProviders: List<SsoIdentityProvider> = FALLBACK_SSO_IDENTITY_PROVIDERS,
            hasOidcCompatibilityFlow: Boolean = false
    ) = LoginFlowResult(
            supportedLoginTypes = supportedLoginTypes,
            ssoIdentityProviders = ssoProviders,
            isLoginAndRegistrationSupported = true,
            homeServerUrl = A_DECLARED_HOMESERVER_URL,
            isOutdatedHomeserver = false,
            hasOidcCompatibilityFlow = hasOidcCompatibilityFlow,
            isLogoutDevicesSupported = false,
            isLoginWithQrSupported = false,
    )

    private fun expectedResult(
            isHomeserverOutdated: Boolean = false,
            preferredLoginMode: LoginMode = LoginMode.Unsupported,
            supportedLoginTypes: List<String> = emptyList(),
            homeserverSourceUrl: String = A_HOMESERVER_CONFIG.homeServerUri.toString(),
            hasOidcCompatibilityFlow: Boolean = false
    ) = StartAuthenticationResult(
            isHomeserverOutdated,
            SelectedHomeserverState(
                    userFacingUrl = homeserverSourceUrl,
                    upstreamUrl = A_DECLARED_HOMESERVER_URL,
                    preferredLoginMode = preferredLoginMode,
                    supportedLoginTypes = supportedLoginTypes,
                    hasOidcCompatibilityFlow = hasOidcCompatibilityFlow
            )
    )

    private fun verifyClearsAndThenStartsLogin(homeServerConnectionConfig: HomeServerConnectionConfig) {
        coVerifyOrder {
            fakeAuthenticationService.cancelPendingLoginOrRegistration()
            fakeAuthenticationService.getLoginFlow(homeServerConnectionConfig)
        }
    }
}
