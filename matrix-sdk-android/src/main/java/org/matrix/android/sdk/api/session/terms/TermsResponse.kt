/*
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

package org.matrix.android.sdk.api.session.terms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LocalizedFlowDataLoginTerms
import org.matrix.android.sdk.api.util.JsonDict

/**
 * This class represent a localized privacy policy for registration Flow.
 */
@JsonClass(generateAdapter = true)
data class TermsResponse(
        @Json(name = "policies")
        val policies: JsonDict? = null
) {

    fun getLocalizedTerms(userLanguage: String,
                          defaultLanguage: String = "en"): List<LocalizedFlowDataLoginTerms> {
        return policies?.map {
            val tos = policies[it.key] as? Map<*, *> ?: return@map null
            ((tos[userLanguage] ?: tos[defaultLanguage]) as? Map<*, *>)?.let { termsMap ->
                val name = termsMap[NAME] as? String
                val url = termsMap[URL] as? String
                LocalizedFlowDataLoginTerms(
                        policyName = it.key,
                        localizedUrl = url,
                        localizedName = name,
                        version = tos[VERSION] as? String
                )
            }
        }?.filterNotNull().orEmpty()
    }

    private companion object {
        const val VERSION = "version"
        const val NAME = "name"
        const val URL = "url"
    }
}
