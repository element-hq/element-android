/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.thirdparty

import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetThirdPartyProtocolsTask : Task<Unit, Map<String, ThirdPartyProtocol>>

internal class DefaultGetThirdPartyProtocolsTask @Inject constructor(
        private val thirdPartyAPI: ThirdPartyAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetThirdPartyProtocolsTask {

    override suspend fun execute(params: Unit): Map<String, ThirdPartyProtocol> {
        return executeRequest(globalErrorReceiver) {
            thirdPartyAPI.thirdPartyProtocols()
        }
    }
}
