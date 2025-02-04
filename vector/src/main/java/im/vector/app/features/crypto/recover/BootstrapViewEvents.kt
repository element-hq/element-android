/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse

sealed class BootstrapViewEvents : VectorViewEvents {
    data class Dismiss(val success: Boolean) : BootstrapViewEvents()
    data class ModalError(val error: String) : BootstrapViewEvents()
    object RecoveryKeySaved : BootstrapViewEvents()
    data class SkipBootstrap(val genKeyOption: Boolean = true) : BootstrapViewEvents()
    data class RequestReAuth(val flowResponse: RegistrationFlowResponse, val lastErrorCode: String?) : BootstrapViewEvents()
}
