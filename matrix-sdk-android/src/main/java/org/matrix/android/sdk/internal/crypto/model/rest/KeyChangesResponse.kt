/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class describes the key changes response.
 */
@JsonClass(generateAdapter = true)
internal data class KeyChangesResponse(
        // list of user ids which have new devices
        @Json(name = "changed")
        val changed: List<String>? = null,

        //  List of user ids who are no more tracked.
        @Json(name = "left")
        val left: List<String>? = null
)
