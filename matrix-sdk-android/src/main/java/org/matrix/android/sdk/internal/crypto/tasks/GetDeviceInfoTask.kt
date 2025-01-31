/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetDeviceInfoTask : Task<GetDeviceInfoTask.Params, DeviceInfo> {
    data class Params(val deviceId: String)
}

internal class DefaultGetDeviceInfoTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetDeviceInfoTask {

    override suspend fun execute(params: GetDeviceInfoTask.Params): DeviceInfo {
        return executeRequest(globalErrorReceiver) {
            cryptoApi.getDeviceInfo(params.deviceId)
        }
    }
}
