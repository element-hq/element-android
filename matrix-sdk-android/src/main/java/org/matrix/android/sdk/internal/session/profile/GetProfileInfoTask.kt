/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.profile

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class GetProfileInfoTask : Task<GetProfileInfoTask.Params, JsonDict> {
    data class Params(
            val userId: String
    )
}

internal class DefaultGetProfileInfoTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetProfileInfoTask() {

    override suspend fun execute(params: Params): JsonDict {
        return executeRequest(globalErrorReceiver) {
            profileAPI.getProfile(params.userId)
        }
    }
}
