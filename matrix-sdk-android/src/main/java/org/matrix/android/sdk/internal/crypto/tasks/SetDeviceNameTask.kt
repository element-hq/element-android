/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.UpdateDeviceInfoBody
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface SetDeviceNameTask : Task<SetDeviceNameTask.Params, Unit> {
    data class Params(
            // the device id
            val deviceId: String,
            // the device name
            val deviceName: String
    )
}

internal class DefaultSetDeviceNameTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SetDeviceNameTask {

    override suspend fun execute(params: SetDeviceNameTask.Params) {
        val body = UpdateDeviceInfoBody(
                displayName = params.deviceName
        )
        return executeRequest(globalErrorReceiver) {
            cryptoApi.updateDeviceInfo(params.deviceId, body)
        }
    }
}
