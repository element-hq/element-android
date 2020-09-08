/*
 * Copyright (c) 2020 New Vector Ltd
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

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.identity.FoundThreePid
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium
import org.matrix.android.sdk.internal.crypto.tools.withOlmUtility
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.identity.model.IdentityHashDetailResponse
import org.matrix.android.sdk.internal.session.identity.model.IdentityLookUpParams
import org.matrix.android.sdk.internal.session.identity.model.IdentityLookUpResponse
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.base64ToBase64Url
import java.util.Locale
import javax.inject.Inject

internal interface IdentityBulkLookupTask : Task<IdentityBulkLookupTask.Params, List<FoundThreePid>> {
    data class Params(
            val threePids: List<ThreePid>
    )
}

internal class DefaultIdentityBulkLookupTask @Inject constructor(
        private val identityApiProvider: IdentityApiProvider,
        private val identityStore: IdentityStore,
        @UserId private val userId: String
) : IdentityBulkLookupTask {

    override suspend fun execute(params: IdentityBulkLookupTask.Params): List<FoundThreePid> {
        val identityAPI = getIdentityApiAndEnsureTerms(identityApiProvider, userId)
        val identityData = identityStore.getIdentityData() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val pepper = identityData.hashLookupPepper
        val hashDetailResponse = if (pepper == null) {
            // We need to fetch the hash details first
            fetchAndStoreHashDetails(identityAPI)
        } else {
            IdentityHashDetailResponse(pepper, identityData.hashLookupAlgorithm)
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
                                       canRetry: Boolean): IdentityLookUpResponse {
        return try {
            executeRequest(null) {
                apiCall = identityAPI.lookup(IdentityLookUpParams(
                        hashedAddresses,
                        IdentityHashDetailResponse.ALGORITHM_SHA256,
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
                            .also { identityStore.setHashDetails(it) }
                            .let { lookUpInternal(identityAPI, hashedAddresses, it, false /* Avoid infinite loop */) }
                } else {
                    // Retrieve the new hash details
                    val newHashDetailResponse = fetchAndStoreHashDetails(identityAPI)

                    if (hashDetailResponse.algorithms.contains(IdentityHashDetailResponse.ALGORITHM_SHA256).not()) {
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
                .also { identityStore.setHashDetails(it) }
    }

    private fun handleSuccess(threePids: List<ThreePid>, hashedAddresses: List<String>, identityLookUpResponse: IdentityLookUpResponse): List<FoundThreePid> {
        return identityLookUpResponse.mappings.keys.map { hashedAddress ->
            FoundThreePid(threePids[hashedAddresses.indexOf(hashedAddress)], identityLookUpResponse.mappings[hashedAddress] ?: error(""))
        }
    }
}
