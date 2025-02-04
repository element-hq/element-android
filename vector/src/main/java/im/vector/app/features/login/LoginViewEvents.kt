/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.auth.registration.FlowResult

/**
 * Transient events for Login.
 */
sealed class LoginViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : LoginViewEvents()
    data class Failure(val throwable: Throwable) : LoginViewEvents()

    data class RegistrationFlowResult(val flowResult: FlowResult, val isRegistrationStarted: Boolean) : LoginViewEvents()
    object OutdatedHomeserver : LoginViewEvents()

    // Navigation event

    object OpenServerSelection : LoginViewEvents()
    data class OnServerSelectionDone(val serverType: ServerType) : LoginViewEvents()
    object OnLoginFlowRetrieved : LoginViewEvents()
    data class OnSignModeSelected(val signMode: SignMode) : LoginViewEvents()
    object OnForgetPasswordClicked : LoginViewEvents()
    object OnResetPasswordSendThreePidDone : LoginViewEvents()
    object OnResetPasswordMailConfirmationSuccess : LoginViewEvents()
    object OnResetPasswordMailConfirmationSuccessDone : LoginViewEvents()

    data class OnSendEmailSuccess(val email: String) : LoginViewEvents()
    data class OnSendMsisdnSuccess(val msisdn: String) : LoginViewEvents()

    data class OnWebLoginError(val errorCode: Int, val description: String, val failingUrl: String) : LoginViewEvents()
}
