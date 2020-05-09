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

package im.vector.matrix.android.internal.session.identity

import im.vector.matrix.android.api.session.identity.IdentityServiceError
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.session.identity.toMedium
import im.vector.matrix.android.internal.auth.registration.SuccessResult
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.identity.db.RealmIdentityServiceStore
import im.vector.matrix.android.internal.session.identity.model.IdentityRequestOwnershipParams
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal interface IdentitySubmitTokenForBindingTask : Task<IdentitySubmitTokenForBindingTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid,
            val token: String
    )
}

internal class DefaultIdentitySubmitTokenForBindingTask @Inject constructor(
        private val identityApiProvider: IdentityApiProvider,
        private val identityServiceStore: RealmIdentityServiceStore,
        @UserId private val userId: String
) : IdentitySubmitTokenForBindingTask {

    override suspend fun execute(params: IdentitySubmitTokenForBindingTask.Params) {
        val identityAPI = getIdentityApiAndEnsureTerms(identityApiProvider, userId)
        val pendingThreePid = identityServiceStore.getPendingBinding(params.threePid) ?: throw IdentityServiceError.NoCurrentBindingError

        val tokenResponse = executeRequest<SuccessResult>(null) {
            apiCall = identityAPI.submitToken(
                    params.threePid.toMedium(),
                    IdentityRequestOwnershipParams(
                            clientSecret = pendingThreePid.clientSecret,
                            sid = pendingThreePid.sid,
                            token = params.token
                    ))
        }

        if (!tokenResponse.isSuccess()) {
            throw IdentityServiceError.BindingError
        }
    }
}
