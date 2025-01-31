/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.location

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface CheckIfExistingActiveLiveTask : Task<CheckIfExistingActiveLiveTask.Params, Boolean> {
    data class Params(
            val roomId: String,
    )
}

internal class DefaultCheckIfExistingActiveLiveTask @Inject constructor(
        private val getActiveBeaconInfoForUserTask: GetActiveBeaconInfoForUserTask,
) : CheckIfExistingActiveLiveTask {

    override suspend fun execute(params: CheckIfExistingActiveLiveTask.Params): Boolean {
        val getActiveBeaconTaskParams = GetActiveBeaconInfoForUserTask.Params(
                roomId = params.roomId
        )
        return getActiveBeaconInfoForUserTask.execute(getActiveBeaconTaskParams)
                ?.getClearContent()
                ?.toModel<MessageBeaconInfoContent>()
                ?.isLive
                .orFalse()
    }
}
