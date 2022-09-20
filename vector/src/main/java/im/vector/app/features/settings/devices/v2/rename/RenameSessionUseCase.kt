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

package im.vector.app.features.settings.devices.v2.rename

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.andThen
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import org.matrix.android.sdk.api.util.awaitCallback
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
        awaitCallback<Unit> { matrixCallback ->
            activeSessionHolder.getActiveSession()
                    .cryptoService()
                    .setDeviceName(deviceId, newName, matrixCallback)
        }
    }

    private fun refreshDevices() = runCatching { refreshDevicesUseCase.execute() }
}
