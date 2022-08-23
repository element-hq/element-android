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
 *
 */

package im.vector.app.features.onboarding

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.failure.Failure as SdkFailure

/**
 * Transient events for Login.
 */
sealed class OnboardingViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : OnboardingViewEvents()
    data class Failure(val throwable: Throwable) : OnboardingViewEvents()
    data class UnrecognisedCertificateFailure(val retryAction: OnboardingAction, val cause: SdkFailure.UnrecognizedCertificateFailure) : OnboardingViewEvents()

    object DisplayRegistrationFallback : OnboardingViewEvents()
    data class DisplayRegistrationStage(val stage: Stage) : OnboardingViewEvents()
    object DisplayStartRegistration : OnboardingViewEvents()
    object OutdatedHomeserver : OnboardingViewEvents()

    // Navigation event

    object OpenUseCaseSelection : OnboardingViewEvents()
    object OpenServerSelection : OnboardingViewEvents()
    object OpenCombinedRegister : OnboardingViewEvents()
    object OpenCombinedLogin : OnboardingViewEvents()
    object EditServerSelection : OnboardingViewEvents()
    data class OnServerSelectionDone(val serverType: ServerType) : OnboardingViewEvents()
    object OnLoginFlowRetrieved : OnboardingViewEvents()
    object OnHomeserverEdited : OnboardingViewEvents()
    data class OnSignModeSelected(val signMode: SignMode) : OnboardingViewEvents()
    object OnForgetPasswordClicked : OnboardingViewEvents()

    data class OnResetPasswordEmailConfirmationSent(val email: String) : OnboardingViewEvents()
    object OpenResetPasswordComplete : OnboardingViewEvents()
    object OnResetPasswordBreakerConfirmed : OnboardingViewEvents()
    object OnResetPasswordComplete : OnboardingViewEvents()

    data class OnSendEmailSuccess(val email: String, val isRestoredSession: Boolean) : OnboardingViewEvents()
    data class OnSendMsisdnSuccess(val msisdn: String) : OnboardingViewEvents()

    data class OnWebLoginError(val errorCode: Int, val description: String, val failingUrl: String) : OnboardingViewEvents()
    object OnAccountCreated : OnboardingViewEvents()
    object OnAccountSignedIn : OnboardingViewEvents()
    object OnTakeMeHome : OnboardingViewEvents()
    object OnChooseDisplayName : OnboardingViewEvents()
    object OnChooseProfilePicture : OnboardingViewEvents()
    object OnPersonalizationComplete : OnboardingViewEvents()
    object OnBack : OnboardingViewEvents()
}
