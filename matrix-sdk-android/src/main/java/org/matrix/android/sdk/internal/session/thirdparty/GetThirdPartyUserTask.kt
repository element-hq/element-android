/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.thirdparty

import org.matrix.android.sdk.api.session.thirdparty.model.ThirdPartyUser
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetThirdPartyUserTask : Task<GetThirdPartyUserTask.Params, List<ThirdPartyUser>> {

    data class Params(
            val protocol: String,
            val fields: Map<String, String> = emptyMap()
    )
}

internal class DefaultGetThirdPartyUserTask @Inject constructor(
        private val thirdPartyAPI: ThirdPartyAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetThirdPartyUserTask {

    override suspend fun execute(params: GetThirdPartyUserTask.Params): List<ThirdPartyUser> {
        return executeRequest(globalErrorReceiver) {
            thirdPartyAPI.getThirdPartyUser(params.protocol, params.fields)
        }
    }
}
