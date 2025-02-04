/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for the verification bottom sheet.
 */
sealed class VerificationBottomSheetViewEvents : VectorViewEvents {
    object Dismiss : VerificationBottomSheetViewEvents()
    object DismissAndOpenDeviceSettings : VerificationBottomSheetViewEvents()
    object AccessSecretStore : VerificationBottomSheetViewEvents()
    object ResetAll : VerificationBottomSheetViewEvents()
    object GoToSettings : VerificationBottomSheetViewEvents()
    data class ConfirmCancel(val otherUserId: String, val deviceId: String?) : VerificationBottomSheetViewEvents()
    data class ModalError(val errorMessage: CharSequence) : VerificationBottomSheetViewEvents()
    data class RequestNotFound(val transactionId: String) : VerificationBottomSheetViewEvents()
}
