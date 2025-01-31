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
import org.matrix.android.sdk.internal.di.AuthenticatedIdentity
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.token.AccessTokenProvider
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.identity.data.getIdentityServerUrlWithoutProtocol
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class BindThreePidsTask : Task<BindThreePidsTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultBindThreePidsTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        private val identityStore: IdentityStore,
        @AuthenticatedIdentity private val accessTokenProvider: AccessTokenProvider,
        private val globalErrorReceiver: GlobalErrorReceiver
) : BindThreePidsTask() {
    override suspend fun execute(params: Params) {
        val identityServerUrlWithoutProtocol = identityStore.getIdentityServerUrlWithoutProtocol() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val identityServerAccessToken = accessTokenProvider.getToken() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val identityPendingBinding = identityStore.getPendingBinding(params.threePid) ?: throw IdentityServiceError.NoCurrentBindingError

        executeRequest(globalErrorReceiver) {
            profileAPI.bindThreePid(
                    BindThreePidBody(
                            clientSecret = identityPendingBinding.clientSecret,
                            identityServerUrlWithoutProtocol = identityServerUrlWithoutProtocol,
                            identityServerAccessToken = identityServerAccessToken,
                            sid = identityPendingBinding.sid
                    )
            )
        }

        // Binding is over, cleanup the store
        identityStore.deletePendingBinding(params.threePid)
    }
}
