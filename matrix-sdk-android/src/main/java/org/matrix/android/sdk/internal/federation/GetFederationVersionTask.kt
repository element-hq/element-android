/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.federation

import org.matrix.android.sdk.api.federation.FederationVersion
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetFederationVersionTask : Task<Unit, FederationVersion>

internal class DefaultGetFederationVersionTask @Inject constructor(
        private val federationAPI: FederationAPI
) : GetFederationVersionTask {

    override suspend fun execute(params: Unit): FederationVersion {
        val result = executeRequest(null) {
            federationAPI.getVersion()
        }

        return FederationVersion(
                name = result.server?.name,
                version = result.server?.version
        )
    }
}
