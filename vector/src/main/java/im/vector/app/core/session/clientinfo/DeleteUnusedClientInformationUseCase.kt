/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
