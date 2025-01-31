/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing the ThreePids response.
 */
@JsonClass(generateAdapter = true)
internal data class AccountThreePidsResponse(
        @Json(name = "threepids")
        val threePids: List<ThirdPartyIdentifier>? = null
)
