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
 * This class provides the parameter to delete a device.
 */
@JsonClass(generateAdapter = true)
internal data class DeleteDeviceParams(
        @Json(name = "auth")
        val auth: Map<String, *>? = null
)
