/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.pushers.gateway

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PushGatewayDevice(
        /**
         * Required. The app_id given when the pusher was created.
         */
        @Json(name = "app_id")
        val appId: String,
        /**
         * Required. The pushkey given when the pusher was created.
         */
        @Json(name = "pushkey")
        val pushKey: String
)
