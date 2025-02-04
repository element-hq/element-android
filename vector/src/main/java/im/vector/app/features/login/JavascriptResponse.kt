/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.Credentials

@JsonClass(generateAdapter = true)
data class JavascriptResponse(
        @Json(name = "action")
        val action: String? = null,

        /**
         * Use for captcha result.
         */
        @Json(name = "response")
        val response: String? = null,

        /**
         * Used for login/registration result.
         */
        @Json(name = "credentials")
        val credentials: Credentials? = null
)
