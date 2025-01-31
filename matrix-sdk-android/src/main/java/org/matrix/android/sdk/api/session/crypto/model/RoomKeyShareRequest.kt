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
 * Class representing a room key request content.
 */
@JsonClass(generateAdapter = true)
data class RoomKeyShareRequest(
        @Json(name = "action")
        override val action: String? = GossipingToDeviceObject.ACTION_SHARE_REQUEST,

        @Json(name = "requesting_device_id")
        override val requestingDeviceId: String? = null,

        @Json(name = "request_id")
        override val requestId: String? = null,

        @Json(name = "body")
        val body: RoomKeyRequestBody? = null
) : GossipingToDeviceObject
