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

import im.vector.matrix.android.api.session.identity.FoundThreePid
import im.vector.matrix.android.api.session.identity.IdentityServiceError
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments.base64ToBase64Url
import im.vector.matrix.android.internal.crypto.tools.withOlmUtility
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.identity.db.IdentityServiceStore
import im.vector.matrix.android.internal.session.identity.model.IdentityHashDetailResponse
import im.vector.matrix.android.internal.session.identity.model.IdentityLookUpV2Params
import im.vector.matrix.android.internal.session.identity.model.IdentityLookUpV2Response
import im.vector.matrix.android.internal.task.Task
import java.util.Locale
import javax.inject.Inject

internal interface BulkLookupTask : Task<BulkLookupTask.Params, List<FoundThreePid>> {
    data class Params(
            val threePids: List<ThreePid>
    )
}

internal class DefaultBulkLookupTask @Inject constructor(
        private val identityApiProvider: IdentityApiProvider,
        private val identityServiceStore: IdentityServiceStore
) : BulkLookupTask {

    override suspend fun execute(params: BulkLookupTask.Params): List<FoundThreePid> {
        val identityAPI = identityApiProvider.identityApi ?: throw IdentityServiceError.NoIdentityServerConfigured
        val entity = identityServiceStore.get() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val pepper = entity.hashLookupPepper
        val hashDetailResponse = if (pepper == null) {
            // We need to fetch the hash details first
            executeRequest<IdentityHashDetailResponse>(null) {
                apiCall = identityAPI.hashDetails()
            }
                    .also { identityServiceStore.setHashDetails(it) }
        } else {
            IdentityHashDetailResponse(pepper, entity.hashLookupAlgorithm.toList())
        }

        if (hashDetailResponse.algorithms.contains("sha256").not()) {
            // TODO We should ask the user if he is ok to send their 3Pid in clear, but for the moment do not do it
            throw IdentityServiceError.BulkLookupSha256NotSupported
        }

        val hashedAddresses = withOlmUtility { olmUtility ->
            params.threePids.map { threePid ->
                base64ToBase64Url(
                        olmUtility.sha256(threePid.value.toLowerCase(Locale.ROOT)
                                + " " + threePid.toMedium() + " " + hashDetailResponse.pepper)
                )
            }
        }

        val identityLookUpV2Response = executeRequest<IdentityLookUpV2Response>(null) {
            apiCall = identityAPI.bulkLookupV2(IdentityLookUpV2Params(
                    hashedAddresses,
                    "sha256",
                    hashDetailResponse.pepper
            ))
        }

        // TODO Catch invalid hash pepper and retry

        // Convert back to List<FoundThreePid>
        return handleSuccess(params.threePids, hashedAddresses, identityLookUpV2Response)
    }

    private fun handleSuccess(threePids: List<ThreePid>, hashedAddresses: List<String>, identityLookUpV2Response: IdentityLookUpV2Response): List<FoundThreePid> {
        return identityLookUpV2Response.mappings.keys.map { hashedAddress ->
            FoundThreePid(threePids[hashedAddresses.indexOf(hashedAddress)], identityLookUpV2Response.mappings[hashedAddress] ?: error(""))
        }
    }

    private fun ThreePid.toMedium(): String {
        return when (this) {
            is ThreePid.Email  -> "email"
            is ThreePid.Msisdn -> "msisdn"
        }
    }
}
