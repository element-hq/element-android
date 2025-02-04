/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.RegistrationResult.FlowResponse
import org.matrix.android.sdk.api.auth.registration.RegistrationResult.Success
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.failure.is401
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject
import org.matrix.android.sdk.api.auth.registration.RegistrationResult as MatrixRegistrationResult

class RegistrationWizardActionDelegate @Inject constructor(
        private val authenticationService: AuthenticationService
) {

    private val registrationWizard: RegistrationWizard
        get() = authenticationService.getRegistrationWizard()

    suspend fun executeAction(action: RegisterAction): RegistrationResult {
        return when (action) {
            RegisterAction.StartRegistration -> resultOf { registrationWizard.getRegistrationFlow() }
            is RegisterAction.CaptchaDone -> resultOf { registrationWizard.performReCaptcha(action.captchaResponse) }
            is RegisterAction.AcceptTerms -> resultOf { registrationWizard.acceptTerms() }
            is RegisterAction.RegisterDummy -> resultOf { registrationWizard.dummy() }
            is RegisterAction.AddThreePid -> handleAddThreePid(registrationWizard, action)
            is RegisterAction.SendAgainThreePid -> resultOf { registrationWizard.sendAgainThreePid() }
            is RegisterAction.ValidateThreePid -> resultOf { registrationWizard.handleValidateThreePid(action.code) }
            is RegisterAction.CheckIfEmailHasBeenValidated -> handleCheckIfEmailIsValidated(registrationWizard, action.delayMillis)
            is RegisterAction.CreateAccount -> resultOf {
                registrationWizard.createAccount(
                        action.username,
                        action.password,
                        action.initialDeviceName
                )
            }
        }
    }

    private suspend fun handleAddThreePid(wizard: RegistrationWizard, action: RegisterAction.AddThreePid): RegistrationResult {
        return runCatching { wizard.addThreePid(action.threePid) }.fold(
                onSuccess = { it.toRegistrationResult() },
                onFailure = {
                    when {
                        action.threePid is RegisterThreePid.Email && it.is401() -> RegistrationResult.SendEmailSuccess(action.threePid)
                        action.threePid is RegisterThreePid.Msisdn && it.is401() -> RegistrationResult.SendMsisdnSuccess(action.threePid)
                        else -> RegistrationResult.Error(it)
                    }
                }
        )
    }

    private tailrec suspend fun handleCheckIfEmailIsValidated(registrationWizard: RegistrationWizard, delayMillis: Long): RegistrationResult {
        return runCatching { registrationWizard.checkIfEmailHasBeenValidated(delayMillis) }.fold(
                onSuccess = { it.toRegistrationResult() },
                onFailure = {
                    when {
                        it.is401() -> null // recursively continue to check with a delay
                        else -> RegistrationResult.Error(it)
                    }
                }
        ) ?: handleCheckIfEmailIsValidated(registrationWizard, 10_000)
    }
}

private inline fun resultOf(block: () -> MatrixRegistrationResult): RegistrationResult {
    return runCatching { block() }.fold(
            onSuccess = { it.toRegistrationResult() },
            onFailure = { RegistrationResult.Error(it) }
    )
}

private fun MatrixRegistrationResult.toRegistrationResult() = when (this) {
    is FlowResponse -> RegistrationResult.NextStep(flowResult)
    is Success -> RegistrationResult.Complete(session)
}

sealed interface RegistrationResult {
    data class Error(val cause: Throwable) : RegistrationResult
    data class Complete(val session: Session) : RegistrationResult
    data class NextStep(val flowResult: FlowResult) : RegistrationResult
    data class SendEmailSuccess(val email: RegisterThreePid.Email) : RegistrationResult
    data class SendMsisdnSuccess(val msisdn: RegisterThreePid.Msisdn) : RegistrationResult
}

sealed interface RegisterAction {
    object StartRegistration : RegisterAction
    data class CreateAccount(val username: String, val password: String, val initialDeviceName: String) : RegisterAction

    data class AddThreePid(val threePid: RegisterThreePid) : RegisterAction
    object SendAgainThreePid : RegisterAction
    data class ValidateThreePid(val code: String) : RegisterAction
    data class CheckIfEmailHasBeenValidated(val delayMillis: Long) : RegisterAction

    data class CaptchaDone(val captchaResponse: String) : RegisterAction
    object AcceptTerms : RegisterAction
    object RegisterDummy : RegisterAction
}

fun RegisterAction.ignoresResult() = when (this) {
    is RegisterAction.SendAgainThreePid -> true
    else -> false
}

fun RegisterAction.hasLoadingState() = when (this) {
    is RegisterAction.CheckIfEmailHasBeenValidated -> false
    else -> true
}
