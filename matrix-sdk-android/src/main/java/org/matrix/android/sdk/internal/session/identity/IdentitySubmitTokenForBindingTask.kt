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

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.identity.model.IdentityRequestOwnershipParams
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface IdentitySubmitTokenForBindingTask : Task<IdentitySubmitTokenForBindingTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid,
            val token: String
    )
}

internal class DefaultIdentitySubmitTokenForBindingTask @Inject constructor(
        private val identityApiProvider: IdentityApiProvider,
        private val identityStore: IdentityStore,
        @UserId private val userId: String
) : IdentitySubmitTokenForBindingTask {

    override suspend fun execute(params: IdentitySubmitTokenForBindingTask.Params) {
        val identityAPI = getIdentityApiAndEnsureTerms(identityApiProvider, userId)
        val identityPendingBinding = identityStore.getPendingBinding(params.threePid) ?: throw IdentityServiceError.NoCurrentBindingError

        val tokenResponse = executeRequest(null) {
            identityAPI.submitToken(
                    params.threePid.toMedium(),
                    IdentityRequestOwnershipParams(
                            clientSecret = identityPendingBinding.clientSecret,
                            sid = identityPendingBinding.sid,
                            token = params.token
                    ))
        }

        if (!tokenResponse.isSuccess()) {
            throw IdentityServiceError.BindingError
        }
    }
}
