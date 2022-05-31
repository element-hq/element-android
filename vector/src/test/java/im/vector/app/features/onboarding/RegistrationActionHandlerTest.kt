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

import im.vector.app.test.fakes.FakeRegistrationWizard
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.a401ServerError
import io.mockk.coVerifyAll
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.registration.RegistrationResult as SdkResult

private const val IGNORED_DELAY = 0L
private val AN_ERROR = RuntimeException()
private val A_SESSION = FakeSession()
private val AN_EXPECTED_RESULT = RegistrationResult.Complete(A_SESSION)
private const val A_USERNAME = "a username"
private const val A_PASSWORD = "a password"
private const val AN_INITIAL_DEVICE_NAME = "a device name"
private const val A_CAPTCHA_RESPONSE = "a captcha response"
private const val A_PID_CODE = "a pid code"
private const val EMAIL_VALIDATED_DELAY = 10000L
private val A_PID_TO_REGISTER = RegisterThreePid.Email("an email")

class RegistrationActionHandlerTest {

    private val fakeRegistrationWizard = FakeRegistrationWizard()
    private val registrationActionHandler = RegistrationActionHandler()

    @Test
    fun `when handling register action then delegates to wizard`() = runTest {
        val cases = listOf(
                case(RegisterAction.StartRegistration) { getRegistrationFlow() },
                case(RegisterAction.CaptchaDone(A_CAPTCHA_RESPONSE)) { performReCaptcha(A_CAPTCHA_RESPONSE) },
                case(RegisterAction.AcceptTerms) { acceptTerms() },
                case(RegisterAction.RegisterDummy) { dummy() },
                case(RegisterAction.AddThreePid(A_PID_TO_REGISTER)) { addThreePid(A_PID_TO_REGISTER) },
                case(RegisterAction.SendAgainThreePid) { sendAgainThreePid() },
                case(RegisterAction.ValidateThreePid(A_PID_CODE)) { handleValidateThreePid(A_PID_CODE) },
                case(RegisterAction.CheckIfEmailHasBeenValidated(EMAIL_VALIDATED_DELAY)) { checkIfEmailHasBeenValidated(EMAIL_VALIDATED_DELAY) },
                case(RegisterAction.CreateAccount(A_USERNAME, A_PASSWORD, AN_INITIAL_DEVICE_NAME)) {
                    createAccount(A_USERNAME, A_PASSWORD, AN_INITIAL_DEVICE_NAME)
                }
        )

        cases.forEach { testSuccessfulActionDelegation(it) }
    }

    @Test
    fun `given adding an email ThreePid fails with 401, when handling register action, then infer EmailSuccess`() = runTest {
        fakeRegistrationWizard.givenAddEmailThreePidErrors(
                cause = a401ServerError(),
                email = A_PID_TO_REGISTER.email
        )

        val result = registrationActionHandler.handleRegisterAction(fakeRegistrationWizard, RegisterAction.AddThreePid(A_PID_TO_REGISTER))

        result shouldBeEqualTo RegistrationResult.SendEmailSuccess(A_PID_TO_REGISTER.email)
    }

    @Test
    fun `given email verification errors with 401 then fatal error, when checking email validation, then continues to poll until non 401 error`() = runTest {
        val errorsToThrow = listOf(
                a401ServerError(),
                a401ServerError(),
                a401ServerError(),
                AN_ERROR
        )
        fakeRegistrationWizard.givenCheckIfEmailHasBeenValidatedErrors(errorsToThrow)

        val result = registrationActionHandler.handleRegisterAction(fakeRegistrationWizard, RegisterAction.CheckIfEmailHasBeenValidated(IGNORED_DELAY))

        fakeRegistrationWizard.verifyCheckedEmailedVerification(times = errorsToThrow.size)
        result shouldBeEqualTo RegistrationResult.Error(AN_ERROR)
    }

    @Test
    fun `given email verification errors with 401 and succeeds, when checking email validation, then continues to poll until success`() = runTest {
        val errorsToThrow = listOf(
                a401ServerError(),
                a401ServerError(),
                a401ServerError()
        )
        fakeRegistrationWizard.givenCheckIfEmailHasBeenValidatedErrors(errorsToThrow, finally = SdkResult.Success(A_SESSION))

        val result = registrationActionHandler.handleRegisterAction(fakeRegistrationWizard, RegisterAction.CheckIfEmailHasBeenValidated(IGNORED_DELAY))

        fakeRegistrationWizard.verifyCheckedEmailedVerification(times = errorsToThrow.size + 1)
        result shouldBeEqualTo RegistrationResult.Complete(A_SESSION)
    }

    private suspend fun testSuccessfulActionDelegation(case: Case) {
        val fakeRegistrationWizard = FakeRegistrationWizard()
        val registrationActionHandler = RegistrationActionHandler()
        fakeRegistrationWizard.givenSuccessFor(result = A_SESSION, case.expect)

        val result = registrationActionHandler.handleRegisterAction(fakeRegistrationWizard, case.action)

        coVerifyAll { case.expect(fakeRegistrationWizard) }
        result shouldBeEqualTo AN_EXPECTED_RESULT
    }
}

private fun case(action: RegisterAction, expect: suspend RegistrationWizard.() -> SdkResult) = Case(action, expect)

private class Case(val action: RegisterAction, val expect: suspend RegistrationWizard.() -> SdkResult)
