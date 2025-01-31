/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.tasks

import org.matrix.android.sdk.internal.crypto.keysbackup.api.RoomKeysApi
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.UpdateKeysBackupVersionBody
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdateKeysBackupVersionTask : Task<UpdateKeysBackupVersionTask.Params, Unit> {
    data class Params(
            val version: String,
            val keysBackupVersionBody: UpdateKeysBackupVersionBody
    )
}

internal class DefaultUpdateKeysBackupVersionTask @Inject constructor(
        private val roomKeysApi: RoomKeysApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UpdateKeysBackupVersionTask {

    override suspend fun execute(params: UpdateKeysBackupVersionTask.Params) {
        return executeRequest(globalErrorReceiver) {
            roomKeysApi.updateKeysBackupVersion(params.version, params.keysBackupVersionBody)
        }
    }
}
