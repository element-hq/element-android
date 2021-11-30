/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.discovery

import im.vector.app.core.utils.ensureProtocol
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.terms.TermsService

suspend fun Session.fetchIdentityServerWithTerms(userLanguage: String): IdentityServerWithTerms? {
    val identityServerUrl = identityService().getCurrentIdentityServerUrl()
    return identityServerUrl?.let {
        val terms = getTerms(TermsService.ServiceType.IdentityService, identityServerUrl.ensureProtocol())
                .serverResponse
                .getLocalizedTerms(userLanguage)
        val policyUrls = terms.mapNotNull {
            val name = it.localizedName ?: it.policyName
            val url = it.localizedUrl
            if (name == null || url == null) {
                null
            } else {
                IdentityServerPolicy(name = name, url = url)
            }
        }
        IdentityServerWithTerms(identityServerUrl, policyUrls)
    }
}
