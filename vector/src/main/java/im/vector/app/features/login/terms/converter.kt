/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.login.terms

import org.matrix.android.sdk.api.auth.registration.TermPolicies
import org.matrix.android.sdk.internal.auth.registration.LocalizedFlowDataLoginTerms

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
fun TermPolicies.toLocalizedLoginTerms(userLanguage: String,
                                       defaultLanguage: String = "en"): List<LocalizedFlowDataLoginTerms> {
    val result = ArrayList<LocalizedFlowDataLoginTerms>()

    val policies = get("policies")
    if (policies is Map<*, *>) {
        policies.keys.forEach { policyName ->
            val localizedFlowDataLoginTerms = LocalizedFlowDataLoginTerms()
            localizedFlowDataLoginTerms.policyName = policyName as String

            val policy = policies[policyName]

            // Enter this policy
            if (policy is Map<*, *>) {
                // Version
                localizedFlowDataLoginTerms.version = policy["version"] as String?

                var userLanguageUrlAndName: UrlAndName? = null
                var defaultLanguageUrlAndName: UrlAndName? = null
                var firstUrlAndName: UrlAndName? = null

                // Search for language
                policy.keys.forEach { policyKey ->
                    when (policyKey) {
                        "version"       -> Unit // Ignore
                        userLanguage    -> {
                            // We found the data for the user language
                            userLanguageUrlAndName = extractUrlAndName(policy[policyKey])
                        }
                        defaultLanguage -> {
                            // We found default language
                            defaultLanguageUrlAndName = extractUrlAndName(policy[policyKey])
                        }
                        else            -> {
                            if (firstUrlAndName == null) {
                                // Get at least some data
                                firstUrlAndName = extractUrlAndName(policy[policyKey])
                            }
                        }
                    }
                }

                // Copy found language data by priority
                when {
                    userLanguageUrlAndName != null    -> {
                        localizedFlowDataLoginTerms.localizedUrl = userLanguageUrlAndName!!.url
                        localizedFlowDataLoginTerms.localizedName = userLanguageUrlAndName!!.name
                    }
                    defaultLanguageUrlAndName != null -> {
                        localizedFlowDataLoginTerms.localizedUrl = defaultLanguageUrlAndName!!.url
                        localizedFlowDataLoginTerms.localizedName = defaultLanguageUrlAndName!!.name
                    }
                    firstUrlAndName != null           -> {
                        localizedFlowDataLoginTerms.localizedUrl = firstUrlAndName!!.url
                        localizedFlowDataLoginTerms.localizedName = firstUrlAndName!!.name
                    }
                }
            }

            result.add(localizedFlowDataLoginTerms)
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
