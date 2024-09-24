/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

class ToggleIpAddressVisibilityUseCase @Inject constructor(
        private val vectorPreferences: VectorPreferences,
) {

    fun execute() {
        val currentVisibility = vectorPreferences.showIpAddressInSessionManagerScreens()
        vectorPreferences.setIpAddressVisibilityInDeviceManagerScreens(!currentVisibility)
    }
}
