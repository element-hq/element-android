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
import org.matrix.android.sdk.api.auth.registration.FlowResult

/**
 * Transient events for Login
 */
sealed class OnboardingViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : OnboardingViewEvents()
    data class Failure(val throwable: Throwable) : OnboardingViewEvents()

    data class RegistrationFlowResult(val flowResult: FlowResult, val isRegistrationStarted: Boolean) : OnboardingViewEvents()
    object OutdatedHomeserver : OnboardingViewEvents()

    // Navigation event

    object OpenUseCaseSelection : OnboardingViewEvents()
    object OpenServerSelection : OnboardingViewEvents()
    object OpenCombinedRegister : OnboardingViewEvents()
    object EditServerSelection : OnboardingViewEvents()
    data class OnServerSelectionDone(val serverType: ServerType) : OnboardingViewEvents()
    object OnLoginFlowRetrieved : OnboardingViewEvents()
    object OnHomeserverEdited : OnboardingViewEvents()
    data class OnSignModeSelected(val signMode: SignMode) : OnboardingViewEvents()
    object OnForgetPasswordClicked : OnboardingViewEvents()
    object OnResetPasswordSendThreePidDone : OnboardingViewEvents()
    object OnResetPasswordMailConfirmationSuccess : OnboardingViewEvents()
    object OnResetPasswordMailConfirmationSuccessDone : OnboardingViewEvents()

    data class OnSendEmailSuccess(val email: String) : OnboardingViewEvents()
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
