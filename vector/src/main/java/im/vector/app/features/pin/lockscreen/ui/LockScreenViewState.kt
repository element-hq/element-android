/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
