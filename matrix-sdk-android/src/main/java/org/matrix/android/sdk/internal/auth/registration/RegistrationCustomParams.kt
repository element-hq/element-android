/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict

/**
 * Class to pass parameters to the custom registration types for /register.
 */
@JsonClass(generateAdapter = true)
internal data class RegistrationCustomParams(
        // authentication parameters
        @Json(name = "auth")
        val auth: JsonDict? = null,
)
