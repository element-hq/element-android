/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.FakeUriFactory
import im.vector.app.test.fakes.toTestString
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.should
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.WellKnown
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult

private val A_DIRECT_LOGIN_ACTION = OnboardingAction.AuthenticateAction.LoginDirect("@a-user:id.org", "a-password", "a-device-name")
private val A_WELLKNOWN_SUCCESS_RESULT = WellknownResult.Prompt("https://homeserverurl.com", identityServerUrl = null, WellKnown())
private val A_WELLKNOWN_FAILED_WITH_CONTENT_RESULT = WellknownResult.FailPrompt("https://homeserverurl.com", WellKnown())
private val A_WELLKNOWN_FAILED_WITHOUT_CONTENT_RESULT = WellknownResult.FailPrompt(null, null)
private val NO_HOMESERVER_CONFIG: HomeServerConnectionConfig? = null
private val A_FALLBACK_CONFIG: HomeServerConnectionConfig = HomeServerConnectionConfig(
        homeServerUri = FakeUri("https://${A_DIRECT_LOGIN_ACTION.matrixId.getServerName()}").instance,
        homeServerUriBase = FakeUri(A_WELLKNOWN_SUCCESS_RESULT.homeServerUrl).instance,
        identityServerUri = null
)
private val AN_ERROR = RuntimeException()

class DirectLoginUseCaseTest {

    private val fakeAuthenticationService = FakeAuthenticationService()
    private val fakeStringProvider = FakeStringProvider()
    private val fakeSession = FakeSession()

    private val useCase = DirectLoginUseCase(fakeAuthenticationService, fakeStringProvider.instance, FakeUriFactory().instance)

    @Test
    fun `when logging in directly, then returns success with direct session result`() = runTest {
        fakeAuthenticationService.givenWellKnown(A_DIRECT_LOGIN_ACTION.matrixId, config = NO_HOMESERVER_CONFIG, result = A_WELLKNOWN_SUCCESS_RESULT)
        val (username, password, initialDeviceName) = A_DIRECT_LOGIN_ACTION
        fakeAuthenticationService.givenDirectAuthentication(A_FALLBACK_CONFIG, username, password, initialDeviceName, result = fakeSession)

        val result = useCase.execute(A_DIRECT_LOGIN_ACTION, homeServerConnectionConfig = NO_HOMESERVER_CONFIG)

        result shouldBeEqualTo Result.success(fakeSession)
    }

    @Test
    fun `given wellknown fails with content, when logging in directly, then returns success with direct session result`() = runTest {
        fakeAuthenticationService.givenWellKnown(
                A_DIRECT_LOGIN_ACTION.matrixId,
                config = NO_HOMESERVER_CONFIG,
                result = A_WELLKNOWN_FAILED_WITH_CONTENT_RESULT
        )
        val (username, password, initialDeviceName) = A_DIRECT_LOGIN_ACTION
        fakeAuthenticationService.givenDirectAuthentication(A_FALLBACK_CONFIG, username, password, initialDeviceName, result = fakeSession)

        val result = useCase.execute(A_DIRECT_LOGIN_ACTION, homeServerConnectionConfig = NO_HOMESERVER_CONFIG)

        result shouldBeEqualTo Result.success(fakeSession)
    }

    @Test
    fun `given wellknown fails without content, when logging in directly, then returns well known error`() = runTest {
        fakeAuthenticationService.givenWellKnown(
                A_DIRECT_LOGIN_ACTION.matrixId,
                config = NO_HOMESERVER_CONFIG,
                result = A_WELLKNOWN_FAILED_WITHOUT_CONTENT_RESULT
        )
        val (username, password, initialDeviceName) = A_DIRECT_LOGIN_ACTION
        fakeAuthenticationService.givenDirectAuthentication(A_FALLBACK_CONFIG, username, password, initialDeviceName, result = fakeSession)

        val result = useCase.execute(A_DIRECT_LOGIN_ACTION, homeServerConnectionConfig = NO_HOMESERVER_CONFIG)

        result should { this.isFailure }
        result should { this.exceptionOrNull() is Exception }
        result should { this.exceptionOrNull()?.message == CommonStrings.autodiscover_well_known_error.toTestString() }
    }

    @Test
    fun `given wellknown throws, when logging in directly, then returns failure result with original cause`() = runTest {
        fakeAuthenticationService.givenWellKnownThrows(A_DIRECT_LOGIN_ACTION.matrixId, config = NO_HOMESERVER_CONFIG, cause = AN_ERROR)

        val result = useCase.execute(A_DIRECT_LOGIN_ACTION, homeServerConnectionConfig = NO_HOMESERVER_CONFIG)

        result shouldBeEqualTo Result.failure(AN_ERROR)
    }

    @Test
    fun `given direct authentication throws, when logging in directly, then returns failure result with original cause`() = runTest {
        fakeAuthenticationService.givenWellKnown(A_DIRECT_LOGIN_ACTION.matrixId, config = NO_HOMESERVER_CONFIG, result = A_WELLKNOWN_SUCCESS_RESULT)
        val (username, password, initialDeviceName) = A_DIRECT_LOGIN_ACTION
        fakeAuthenticationService.givenDirectAuthenticationThrows(A_FALLBACK_CONFIG, username, password, initialDeviceName, cause = AN_ERROR)

        val result = useCase.execute(A_DIRECT_LOGIN_ACTION, homeServerConnectionConfig = NO_HOMESERVER_CONFIG)

        result shouldBeEqualTo Result.failure(AN_ERROR)
    }
}
