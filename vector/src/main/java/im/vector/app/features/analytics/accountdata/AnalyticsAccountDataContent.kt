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

package im.vector.app.features.analytics.accountdata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnalyticsAccountDataContent(
        // A randomly generated analytics token for this user.
        // This is suggested to be a 128-bit hex encoded string.
        @Json(name = "id")
        val id: String? = null,
        // Boolean indicating whether the user has opted in.
        // If null or not set, the user hasn't yet given consent either way
        @Json(name = "pseudonymousAnalyticsOptIn")
        val pseudonymousAnalyticsOptIn: Boolean? = null,
        // Boolean indicating whether to show the analytics opt-in prompt.
        @Json(name = "showPseudonymousAnalyticsPrompt")
        val showPseudonymousAnalyticsPrompt: Boolean? = null
)
