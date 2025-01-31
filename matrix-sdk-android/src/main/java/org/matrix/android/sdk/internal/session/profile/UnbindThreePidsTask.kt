/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.profile

import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.identity.data.getIdentityServerUrlWithoutProtocol
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class UnbindThreePidsTask : Task<UnbindThreePidsTask.Params, Boolean> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultUnbindThreePidsTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        private val identityStore: IdentityStore,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UnbindThreePidsTask() {
    override suspend fun execute(params: Params): Boolean {
        val identityServerUrlWithoutProtocol = identityStore.getIdentityServerUrlWithoutProtocol()
                ?: throw IdentityServiceError.NoIdentityServerConfigured

        return executeRequest(globalErrorReceiver) {
            profileAPI.unbindThreePid(
                    UnbindThreePidBody(
                            identityServerUrlWithoutProtocol,
                            params.threePid.toMedium(),
                            params.threePid.value
                    )
            )
        }.isSuccess()
    }
}
