/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth

import org.matrix.android.sdk.api.auth.data.LocalizedFlowDataLoginTerms
import org.matrix.android.sdk.api.auth.registration.TermPolicies

/**
 * This method extract the policies from the login terms parameter, regarding the user language.
 * For each policy, if user language is not found, the default language is used and if not found, the first url and name are used (not predictable)
 *
 * Example of Data:
 * <pre>
 * "m.login.terms": {
 *       "policies": {
 *         "privacy_policy": {
 *           "version": "1.0",
 *           "en": {
 *             "url": "http:\/\/matrix.org\/_matrix\/consent?v=1.0",
 *             "name": "Terms and Conditions"
 *           }
 *         }
 *       }
 *     }
 *</pre>
 *
 * @param userLanguage the user language
 * @param defaultLanguage the default language to use if the user language is not found for a policy in registrationFlowResponse
 */
fun TermPolicies.toLocalizedLoginTerms(
        userLanguage: String,
        defaultLanguage: String = "en"
): List<LocalizedFlowDataLoginTerms> {
    val result = ArrayList<LocalizedFlowDataLoginTerms>()

    val policies = get("policies")
    if (policies is Map<*, *>) {
        policies.keys.forEach { policyName ->
            val localizedFlowDataLoginTermsPolicyName = policyName as String
            var localizedFlowDataLoginTermsVersion: String? = null
            var localizedFlowDataLoginTermsLocalizedUrl: String? = null
            var localizedFlowDataLoginTermsLocalizedName: String? = null

            val policy = policies[policyName]

            // Enter this policy
            if (policy is Map<*, *>) {
                // Version
                localizedFlowDataLoginTermsVersion = policy["version"] as String?

                var userLanguageUrlAndName: UrlAndName? = null
                var defaultLanguageUrlAndName: UrlAndName? = null
                var firstUrlAndName: UrlAndName? = null

                // Search for language
                policy.keys.forEach { policyKey ->
                    when (policyKey) {
                        "version" -> Unit // Ignore
                        userLanguage -> {
                            // We found the data for the user language
                            userLanguageUrlAndName = extractUrlAndName(policy[policyKey])
                        }
                        defaultLanguage -> {
                            // We found default language
                            defaultLanguageUrlAndName = extractUrlAndName(policy[policyKey])
                        }
                        else -> {
                            if (firstUrlAndName == null) {
                                // Get at least some data
                                firstUrlAndName = extractUrlAndName(policy[policyKey])
                            }
                        }
                    }
                }

                // Copy found language data by priority
                when {
                    userLanguageUrlAndName != null -> {
                        localizedFlowDataLoginTermsLocalizedUrl = userLanguageUrlAndName!!.url
                        localizedFlowDataLoginTermsLocalizedName = userLanguageUrlAndName!!.name
                    }
                    defaultLanguageUrlAndName != null -> {
                        localizedFlowDataLoginTermsLocalizedUrl = defaultLanguageUrlAndName!!.url
                        localizedFlowDataLoginTermsLocalizedName = defaultLanguageUrlAndName!!.name
                    }
                    firstUrlAndName != null -> {
                        localizedFlowDataLoginTermsLocalizedUrl = firstUrlAndName!!.url
                        localizedFlowDataLoginTermsLocalizedName = firstUrlAndName!!.name
                    }
                }
            }

            result.add(
                    LocalizedFlowDataLoginTerms(
                            policyName = localizedFlowDataLoginTermsPolicyName,
                            version = localizedFlowDataLoginTermsVersion,
                            localizedUrl = localizedFlowDataLoginTermsLocalizedUrl,
                            localizedName = localizedFlowDataLoginTermsLocalizedName
                    )
            )
        }
    }

    return result
}

private fun extractUrlAndName(policyData: Any?): UrlAndName? {
    if (policyData is Map<*, *>) {
        val url = policyData["url"] as String?
        val name = policyData["name"] as String?

        if (url != null && name != null) {
            return UrlAndName(url, name)
        }
    }
    return null
}
