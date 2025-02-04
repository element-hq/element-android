/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.rename

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.andThen
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import javax.inject.Inject

class RenameSessionUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val refreshDevicesUseCase: RefreshDevicesUseCase,
) {

    suspend fun execute(deviceId: String, newName: String): Result<Unit> {
        return renameDevice(deviceId, newName)
                .andThen { refreshDevices() }
    }

    private suspend fun renameDevice(deviceId: String, newName: String) = runCatching {
            activeSessionHolder.getActiveSession()
                    .cryptoService()
                    .setDeviceName(deviceId, newName)
    }

    private suspend fun refreshDevices() = runCatching { refreshDevicesUseCase.execute() }
}
