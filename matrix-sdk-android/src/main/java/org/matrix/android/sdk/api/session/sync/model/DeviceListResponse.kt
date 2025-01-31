/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.JsonClass

/**
 * This class describes the device list response from a sync request.
 */
@JsonClass(generateAdapter = true)
data class DeviceListResponse(
        // user ids list which have new crypto devices
        val changed: List<String> = emptyList(),
        //  List of user ids who are no more tracked.
        val left: List<String> = emptyList()
)
