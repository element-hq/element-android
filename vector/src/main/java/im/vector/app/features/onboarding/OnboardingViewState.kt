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

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode

data class OnboardingViewState(
        val asyncLoginAction: Async<Unit> = Uninitialized,
        val asyncHomeServerLoginFlowRequest: Async<Unit> = Uninitialized,
        val asyncResetPassword: Async<Unit> = Uninitialized,
        val asyncResetMailConfirmed: Async<Unit> = Uninitialized,
        val asyncRegistration: Async<Unit> = Uninitialized,
        val asyncDisplayName: Async<Unit> = Uninitialized,

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
        val resetPasswordEmail: String? = null,
        @PersistState
        val homeServerUrlFromUser: String? = null,

        // Can be modified after a Wellknown request
        @PersistState
        val homeServerUrl: String? = null,

        // For SSO session recovery
        @PersistState
        val deviceId: String? = null,

        // Network result
        @PersistState
        val loginMode: LoginMode = LoginMode.Unknown,
        // Supported types for the login. We cannot use a sealed class for LoginType because it is not serializable
        @PersistState
        val loginModeSupportedTypes: List<String> = emptyList(),
        val knownCustomHomeServersUrls: List<String> = emptyList(),
        val isForceLoginFallbackEnabled: Boolean = false,
) : MavericksState {

    fun isLoading(): Boolean {
        return asyncLoginAction is Loading ||
                asyncHomeServerLoginFlowRequest is Loading ||
                asyncResetPassword is Loading ||
                asyncResetMailConfirmed is Loading ||
                asyncRegistration is Loading ||
                asyncDisplayName is Loading
    }

    fun isAuthTaskCompleted(): Boolean {
        return asyncLoginAction is Success
    }
}

enum class OnboardingFlow {
    SignIn,
    SignUp,
    SignInSignUp
}
