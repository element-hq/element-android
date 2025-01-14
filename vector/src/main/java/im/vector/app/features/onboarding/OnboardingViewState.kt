/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import android.net.Uri
import android.os.Parcelable
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.PersistState
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import kotlinx.parcelize.Parcelize

data class OnboardingViewState(
        val isLoading: Boolean = false,

        @PersistState
        val onboardingFlow: OnboardingFlow? = null,

        // User choices
        @PersistState
        val serverType: ServerType = ServerType.Unknown,
        @PersistState
        val useCase: FtueUseCase? = null,
        @PersistState
        val signMode: SignMode = SignMode.Unknown,
        @PersistState
        val resetState: ResetState = ResetState(),

        // For SSO session recovery
        @PersistState
        val deviceId: String? = null,

        val knownCustomHomeServersUrls: List<String> = emptyList(),
        val isForceLoginFallbackEnabled: Boolean = false,

        @PersistState
        val registrationState: RegistrationState = RegistrationState(),

        @PersistState
        val selectedHomeserver: SelectedHomeserverState = SelectedHomeserverState(),

        @PersistState
        val selectedAuthenticationState: SelectedAuthenticationState = SelectedAuthenticationState(),

        @PersistState
        val personalizationState: PersonalizationState = PersonalizationState(),
) : MavericksState

enum class OnboardingFlow {
    SignIn,
    SignUp,
    SignInSignUp
}

@Parcelize
data class SelectedHomeserverState(
        val userFacingUrl: String? = null,
        val upstreamUrl: String? = null,
        val preferredLoginMode: LoginMode = LoginMode.Unknown,
        val supportedLoginTypes: List<String> = emptyList(),
        val hasOidcCompatibilityFlow: Boolean = false,
        val isLogoutDevicesSupported: Boolean = false,
        val isLoginWithQrSupported: Boolean = false,
) : Parcelable

@Parcelize
data class PersonalizationState(
        val userId: String = "",
        val supportsChangingDisplayName: Boolean = false,
        val supportsChangingProfilePicture: Boolean = false,
        val displayName: String? = null,
        val selectedPictureUri: Uri? = null,
) : Parcelable {

    fun supportsPersonalization() = supportsChangingDisplayName || supportsChangingProfilePicture
}

@Parcelize
data class ResetState(
        val email: String? = null,
        val newPassword: String? = null,
        val supportsLogoutAllDevices: Boolean = false
) : Parcelable

@Parcelize
data class SelectedAuthenticationState(
        val description: AuthenticationDescription? = null,
) : Parcelable

@Parcelize
data class RegistrationState(
        val email: String? = null,
        val isUserNameAvailable: Boolean = false,
        val selectedMatrixId: String? = null,
) : Parcelable
