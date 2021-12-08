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

package im.vector.app.features.ftue

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import org.matrix.android.sdk.api.auth.registration.FlowResult

/**
 * Transient events for Login
 */
sealed class FTUEViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : FTUEViewEvents()
    data class Failure(val throwable: Throwable) : FTUEViewEvents()

    data class RegistrationFlowResult(val flowResult: FlowResult, val isRegistrationStarted: Boolean) : FTUEViewEvents()
    object OutdatedHomeserver : FTUEViewEvents()

    // Navigation event

    object OpenServerSelection : FTUEViewEvents()
    data class OnServerSelectionDone(val serverType: ServerType) : FTUEViewEvents()
    object OnLoginFlowRetrieved : FTUEViewEvents()
    data class OnSignModeSelected(val signMode: SignMode) : FTUEViewEvents()
    object OnForgetPasswordClicked : FTUEViewEvents()
    object OnResetPasswordSendThreePidDone : FTUEViewEvents()
    object OnResetPasswordMailConfirmationSuccess : FTUEViewEvents()
    object OnResetPasswordMailConfirmationSuccessDone : FTUEViewEvents()

    data class OnSendEmailSuccess(val email: String) : FTUEViewEvents()
    data class OnSendMsisdnSuccess(val msisdn: String) : FTUEViewEvents()

    data class OnWebLoginError(val errorCode: Int, val description: String, val failingUrl: String) : FTUEViewEvents()
}
