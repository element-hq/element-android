/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.login

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.internal.network.ssl.Fingerprint

sealed class TchapLoginAction : VectorViewModelAction {
    data class UpdateHomeServer(val homeServerUrl: String) : TchapLoginAction()
    data class UpdateSignMode(val signMode: SignMode) : TchapLoginAction()
    data class InitWith(val loginConfig: LoginConfig?) : TchapLoginAction()
    data class ResetPassword(val email: String, val newPassword: String) : TchapLoginAction()
    object ResetPasswordMailConfirmed : TchapLoginAction()

    // Login or Register, depending on the signMode
    data class LoginOrRegister(val username: String, val password: String, val initialDeviceName: String) : TchapLoginAction()

    // Register actions
    open class RegisterAction : TchapLoginAction()

    data class AddThreePid(val threePid: RegisterThreePid) : RegisterAction()
    object SendAgainThreePid : RegisterAction()

    // TODO Confirm Email (from link in the email, open in the phone, intercepted by RiotX)
    data class ValidateThreePid(val code: String) : RegisterAction()

    data class CaptchaDone(val captchaResponse: String) : RegisterAction()
    object AcceptTerms : RegisterAction()
    object RegisterDummy : RegisterAction()

    // Reset actions
    open class ResetAction : TchapLoginAction()

    object ResetSignMode : ResetAction()
    object ResetLogin : ResetAction()
    object ResetResetPassword : ResetAction()

    // For the soft logout case
    data class SetupSsoForSessionRecovery(val homeServerUrl: String,
                                          val deviceId: String,
                                          val ssoIdentityProviders: List<SsoIdentityProvider>?) : TchapLoginAction()

    data class PostViewEvent(val viewEvent: TchapLoginViewEvents) : TchapLoginAction()
}
