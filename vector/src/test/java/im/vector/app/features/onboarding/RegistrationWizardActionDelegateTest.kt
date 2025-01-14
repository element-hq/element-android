/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeRegistrationWizard
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.a401ServerError
import io.mockk.coVerifyAll
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.registration.RegistrationResult as MatrixRegistrationResult

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
private val AN_EMAIL_PID_TO_REGISTER = RegisterThreePid.Email("an email")
private val A_PHONE_PID_TO_REGISTER = RegisterThreePid.Msisdn("+11111111111", countryCode = "GB")

class RegistrationWizardActionDelegateTest {

    private val fakeRegistrationWizard = FakeRegistrationWizard()
    private val fakeAuthenticationService = FakeAuthenticationService().also {
        it.givenRegistrationWizard(fakeRegistrationWizard)
    }
    private val registrationActionHandler = RegistrationWizardActionDelegate(fakeAuthenticationService)

    @Test
    fun `when handling register action then delegates to wizard`() = runTest {
        val cases = listOf(
                case(RegisterAction.StartRegistration) { getRegistrationFlow() },
                case(RegisterAction.CaptchaDone(A_CAPTCHA_RESPONSE)) { performReCaptcha(A_CAPTCHA_RESPONSE) },
                case(RegisterAction.AcceptTerms) { acceptTerms() },
                case(RegisterAction.RegisterDummy) { dummy() },
                case(RegisterAction.AddThreePid(AN_EMAIL_PID_TO_REGISTER)) { addThreePid(AN_EMAIL_PID_TO_REGISTER) },
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
        fakeRegistrationWizard.givenAddThreePidErrors(
                cause = a401ServerError(),
                threePid = AN_EMAIL_PID_TO_REGISTER
        )

        val result = registrationActionHandler.executeAction(RegisterAction.AddThreePid(AN_EMAIL_PID_TO_REGISTER))

        result shouldBeEqualTo RegistrationResult.SendEmailSuccess(AN_EMAIL_PID_TO_REGISTER)
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

        val result = registrationActionHandler.executeAction(RegisterAction.CheckIfEmailHasBeenValidated(IGNORED_DELAY))

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
        fakeRegistrationWizard.givenCheckIfEmailHasBeenValidatedErrors(errorsToThrow, finally = MatrixRegistrationResult.Success(A_SESSION))

        val result = registrationActionHandler.executeAction(RegisterAction.CheckIfEmailHasBeenValidated(IGNORED_DELAY))

        fakeRegistrationWizard.verifyCheckedEmailedVerification(times = errorsToThrow.size + 1)
        result shouldBeEqualTo RegistrationResult.Complete(A_SESSION)
    }

    @Test
    fun `given adding an Msisdn ThreePid fails with 401, when handling register action, then infer EmailSuccess`() = runTest {
        fakeRegistrationWizard.givenAddThreePidErrors(
                cause = a401ServerError(),
                threePid = A_PHONE_PID_TO_REGISTER
        )

        val result = registrationActionHandler.executeAction(RegisterAction.AddThreePid(A_PHONE_PID_TO_REGISTER))

        result shouldBeEqualTo RegistrationResult.SendMsisdnSuccess(A_PHONE_PID_TO_REGISTER)
    }

    private suspend fun testSuccessfulActionDelegation(case: Case) {
        val fakeRegistrationWizard = FakeRegistrationWizard()
        val authenticationService = FakeAuthenticationService().also { it.givenRegistrationWizard(fakeRegistrationWizard) }
        val registrationActionHandler = RegistrationWizardActionDelegate(authenticationService)
        fakeRegistrationWizard.givenSuccessFor(result = A_SESSION, case.expect)

        val result = registrationActionHandler.executeAction(case.action)

        coVerifyAll { case.expect(fakeRegistrationWizard) }
        result shouldBeEqualTo AN_EXPECTED_RESULT
    }
}

private fun case(action: RegisterAction, expect: suspend RegistrationWizard.() -> MatrixRegistrationResult) = Case(action, expect)

private class Case(val action: RegisterAction, val expect: suspend RegistrationWizard.() -> MatrixRegistrationResult)
