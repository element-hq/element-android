/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.tasks

import org.matrix.android.sdk.internal.crypto.keysbackup.api.RoomKeysApi
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface DeleteBackupTask : Task<DeleteBackupTask.Params, Unit> {
    data class Params(
            val version: String
    )
}

internal class DefaultDeleteBackupTask @Inject constructor(
        private val roomKeysApi: RoomKeysApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : DeleteBackupTask {

    override suspend fun execute(params: DeleteBackupTask.Params) {
        return executeRequest(globalErrorReceiver) {
            roomKeysApi.deleteBackup(params.version)
        }
    }
}
