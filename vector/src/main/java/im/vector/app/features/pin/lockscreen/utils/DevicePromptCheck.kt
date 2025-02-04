/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.utils

import android.os.Build
import androidx.biometric.BiometricPrompt

/**
 * Helper to detect devices with [BiometricPrompt] issues.
 * The device lists are taken from [this repository](https://github.com/sergeykomlach/AdvancedBiometricPromptCompat/), in DevicesWithKnownBugs.kt.
 */
object DevicePromptCheck {

    private val onePlusModelsWithWorkingBiometricUI = setOf(
            "A0001", // OnePlus One
            "ONE A2001", "ONE A2003", "ONE A2005", // OnePlus 2
            "ONE E1001", "ONE E1003", "ONE E1005", // OnePlus X
            "ONEPLUS A3000", "ONEPLUS SM-A3000", "ONEPLUS A3003", // OnePlus 3
            "ONEPLUS A3010", // OnePlus 3T
            "ONEPLUS A5000", // OnePlus 5
            "ONEPLUS A5010", // OnePlus 5T
            "ONEPLUS A6000", "ONEPLUS A6003", // OnePlus 6
    )

    private val lgModelsWithoutBiometricUI = setOf(
            "G810", // G8 ThinQ "G820", G8S ThinQ
            "G850", // G8X ThinQ
            "G900", // Velvet/Velvet 5G
            "G910", // Velvet 4G Dual Sim
    )

    /**
     * New OnePlus devices have a bug that prevents the system biometric UI from appearing, only the under display fingerprint is shown.
     * See [this OP forum thread](https://forums.oneplus.com/threads/oneplus-7-pro-fingerprint-biometricprompt-does-not-show.1035821/).
     */
    private val isOnePlusDeviceWithNoBiometricUI: Boolean =
            Build.BRAND.equals("OnePlus", ignoreCase = true) &&
                    !onePlusModelsWithWorkingBiometricUI.contains(Build.MODEL) &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    /**
     * Some LG models don't seem to have a system biometric prompt at all.
     */
    private val isLGDeviceWithNoBiometricUI: Boolean =
            Build.BRAND.equals("LG", ignoreCase = true) && lgModelsWithoutBiometricUI.contains(Build.MODEL)

    /**
     * Check if this device is included in the list of devices with known Biometric Prompt issues.
     */
    val isDeviceWithNoBiometricUI: Boolean = isOnePlusDeviceWithNoBiometricUI || isLGDeviceWithNoBiometricUI
}
