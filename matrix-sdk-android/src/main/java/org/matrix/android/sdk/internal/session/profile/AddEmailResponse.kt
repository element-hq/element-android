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

@JsonClass(generateAdapter = true)
internal data class AddEmailResponse(
        /**
         * Required. The session ID. Session IDs are opaque strings that must consist entirely
         * of the characters [0-9a-zA-Z.=_-]. Their length must not exceed 255 characters and they must not be empty.
         */
        @Json(name = "sid")
        val sid: String
)
