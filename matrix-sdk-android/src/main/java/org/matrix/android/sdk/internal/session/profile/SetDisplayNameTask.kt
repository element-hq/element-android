/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.profile

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class SetDisplayNameTask : Task<SetDisplayNameTask.Params, Unit> {
    data class Params(
            val userId: String,
            val newDisplayName: String
    )
}

internal class DefaultSetDisplayNameTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SetDisplayNameTask() {

    override suspend fun execute(params: Params) {
        val body = SetDisplayNameBody(
                displayName = params.newDisplayName
        )
        return executeRequest(globalErrorReceiver) {
            profileAPI.setDisplayName(params.userId, body)
        }
    }
}
