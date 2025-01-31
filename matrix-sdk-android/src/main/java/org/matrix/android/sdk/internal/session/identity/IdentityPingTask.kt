/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal interface IdentityPingTask : Task<IdentityPingTask.Params, Unit> {
    data class Params(
            val identityAuthAPI: IdentityAuthAPI
    )
}

internal class DefaultIdentityPingTask @Inject constructor() : IdentityPingTask {

    override suspend fun execute(params: IdentityPingTask.Params) {
        try {
            executeRequest(null) {
                params.identityAuthAPI.ping()
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.ServerError && throwable.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                // Check if API v1 is available
                executeRequest(null) {
                    params.identityAuthAPI.pingV1()
                }
                // API V1 is responding, but not V2 -> Outdated
                throw IdentityServiceError.OutdatedIdentityServer
            } else {
                throw throwable
            }
        }
    }
}
