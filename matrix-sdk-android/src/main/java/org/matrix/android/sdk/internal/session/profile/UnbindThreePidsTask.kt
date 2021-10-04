/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

internal class DefaultUnbindThreePidsTask @Inject constructor(private val profileAPI: ProfileAPI,
                                                              private val identityStore: IdentityStore,
                                                              private val globalErrorReceiver: GlobalErrorReceiver) : UnbindThreePidsTask() {
    override suspend fun execute(params: Params): Boolean {
        val identityServerUrlWithoutProtocol = identityStore.getIdentityServerUrlWithoutProtocol()
                ?: throw IdentityServiceError.NoIdentityServerConfigured

        return executeRequest(globalErrorReceiver) {
            profileAPI.unbindThreePid(
                    UnbindThreePidBody(
                            identityServerUrlWithoutProtocol,
                            params.threePid.toMedium(),
                            params.threePid.value
                    ))
        }.isSuccess()
    }
}
