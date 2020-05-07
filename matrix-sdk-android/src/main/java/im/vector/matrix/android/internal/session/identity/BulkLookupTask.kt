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

import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
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
import im.vector.matrix.android.internal.session.profile.ThirdPartyIdentifier
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
            fetchAndStoreHashDetails(identityAPI)
        } else {
            IdentityHashDetailResponse(pepper, entity.hashLookupAlgorithm.toList())
        }

        if (hashDetailResponse.algorithms.contains("sha256").not()) {
            // TODO We should ask the user if he is ok to send their 3Pid in clear, but for the moment we do not do it
            // Also, what we have in cache could be outdated, the identity server maybe now supports sha256
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

        val identityLookUpV2Response = lookUpInternal(identityAPI, hashedAddresses, hashDetailResponse, true)

        // Convert back to List<FoundThreePid>
        return handleSuccess(params.threePids, hashedAddresses, identityLookUpV2Response)
    }

    private suspend fun lookUpInternal(identityAPI: IdentityAPI,
                                       hashedAddresses: List<String>,
                                       hashDetailResponse: IdentityHashDetailResponse,
                                       canRetry: Boolean): IdentityLookUpV2Response {
        return try {
            executeRequest(null) {
                apiCall = identityAPI.bulkLookupV2(IdentityLookUpV2Params(
                        hashedAddresses,
                        "sha256",
                        hashDetailResponse.pepper
                ))
            }
        } catch (failure: Throwable) {
            // Catch invalid hash pepper and retry
            if (canRetry && failure is Failure.ServerError && failure.error.code == MatrixError.M_INVALID_PEPPER) {
                // This is not documented, by the error can contain the new pepper!
                if (!failure.error.newLookupPepper.isNullOrEmpty()) {
                    // Store it and use it right now
                    hashDetailResponse.copy(pepper = failure.error.newLookupPepper)
                            .also { identityServiceStore.setHashDetails(it) }
                            .let { lookUpInternal(identityAPI, hashedAddresses, it, false /* Avoid infinite loop */) }
                } else {
                    // Retrieve the new hash details
                    val newHashDetailResponse = fetchAndStoreHashDetails(identityAPI)

                    if (hashDetailResponse.algorithms.contains("sha256").not()) {
                        // TODO We should ask the user if he is ok to send their 3Pid in clear, but for the moment we do not do it
                        // Also, what we have in cache is maybe outdated, the identity server maybe now support sha256
                        throw IdentityServiceError.BulkLookupSha256NotSupported
                    }

                    lookUpInternal(identityAPI, hashedAddresses, newHashDetailResponse, false /* Avoid infinite loop */)
                }
            } else {
                // Other error
                throw failure
            }
        }
    }

    private suspend fun fetchAndStoreHashDetails(identityAPI: IdentityAPI): IdentityHashDetailResponse {
        return executeRequest<IdentityHashDetailResponse>(null) {
            apiCall = identityAPI.hashDetails()
        }
                .also { identityServiceStore.setHashDetails(it) }
    }

    private fun handleSuccess(threePids: List<ThreePid>, hashedAddresses: List<String>, identityLookUpV2Response: IdentityLookUpV2Response): List<FoundThreePid> {
        return identityLookUpV2Response.mappings.keys.map { hashedAddress ->
            FoundThreePid(threePids[hashedAddresses.indexOf(hashedAddress)], identityLookUpV2Response.mappings[hashedAddress] ?: error(""))
        }
    }

    private fun ThreePid.toMedium(): String {
        return when (this) {
            is ThreePid.Email  -> ThirdPartyIdentifier.MEDIUM_EMAIL
            is ThreePid.Msisdn -> ThirdPartyIdentifier.MEDIUM_MSISDN
        }
    }
}
