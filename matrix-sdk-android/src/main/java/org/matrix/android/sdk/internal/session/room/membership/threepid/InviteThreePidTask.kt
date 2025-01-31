/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.membership.threepid

import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium
import org.matrix.android.sdk.internal.di.AuthenticatedIdentity
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.token.AccessTokenProvider
import org.matrix.android.sdk.internal.session.identity.EnsureIdentityTokenTask
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.identity.data.getIdentityServerUrlWithoutProtocol
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface InviteThreePidTask : Task<InviteThreePidTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val threePid: ThreePid
    )
}

internal class DefaultInviteThreePidTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val identityStore: IdentityStore,
        private val ensureIdentityTokenTask: EnsureIdentityTokenTask,
        @AuthenticatedIdentity
        private val accessTokenProvider: AccessTokenProvider
) : InviteThreePidTask {

    override suspend fun execute(params: InviteThreePidTask.Params) {
        ensureIdentityTokenTask.execute(Unit)

        val identityServerUrlWithoutProtocol = identityStore.getIdentityServerUrlWithoutProtocol() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val identityServerAccessToken = accessTokenProvider.getToken() ?: throw IdentityServiceError.NoIdentityServerConfigured

        return executeRequest(globalErrorReceiver) {
            val body = ThreePidInviteBody(
                    idServer = identityServerUrlWithoutProtocol,
                    idAccessToken = identityServerAccessToken,
                    medium = params.threePid.toMedium(),
                    address = params.threePid.value
            )
            roomAPI.invite3pid(params.roomId, body)
        }
    }
}
