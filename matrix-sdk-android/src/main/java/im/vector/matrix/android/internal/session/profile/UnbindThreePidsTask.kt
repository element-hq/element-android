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
import im.vector.matrix.android.api.session.identity.toMedium
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.identity.data.IdentityStore
import im.vector.matrix.android.internal.session.identity.data.getIdentityServerUrlWithoutProtocol
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal abstract class UnbindThreePidsTask : Task<UnbindThreePidsTask.Params, Boolean> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultUnbindThreePidsTask @Inject constructor(private val profileAPI: ProfileAPI,
                                                              private val identityStore: IdentityStore,
                                                              private val eventBus: EventBus) : UnbindThreePidsTask() {
    override suspend fun execute(params: Params): Boolean {
        val identityServerUrlWithoutProtocol = identityStore.getIdentityServerUrlWithoutProtocol()
                ?: throw IdentityServiceError.NoIdentityServerConfigured

        return executeRequest<UnbindThreePidResponse>(eventBus) {
            apiCall = profileAPI.unbindThreePid(
                    UnbindThreePidBody(
                            identityServerUrlWithoutProtocol,
                            params.threePid.toMedium(),
                            params.threePid.value
                    ))
        }.isSuccess()
    }
}
