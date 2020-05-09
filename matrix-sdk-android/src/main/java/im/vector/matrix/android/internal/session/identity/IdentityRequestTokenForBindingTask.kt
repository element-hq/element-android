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
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.identity.db.RealmIdentityServiceStore
import im.vector.matrix.android.internal.session.identity.model.IdentityRequestTokenForEmailBody
import im.vector.matrix.android.internal.session.identity.model.IdentityRequestTokenForMsisdnBody
import im.vector.matrix.android.internal.session.identity.model.IdentityRequestTokenResponse
import im.vector.matrix.android.internal.task.Task
import java.util.UUID
import javax.inject.Inject

internal interface IdentityRequestTokenForBindingTask : Task<IdentityRequestTokenForBindingTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultIdentityRequestTokenForBindingTask @Inject constructor(
        private val identityApiProvider: IdentityApiProvider,
        private val identityServiceStore: RealmIdentityServiceStore,
        @UserId private val userId: String
) : IdentityRequestTokenForBindingTask {

    override suspend fun execute(params: IdentityRequestTokenForBindingTask.Params) {
        val identityAPI = getIdentityApiAndEnsureTerms(identityApiProvider, userId)

        val clientSecret = UUID.randomUUID().toString()

        val tokenResponse = executeRequest<IdentityRequestTokenResponse>(null) {
            apiCall = when (params.threePid) {
                is ThreePid.Email  -> identityAPI.requestTokenToBindEmail(IdentityRequestTokenForEmailBody(
                        clientSecret = clientSecret,
                        sendAttempt = 1,
                        email = params.threePid.email
                ))
                is ThreePid.Msisdn -> identityAPI.requestTokenToBindMsisdn(IdentityRequestTokenForMsisdnBody(
                        clientSecret = clientSecret,
                        sendAttempt = 1,
                        phoneNumber = params.threePid.msisdn,
                        countryCode = params.threePid.countryCode
                ))
            }
        }

        if (!tokenResponse.success) {
            throw IdentityServiceError.BindingError
        }

        // Store client secret and sid
        identityServiceStore.storePendingBinding(
                params.threePid,
                clientSecret,
                tokenResponse.sid)
    }
}
