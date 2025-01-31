/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.terms

interface TermsService {
    enum class ServiceType {
        IntegrationManager,
        IdentityService
    }

    suspend fun getTerms(serviceType: ServiceType, baseUrl: String): GetTermsResponse

    suspend fun agreeToTerms(
            serviceType: ServiceType,
            baseUrl: String,
            agreedUrls: List<String>,
            token: String?
    )

    /**
     * Get the homeserver terms, from the register API.
     * Will be updated once https://github.com/matrix-org/matrix-doc/pull/3012 will be implemented.
     */
    suspend fun getHomeserverTerms(baseUrl: String): TermsResponse
}
