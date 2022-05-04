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

package im.vector.app.features.pin.lockscreen.configuration

/**
 * Configuration to be used by the lockscreen feature.
 */
data class LockScreenConfiguration(
        /** Which mode should the UI display, [LockScreenMode.VERIFY] or [LockScreenMode.CREATE]. */
        val mode: LockScreenMode,
        /** Length in digits of the pin code. */
        val pinCodeLength: Int,
        /** Authentication with strong methods (fingerprint, some face/iris unlock implementations) is supported. */
        val isStrongBiometricsEnabled: Boolean,
        /** Authentication with weak methods (most face/iris unlock implementations) is supported. */
        val isWeakBiometricsEnabled: Boolean,
        /** Authentication with device credentials (system lockscreen pin code, password, pattern) is supported. */
        val isDeviceCredentialUnlockEnabled: Boolean,
        /** New pin code creation needs to be inputted twice for confirmation. */
        val needsNewCodeValidation: Boolean,
        /** Biometric authentication should be started automatically when the pin code screen is displayed. Defaults to true. */
        val autoStartBiometric: Boolean = true,
        /** Display a button in the bottom-left corner of the 'pin pad'. Defaults to true. */
        val leftButtonVisible: Boolean = true,
        /** Text of the button in the bottom-left corner of the 'pin pad'. Optional. */
        val leftButtonTitle: String? = null,
        /** Title of the pin code screen. Optional. */
        val title: String? = null,
        /** Subtitle of the pin code screen. Optional. */
        val subtitle: String? = null,
        /** Title of the 'confirm pin code' screen. Optional. */
        val newCodeConfirmationTitle: String? = null,
        /** Clear the inputted pin code on error. Defaults to true. */
        val clearCodeOnError: Boolean = true,
        /** Vibrate on authentication failed. Defaults to true. */
        val vibrateOnError: Boolean = true,
        /** Animated the pin code view on authentication failed. Defaults to true. */
        val animateOnError: Boolean = true,
        /** Title for the Biometric prompt dialog. Optional. */
        val biometricTitle: String? = null,
        /** Subtitle for the Biometric prompt dialog. Optional. */
        val biometricSubtitle: String? = null,
        /** Text for the cancel button of the Biometric prompt dialog. Optional. */
        val biometricCancelButtonTitle: String? = null,
)
