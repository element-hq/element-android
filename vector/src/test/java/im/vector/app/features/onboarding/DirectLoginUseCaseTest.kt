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

import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.FakeUriFactory
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.MatrixPatterns.getDomain
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.WellKnown
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult

private val A_LOGIN_OR_REGISTER_ACTION = OnboardingAction.LoginOrRegister("@a-user:id.org", "a-password", "a-device-name")
private val A_WELLKNOWN_SUCCESS_RESULT = WellknownResult.Prompt("https://homeserverurl.com", identityServerUrl = null, WellKnown())
private val A_WELLKNOWN_FAILED_WITH_CONTENT_RESULT = WellknownResult.FailPrompt("https://homeserverurl.com", WellKnown())
private val NO_HOMESERVER_CONFIG: HomeServerConnectionConfig? = null
private val A_FALLBACK_CONFIG: HomeServerConnectionConfig = HomeServerConnectionConfig(
        homeServerUri = FakeUri("https://${A_LOGIN_OR_REGISTER_ACTION.username.getDomain()}").instance,
        homeServerUriBase = FakeUri(A_WELLKNOWN_SUCCESS_RESULT.homeServerUrl).instance,
        identityServerUri = null
)

class DirectLoginUseCaseTest {

    private val fakeAuthenticationService = FakeAuthenticationService()
    private val fakeStringProvider = FakeStringProvider()
    private val fakeSession = FakeSession()

    private val useCase = DirectLoginUseCase(fakeAuthenticationService, fakeStringProvider.instance, FakeUriFactory().instance)

    @Test
    fun `when logging in directly, then returns success with direct session result`() = runTest {
        fakeAuthenticationService.givenWellKnown(A_LOGIN_OR_REGISTER_ACTION.username, config = NO_HOMESERVER_CONFIG, result = A_WELLKNOWN_SUCCESS_RESULT)
        val (username, password, initialDeviceName) = A_LOGIN_OR_REGISTER_ACTION
        fakeAuthenticationService.givenDirectAuthentication(A_FALLBACK_CONFIG, username, password, initialDeviceName, result = fakeSession)

        val result = useCase.execute(A_LOGIN_OR_REGISTER_ACTION, homeServerConnectionConfig = NO_HOMESERVER_CONFIG)

        result shouldBeEqualTo Result.success(fakeSession)
    }

    @Test
    fun `given wellknown fails but has content, when logging in directly, then returns success with direct session result`() = runTest {
        fakeAuthenticationService.givenWellKnown(A_LOGIN_OR_REGISTER_ACTION.username, config = NO_HOMESERVER_CONFIG, result = A_WELLKNOWN_FAILED_WITH_CONTENT_RESULT)
        val (username, password, initialDeviceName) = A_LOGIN_OR_REGISTER_ACTION
        fakeAuthenticationService.givenDirectAuthentication(A_FALLBACK_CONFIG, username, password, initialDeviceName, result = fakeSession)

        val result = useCase.execute(A_LOGIN_OR_REGISTER_ACTION, homeServerConnectionConfig = NO_HOMESERVER_CONFIG)

        result shouldBeEqualTo Result.success(fakeSession)
    }
}
