/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.legacy.riot

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * <b>IMPORTANT:</b> This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 *
 * https://matrix.org/docs/spec/client_server/r0.4.0.html#server-discovery
 * <pre>
 * {
 *     "base_url": "https://vector.im"
 * }
 * </pre>
 */
@JsonClass(generateAdapter = true)
class WellKnownBaseConfig {

    @JvmField
    @Json(name = "base_url")
    var baseURL: String? = null
}
