/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

class SetNewLoginAlertShownUseCase @Inject constructor(
        private val vectorPreferences: VectorPreferences,
) {

    fun execute(deviceIds: List<String>) {
        deviceIds.forEach {
            vectorPreferences.setNewLoginAlertShownForDevice(it)
        }
    }
}
