/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.membership.threepid

import im.vector.matrix.android.api.session.identity.IdentityServiceError
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.session.identity.toMedium
import im.vector.matrix.android.internal.di.AuthenticatedIdentity
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.network.token.AccessTokenProvider
import im.vector.matrix.android.internal.session.identity.EnsureIdentityTokenTask
import im.vector.matrix.android.internal.session.identity.data.IdentityStore
import im.vector.matrix.android.internal.session.identity.data.getIdentityServerUrlWithoutProtocol
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface InviteThreePidTask : Task<InviteThreePidTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val threePid: ThreePid
    )
}

internal class DefaultInviteThreePidTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val eventBus: EventBus,
        private val identityStore: IdentityStore,
        private val ensureIdentityTokenTask: EnsureIdentityTokenTask,
        @AuthenticatedIdentity
        private val accessTokenProvider: AccessTokenProvider
) : InviteThreePidTask {

    override suspend fun execute(params: InviteThreePidTask.Params) {
        ensureIdentityTokenTask.execute(Unit)

        val identityServerUrlWithoutProtocol = identityStore.getIdentityServerUrlWithoutProtocol() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val identityServerAccessToken = accessTokenProvider.getToken() ?: throw IdentityServiceError.NoIdentityServerConfigured

        return executeRequest(eventBus) {
            val body = ThreePidInviteBody(
                    id_server = identityServerUrlWithoutProtocol,
                    id_access_token = identityServerAccessToken,
                    medium = params.threePid.toMedium(),
                    address = params.threePid.value
            )
            apiCall = roomAPI.invite3pid(params.roomId, body)
        }
    }
}
