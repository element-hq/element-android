/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.account.deactivation

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse

/**
 * Transient events for deactivate account settings screen.
 */
sealed class DeactivateAccountViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : DeactivateAccountViewEvents()
    object InvalidAuth : DeactivateAccountViewEvents()
    data class OtherFailure(val throwable: Throwable) : DeactivateAccountViewEvents()
    object Done : DeactivateAccountViewEvents()
    data class RequestReAuth(val registrationFlowResponse: RegistrationFlowResponse, val lastErrorCode: String?) : DeactivateAccountViewEvents()
}
