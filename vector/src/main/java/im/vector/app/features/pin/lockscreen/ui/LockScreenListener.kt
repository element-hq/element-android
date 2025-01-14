/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui

/**
 * Listener class to be notified of any event that could happen in the lock screen UI.
 */
interface LockScreenListener {
    /**
     * In PIN creation mode, called when the first PIN code has been entered.
     */
    fun onFirstCodeEntered() = Unit

    /**
     * In PIN creation mode, called when the confirmation PIN code doesn't match the first one.
     */
    fun onNewCodeValidationFailed() = Unit

    /**
     * In PIN creation mode, called when the PIN code was successfully set up.
     */
    fun onPinCodeCreated() = Unit

    /**
     * In verification mode, called when the authentication succeeded.
     * @param authMethod Authentication method used ([AuthMethod.PIN_CODE] or [AuthMethod.BIOMETRICS]).
     */
    fun onAuthenticationSuccess(authMethod: AuthMethod) = Unit

    /**
     * In verification mode, called when the authentication failed. At this point the user can usually still retry the authentication.
     * @param authMethod Authentication method used ([AuthMethod.PIN_CODE] or [AuthMethod.BIOMETRICS]).
     */
    fun onAuthenticationFailure(authMethod: AuthMethod) = Unit

    /**
     * In verification mode, called when the authentication had a fatal error and can't continue. This is not an authentication failure.
     * @param authMethod Authentication method used ([AuthMethod.PIN_CODE] or [AuthMethod.BIOMETRICS]).
     * @param throwable The error thrown when the authentication flow was interrupted.
     */
    fun onAuthenticationError(authMethod: AuthMethod, throwable: Throwable) = Unit

    /**
     * In verification mode, called when the system authentication key (used for biometrics) has been invalidated and cannot be used anymore.
     */
    fun onBiometricKeyInvalidated() = Unit
}

/**
 * Enum containing the available authentication methods.
 */
enum class AuthMethod {
    PIN_CODE,
    BIOMETRICS
}
