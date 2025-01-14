/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import android.net.Uri
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.network.ssl.Fingerprint

sealed interface OnboardingAction : VectorViewModelAction {
    sealed interface SplashAction : OnboardingAction {
        val onboardingFlow: OnboardingFlow

        data class OnGetStarted(override val onboardingFlow: OnboardingFlow) : SplashAction
        data class OnIAlreadyHaveAnAccount(override val onboardingFlow: OnboardingFlow) : SplashAction
    }

    data class UpdateServerType(val serverType: ServerType) : OnboardingAction

    sealed interface HomeServerChange : OnboardingAction {
        val homeServerUrl: String

        data class SelectHomeServer(override val homeServerUrl: String) : HomeServerChange
        data class EditHomeServer(override val homeServerUrl: String) : HomeServerChange
    }

    data class UpdateUseCase(val useCase: FtueUseCase) : OnboardingAction
    object ResetUseCase : OnboardingAction
    data class UpdateSignMode(val signMode: SignMode) : OnboardingAction
    data class LoginWithToken(val loginToken: String) : OnboardingAction
    data class WebLoginSuccess(val credentials: Credentials) : OnboardingAction
    data class InitWith(val loginConfig: LoginConfig?) : OnboardingAction
    data class ResetPassword(val email: String, val newPassword: String?) : OnboardingAction
    data class ConfirmNewPassword(val newPassword: String, val signOutAllDevices: Boolean) : OnboardingAction
    object ResendResetPassword : OnboardingAction
    object ResetPasswordMailConfirmed : OnboardingAction

    sealed interface UserNameEnteredAction : OnboardingAction {
        data class Registration(val userId: String) : UserNameEnteredAction
        data class Login(val userId: String) : UserNameEnteredAction
    }
    sealed interface AuthenticateAction : OnboardingAction {
        data class Register(val username: String, val password: String, val initialDeviceName: String) : AuthenticateAction
        data class RegisterWithMatrixId(val matrixId: String, val password: String, val initialDeviceName: String) : AuthenticateAction
        data class Login(val username: String, val password: String, val initialDeviceName: String) : AuthenticateAction
        data class LoginDirect(val matrixId: String, val password: String, val initialDeviceName: String) : AuthenticateAction
    }

    object StopEmailValidationCheck : OnboardingAction

    data class PostRegisterAction(val registerAction: RegisterAction) : OnboardingAction

    // Reset actions
    sealed interface ResetAction : OnboardingAction
    object ResetDeeplinkConfig : ResetAction
    object ResetHomeServerType : ResetAction
    object ResetHomeServerUrl : ResetAction
    object ResetSignMode : ResetAction
    object ResetAuthenticationAttempt : ResetAction
    object ResetResetPassword : ResetAction
    object ResetSelectedRegistrationUserName : ResetAction

    // Homeserver history
    object ClearHomeServerHistory : OnboardingAction

    data class PostViewEvent(val viewEvent: OnboardingViewEvents) : OnboardingAction

    data class UserAcceptCertificate(val fingerprint: Fingerprint, val retryAction: OnboardingAction) : OnboardingAction

    object PersonalizeProfile : OnboardingAction
    data class UpdateDisplayName(val displayName: String) : OnboardingAction
    object UpdateDisplayNameSkipped : OnboardingAction
    data class ProfilePictureSelected(val uri: Uri) : OnboardingAction
    object SaveSelectedProfilePicture : OnboardingAction
    object UpdateProfilePictureSkipped : OnboardingAction
}
