/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
