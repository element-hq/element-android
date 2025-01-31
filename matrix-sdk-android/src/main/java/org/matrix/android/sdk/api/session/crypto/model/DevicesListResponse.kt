/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class describes the response to https://matrix.org/docs/spec/client_server/r0.4.0.html#get-matrix-client-r0-devices
 */
@JsonClass(generateAdapter = true)
data class DevicesListResponse(
        @Json(name = "devices")
        val devices: List<DeviceInfo>? = null
)
