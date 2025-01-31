/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.terms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class represent a list of urls of terms the user wants to accept.
 */
@JsonClass(generateAdapter = true)
internal data class AcceptTermsBody(
        @Json(name = "user_accepts")
        val acceptedTermUrls: List<String>
)
