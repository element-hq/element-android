/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.openid

import org.matrix.android.sdk.api.session.openid.OpenIdToken
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetOpenIdTokenTask : Task<Unit, OpenIdToken>

internal class DefaultGetOpenIdTokenTask @Inject constructor(
        @UserId private val userId: String,
        private val openIdAPI: OpenIdAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetOpenIdTokenTask {

    override suspend fun execute(params: Unit): OpenIdToken {
        return executeRequest(globalErrorReceiver) {
            openIdAPI.openIdToken(userId)
        }
    }
}
