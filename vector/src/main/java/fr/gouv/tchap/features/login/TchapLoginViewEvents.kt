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
 *
 */

package fr.gouv.tchap.features.login

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.login.SignMode
import org.matrix.android.sdk.api.auth.registration.FlowResult

/**
 * Transient events for Login
 */
sealed class TchapLoginViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : TchapLoginViewEvents()
    data class Failure(val throwable: Throwable) : TchapLoginViewEvents()

    data class RegistrationFlowResult(val flowResult: FlowResult, val isRegistrationStarted: Boolean) : TchapLoginViewEvents()
    object OutdatedHomeserver : TchapLoginViewEvents()

    // Navigation event

    object OnLoginFlowRetrieved : TchapLoginViewEvents()
    data class OnSignModeSelected(val signMode: SignMode) : TchapLoginViewEvents()
    object OnForgetPasswordClicked : TchapLoginViewEvents()

    data class OnSendEmailSuccess(val email: String) : TchapLoginViewEvents()
    object OnGoToSignInClicked : TchapLoginViewEvents()
}
