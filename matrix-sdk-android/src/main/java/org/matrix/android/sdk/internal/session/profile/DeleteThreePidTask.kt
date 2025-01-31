/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.profile

import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class DeleteThreePidTask : Task<DeleteThreePidTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultDeleteThreePidTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : DeleteThreePidTask() {

    override suspend fun execute(params: Params) {
        val body = DeleteThreePidBody(
                medium = params.threePid.toMedium(),
                address = params.threePid.value
        )
        executeRequest(globalErrorReceiver) {
            profileAPI.deleteThreePid(body)
        }

        // We do not really care about the result for the moment
    }
}
