/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui

import im.vector.app.core.platform.VectorViewEvents

sealed class LockScreenViewEvent : VectorViewEvents {
    data class ClearPinCode(val confirmationFailed: Boolean) : LockScreenViewEvent()
    object CodeCreationComplete : LockScreenViewEvent()
    data class AuthSuccessful(val method: AuthMethod) : LockScreenViewEvent()
    data class AuthFailure(val method: AuthMethod) : LockScreenViewEvent()
    data class AuthError(val method: AuthMethod, val throwable: Throwable) : LockScreenViewEvent()
    object ShowBiometricKeyInvalidatedMessage : LockScreenViewEvent()
    object ShowBiometricPromptAutomatically : LockScreenViewEvent()
}
