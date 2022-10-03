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

package im.vector.app.features.settings.devices.v2.details.extended

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.AppNameProvider
import im.vector.app.core.resources.BuildMeta
import javax.inject.Inject

/**
 * This use case updates if needed the account data event containing extended client info.
 */
class UpdateMatrixClientInfoUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val appNameProvider: AppNameProvider,
        private val buildMeta: BuildMeta,
        private val getMatrixClientInfoUseCase: GetMatrixClientInfoUseCase,
        private val setMatrixClientInfoUseCase: SetMatrixClientInfoUseCase,
) {

    // TODO call the use case after signin + on app startup
    suspend fun execute(): Result<Unit> = runCatching {
        val clientInfo = MatrixClientInfoContent(
                name = appNameProvider.getAppName(),
                version = buildMeta.versionName
        )
        val deviceId = activeSessionHolder.getActiveSession().sessionParams.deviceId.orEmpty()
        if (deviceId.isNotEmpty()) {
            val storedClientInfo = getMatrixClientInfoUseCase.execute(deviceId)
            if (clientInfo != storedClientInfo) {
                setMatrixClientInfoUseCase.execute(clientInfo)
            }
        } else {
            throw NoDeviceIdError()
        }
    }
}
