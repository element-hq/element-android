/*
 * Copyright 2019 New Vector Ltd
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

        val canLoginWithQrCode: Boolean = false,
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
