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

package im.vector.app.features.settings.devices.v2.signout

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.util.awaitCallback
import javax.inject.Inject

// TODO add unit tests
class SignoutSessionUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    suspend fun execute(deviceId: String, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor): Result<Unit> {
        return deleteDevice(deviceId, userInteractiveAuthInterceptor)
    }

    private suspend fun deleteDevice(deviceId: String, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor) = runCatching {
        awaitCallback<Unit> { matrixCallback ->
            activeSessionHolder.getActiveSession()
                    .cryptoService()
                    .deleteDevice(deviceId, userInteractiveAuthInterceptor, matrixCallback)
        }
    }
}
