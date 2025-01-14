/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.discovery

import im.vector.app.core.utils.ensureProtocol
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.terms.TermsResponse
import org.matrix.android.sdk.api.session.terms.TermsService

suspend fun Session.fetchIdentityServerWithTerms(userLanguage: String): ServerAndPolicies? {
    return identityService().getCurrentIdentityServerUrl()
            ?.let { identityServerUrl ->
                val termsResponse = termsService().getTerms(TermsService.ServiceType.IdentityService, identityServerUrl.ensureProtocol())
                        .serverResponse
                buildServerAndPolicies(identityServerUrl, termsResponse, userLanguage)
            }
}

suspend fun Session.fetchHomeserverWithTerms(userLanguage: String): ServerAndPolicies {
    val homeserverUrl = sessionParams.homeServerUrl
    val terms = termsService().getHomeserverTerms(homeserverUrl.ensureProtocol())
    return buildServerAndPolicies(homeserverUrl, terms, userLanguage)
}

private fun buildServerAndPolicies(
        serviceUrl: String,
        termsResponse: TermsResponse,
        userLanguage: String
): ServerAndPolicies {
    val terms = termsResponse.getLocalizedTerms(userLanguage)
    val policyUrls = terms.mapNotNull {
        val name = it.localizedName ?: it.policyName
        val url = it.localizedUrl
        if (name == null || url == null) {
            null
        } else {
            ServerPolicy(name = name, url = url)
        }
    }
    return ServerAndPolicies(serviceUrl, policyUrls)
}
