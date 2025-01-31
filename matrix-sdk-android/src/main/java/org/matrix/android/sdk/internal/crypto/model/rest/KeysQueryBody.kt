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
 * This class represents the body to /keys/query.
 */
@JsonClass(generateAdapter = true)
internal data class KeysQueryBody(
        /**
         * The time (in milliseconds) to wait when downloading keys from remote servers. 10 seconds is the recommended default.
         */
        @Json(name = "timeout")
        val timeout: Int? = null,

        /**
         * Required. The keys to be downloaded.
         * A map from user ID, to a list of device IDs, or to an empty list to indicate all devices for the corresponding user.
         */
        @Json(name = "device_keys")
        val deviceKeys: Map<String, List<String>>,

        /**
         * If the client is fetching keys as a result of a device update received in a sync request, this should be the 'since' token
         * of that sync request, or any later sync token. This allows the server to ensure its response contains the keys advertised
         * by the notification in that sync.
         */
        @Json(name = "token")
        val token: String? = null
)
