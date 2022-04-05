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
import im.vector.app.features.login.LoginMode
import im.vector.app.features.onboarding.StartAuthenticationFlowUseCase.StartAuthenticationResult
import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.toTestString
import io.mockk.coVerifyOrder
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

private const val MATRIX_ORG_URL = "https://any-value.org/"
private const val A_DECLARED_HOMESERVER_URL = "https://foo.bar"
private val A_HOMESERVER_CONFIG = HomeServerConnectionConfig(homeServerUri = FakeUri().instance)
private val SSO_IDENTITY_PROVIDERS = emptyList<SsoIdentityProvider>()

class StartAuthenticationFlowUseCaseTest {

    private val fakeAuthenticationService = FakeAuthenticationService()
    private val fakeStringProvider = FakeStringProvider()

    private val useCase = StartAuthenticationFlowUseCase(fakeAuthenticationService, fakeStringProvider.instance)

    @Before
    fun setUp() {
        fakeAuthenticationService.expectedCancelsPendingLogin()
    }

    @Test
    fun `given empty login result when starting authentication flow then returns empty result`() = runTest {
        val loginResult = aLoginResult()
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult()
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given login supports SSO and Password when starting authentication flow then prefers SsoAndPassword`() = runTest {
        val supportedLoginTypes = listOf(LoginFlowTypes.SSO, LoginFlowTypes.PASSWORD)
        val loginResult = aLoginResult(supportedLoginTypes = supportedLoginTypes)
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = supportedLoginTypes,
                preferredLoginMode = LoginMode.SsoAndPassword(SSO_IDENTITY_PROVIDERS),
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given login supports SSO when starting authentication flow then prefers Sso`() = runTest {
        val supportedLoginTypes = listOf(LoginFlowTypes.SSO)
        val loginResult = aLoginResult(supportedLoginTypes = supportedLoginTypes)
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = supportedLoginTypes,
                preferredLoginMode = LoginMode.Sso(SSO_IDENTITY_PROVIDERS),
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given login supports Password when starting authentication flow then prefers Password`() = runTest {
        val supportedLoginTypes = listOf(LoginFlowTypes.PASSWORD)
        val loginResult = aLoginResult(supportedLoginTypes = supportedLoginTypes)
        fakeAuthenticationService.givenLoginFlow(A_HOMESERVER_CONFIG, loginResult)

        val result = useCase.execute(A_HOMESERVER_CONFIG)

        result shouldBeEqualTo expectedResult(
                supportedLoginTypes = supportedLoginTypes,
                preferredLoginMode = LoginMode.Password,
        )
        verifyClearsAndThenStartsLogin(A_HOMESERVER_CONFIG)
    }

    @Test
    fun `given matrix dot org url when starting authentication flow then provides description`() = runTest {
        val matrixOrgConfig = HomeServerConnectionConfig(homeServerUri = FakeUri(MATRIX_ORG_URL).instance)
        fakeStringProvider.given(R.string.matrix_org_server_url, result = MATRIX_ORG_URL)
        fakeAuthenticationService.givenLoginFlow(matrixOrgConfig, aLoginResult())

        val result = useCase.execute(matrixOrgConfig)

        result shouldBeEqualTo expectedResult(
                description = R.string.ftue_auth_create_account_matrix_dot_org_server_description.toTestString(),
                homeserverSourceUrl = MATRIX_ORG_URL
        )
        verifyClearsAndThenStartsLogin(matrixOrgConfig)
    }

    private fun aLoginResult(
            supportedLoginTypes: List<String> = emptyList()
    ) = LoginFlowResult(
            supportedLoginTypes = supportedLoginTypes,
            ssoIdentityProviders = SSO_IDENTITY_PROVIDERS,
            isLoginAndRegistrationSupported = true,
            homeServerUrl = A_DECLARED_HOMESERVER_URL,
            isOutdatedHomeserver = false
    )

    private fun expectedResult(
            isHomeserverOutdated: Boolean = false,
            description: String? = null,
            preferredLoginMode: LoginMode = LoginMode.Unsupported,
            supportedLoginTypes: List<String> = emptyList(),
            homeserverSourceUrl: String = A_HOMESERVER_CONFIG.homeServerUri.toString()
    ) = StartAuthenticationResult(
            isHomeserverOutdated,
            SelectedHomeserverState(
                    description = description,
                    userFacingUrl = homeserverSourceUrl,
                    upstreamUrl = A_DECLARED_HOMESERVER_URL,
                    preferredLoginMode = preferredLoginMode,
                    supportedLoginTypes = supportedLoginTypes
            )
    )

    private fun verifyClearsAndThenStartsLogin(homeServerConnectionConfig: HomeServerConnectionConfig) {
        coVerifyOrder {
            fakeAuthenticationService.cancelPendingLoginOrRegistration()
            fakeAuthenticationService.getLoginFlow(homeServerConnectionConfig)
        }
    }
}
