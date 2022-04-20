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

package im.vector.app.features.login

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.network.ssl.Fingerprint

sealed class LoginAction : VectorViewModelAction {
    data class OnGetStarted(val resetLoginConfig: Boolean) : LoginAction()

    data class UpdateServerType(val serverType: ServerType) : LoginAction()
    data class UpdateHomeServer(val homeServerUrl: String) : LoginAction()
    data class UpdateSignMode(val signMode: SignMode) : LoginAction()
    data class LoginWithToken(val loginToken: String) : LoginAction()
    data class WebLoginSuccess(val credentials: Credentials) : LoginAction()
    data class InitWith(val loginConfig: LoginConfig?) : LoginAction()
    data class ResetPassword(val email: String, val newPassword: String) : LoginAction()
    object ResetPasswordMailConfirmed : LoginAction()

    // Login or Register, depending on the signMode
    data class LoginOrRegister(val username: String, val password: String, val initialDeviceName: String) : LoginAction()

    // Register actions
    open class RegisterAction : LoginAction()

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
    open class ResetAction : LoginAction()

    object ResetHomeServerType : ResetAction()
    object ResetHomeServerUrl : ResetAction()
    object ResetSignMode : ResetAction()
    object ResetLogin : ResetAction()
    object ResetResetPassword : ResetAction()

    // Homeserver history
    object ClearHomeServerHistory : LoginAction()

    // For the soft logout case
    data class SetupSsoForSessionRecovery(val homeServerUrl: String,
                                          val deviceId: String,
                                          val ssoIdentityProviders: List<SsoIdentityProvider>?) : LoginAction()

    data class PostViewEvent(val viewEvent: LoginViewEvents) : LoginAction()

    data class UserAcceptCertificate(val fingerprint: Fingerprint) : LoginAction()
}
