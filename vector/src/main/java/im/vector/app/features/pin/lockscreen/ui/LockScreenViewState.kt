/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration

data class LockScreenViewState(
        val lockScreenConfiguration: LockScreenConfiguration,
        val canUseBiometricAuth: Boolean,
        val showBiometricPromptAutomatically: Boolean,
        val pinCodeState: PinCodeState,
        val isBiometricKeyInvalidated: Boolean,
) : MavericksState {
    constructor(lockScreenConfiguration: LockScreenConfiguration) : this(
            lockScreenConfiguration, false, false, PinCodeState.Idle, false,
    )
}

sealed class PinCodeState {
    object Idle : PinCodeState()
    data class FirstCodeEntered(val pinCode: String) : PinCodeState()
}
