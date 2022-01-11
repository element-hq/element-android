/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.onboarding

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.internal.network.ssl.Fingerprint

sealed class OnboardingAction : VectorViewModelAction {
    data class OnGetStarted(val resetLoginConfig: Boolean, val onboardingFlow: OnboardingFlow) : OnboardingAction()
    data class OnIAlreadyHaveAnAccount(val resetLoginConfig: Boolean, val onboardingFlow: OnboardingFlow) : OnboardingAction()

    data class UpdateServerType(val serverType: ServerType) : OnboardingAction()
    data class UpdateHomeServer(val homeServerUrl: String) : OnboardingAction()
    data class UpdateSignMode(val signMode: SignMode) : OnboardingAction()
    data class LoginWithToken(val loginToken: String) : OnboardingAction()
    data class WebLoginSuccess(val credentials: Credentials) : OnboardingAction()
    data class InitWith(val loginConfig: LoginConfig?) : OnboardingAction()
    data class ResetPassword(val email: String, val newPassword: String) : OnboardingAction()
    object ResetPasswordMailConfirmed : OnboardingAction()

    // Login or Register, depending on the signMode
    data class LoginOrRegister(val username: String, val password: String, val initialDeviceName: String) : OnboardingAction()

    // Register actions
    open class RegisterAction : OnboardingAction()

    data class AddThreePid(val threePid: RegisterThreePid) : RegisterAction()
    object SendAgainThreePid : RegisterAction()

    // TODO Confirm Email (from link in the email, open in the phone, intercepted by the app)
    data class ValidateThreePid(val code: String) : RegisterAction()

    data class CheckIfEmailHasBeenValidated(val delayMillis: Long) : RegisterAction()
    object StopEmailValidationCheck : RegisterAction()

    data class CaptchaDone(val captchaResponse: String) : RegisterAction()
    object AcceptTerms : RegisterAction()
    object RegisterDummy : RegisterAction()

    // Reset actions
    open class ResetAction : OnboardingAction()

    object ResetHomeServerType : ResetAction()
    object ResetHomeServerUrl : ResetAction()
    object ResetSignMode : ResetAction()
    object ResetLogin : ResetAction()
    object ResetResetPassword : ResetAction()

    // Homeserver history
    object ClearHomeServerHistory : OnboardingAction()

    // For the soft logout case
    data class SetupSsoForSessionRecovery(val homeServerUrl: String,
                                          val deviceId: String,
                                          val ssoIdentityProviders: List<SsoIdentityProvider>?) : OnboardingAction()

    data class PostViewEvent(val viewEvent: OnboardingViewEvents) : OnboardingAction()

    data class UserAcceptCertificate(val fingerprint: Fingerprint) : OnboardingAction()
}
