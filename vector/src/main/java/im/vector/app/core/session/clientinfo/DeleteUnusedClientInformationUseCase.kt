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

package im.vector.app.core.session.clientinfo

import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import javax.inject.Inject

class DeleteUnusedClientInformationUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    suspend fun execute(deviceInfoList: List<DeviceInfo>): Result<Unit> = runCatching {
        // A defensive approach against local storage reports an empty device list (although it is not a seen situation).
        if (deviceInfoList.isEmpty()) return Result.success(Unit)
        val dispatcher = activeSessionHolder.getSafeActiveSession()?.coroutineDispatchers?.io
                ?: return@runCatching
        withContext(dispatcher) {
            val expectedClientInfoKeyList = deviceInfoList.map { MATRIX_CLIENT_INFO_KEY_PREFIX + it.deviceId }
            activeSessionHolder
                    .getSafeActiveSession()
                    ?.accountDataService()
                    ?.getUserAccountDataEventsStartWith(MATRIX_CLIENT_INFO_KEY_PREFIX)
                    ?.map { it.type }
                    ?.subtract(expectedClientInfoKeyList.toSet())
                    ?.forEach { userAccountDataKeyToDelete ->
                        activeSessionHolder.getSafeActiveSession()?.accountDataService()?.deleteUserAccountData(userAccountDataKeyToDelete)
                    }
        }
    }
}
