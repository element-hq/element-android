/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.profile

import im.vector.matrix.android.api.session.identity.IdentityServiceError
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.identity.db.IdentityServiceStore
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal abstract class BindThreePidsTask : Task<BindThreePidsTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultBindThreePidsTask @Inject constructor(private val profileAPI: ProfileAPI,
                                                            private val identityServiceStore: IdentityServiceStore,
                                                            private val eventBus: EventBus) : BindThreePidsTask() {
    override suspend fun execute(params: Params) {
        val idServer = identityServiceStore.getIdentityServerDetails()?.identityServerUrl?.substringAfter("://") ?: throw IdentityServiceError.NoIdentityServerConfigured
        val idToken = identityServiceStore.getIdentityServerDetails()?.token ?: throw IdentityServiceError.NoIdentityServerConfigured
        val pendingThreePid = identityServiceStore.getPendingBinding(params.threePid) ?: throw IdentityServiceError.NoCurrentBindingError

        executeRequest<Unit>(eventBus) {
            apiCall = profileAPI.bindThreePid(
                    BindThreePidBody(
                            clientSecret = pendingThreePid.clientSecret,
                            idServer = idServer,
                            idAccessToken = idToken,
                            sid = pendingThreePid.sid
                    ))
        }

        // Binding is over, cleanup the store
        identityServiceStore.deletePendingBinding(params.threePid)
    }
}
