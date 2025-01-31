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
import org.matrix.android.sdk.api.session.crypto.model.GossipingToDeviceObject
import org.matrix.android.sdk.api.session.crypto.model.GossipingToDeviceObject.Companion.ACTION_SHARE_CANCELLATION

/**
 * Class representing a room key request cancellation content.
 */
@JsonClass(generateAdapter = true)
internal data class ShareRequestCancellation(
        @Json(name = "action")
        override val action: String? = ACTION_SHARE_CANCELLATION,

        @Json(name = "requesting_device_id")
        override val requestingDeviceId: String? = null,

        @Json(name = "request_id")
        override val requestId: String? = null
) : GossipingToDeviceObject
