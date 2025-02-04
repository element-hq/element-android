/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MatrixClientInfoContent(
        // app name
        @Json(name = "name")
        val name: String? = null,
        // app version
        @Json(name = "version")
        val version: String? = null,
        // app url (optional, applicable only for web)
        @Json(name = "url")
        val url: String? = null,
)
