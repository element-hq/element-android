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

import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import javax.inject.Inject

class RegistrationActionHandler @Inject constructor() {

    suspend fun handleRegisterAction(registrationWizard: RegistrationWizard, action: RegisterAction): RegistrationResult {
        return when (action) {
            RegisterAction.StartRegistration               -> registrationWizard.getRegistrationFlow()
            is RegisterAction.CaptchaDone                  -> registrationWizard.performReCaptcha(action.captchaResponse)
            is RegisterAction.AcceptTerms                  -> registrationWizard.acceptTerms()
            is RegisterAction.RegisterDummy                -> registrationWizard.dummy()
            is RegisterAction.AddThreePid                  -> registrationWizard.addThreePid(action.threePid)
            is RegisterAction.SendAgainThreePid            -> registrationWizard.sendAgainThreePid()
            is RegisterAction.ValidateThreePid             -> registrationWizard.handleValidateThreePid(action.code)
            is RegisterAction.CheckIfEmailHasBeenValidated -> registrationWizard.checkIfEmailHasBeenValidated(action.delayMillis)
            is RegisterAction.CreateAccount                -> registrationWizard.createAccount(action.username, action.password, action.initialDeviceName)
        }
    }
}

sealed interface RegisterAction {
    object StartRegistration : RegisterAction
    data class CreateAccount(val username: String, val password: String, val initialDeviceName: String) : RegisterAction

    data class AddThreePid(val threePid: RegisterThreePid) : RegisterAction
    object SendAgainThreePid : RegisterAction

    // TODO Confirm Email (from link in the email, open in the phone, intercepted by the app)
    data class ValidateThreePid(val code: String) : RegisterAction

    data class CheckIfEmailHasBeenValidated(val delayMillis: Long) : RegisterAction

    data class CaptchaDone(val captchaResponse: String) : RegisterAction
    object AcceptTerms : RegisterAction
    object RegisterDummy : RegisterAction
}

fun RegisterAction.ignoresResult() = when (this) {
    is RegisterAction.AddThreePid       -> true
    is RegisterAction.SendAgainThreePid -> true
    else                                -> false
}

fun RegisterAction.hasLoadingState() = when (this) {
    is RegisterAction.CheckIfEmailHasBeenValidated -> false
    else                                           -> true
}
