/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.tasks

import org.matrix.android.sdk.internal.crypto.keysbackup.api.RoomKeysApi
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.BackupKeysResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.RoomKeysBackupData
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface StoreRoomSessionsDataTask : Task<StoreRoomSessionsDataTask.Params, BackupKeysResult> {
    data class Params(
            val roomId: String,
            val version: String,
            val roomKeysBackupData: RoomKeysBackupData
    )
}

internal class DefaultStoreRoomSessionsDataTask @Inject constructor(
        private val roomKeysApi: RoomKeysApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : StoreRoomSessionsDataTask {

    override suspend fun execute(params: StoreRoomSessionsDataTask.Params): BackupKeysResult {
        return executeRequest(globalErrorReceiver) {
            roomKeysApi.storeRoomSessionsData(
                    params.roomId,
                    params.version,
                    params.roomKeysBackupData
            )
        }
    }
}
