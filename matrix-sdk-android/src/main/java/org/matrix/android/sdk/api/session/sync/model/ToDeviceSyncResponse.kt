/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

// ToDeviceSyncResponse represents the data directly sent to one of user's devices.
@JsonClass(generateAdapter = true)
data class ToDeviceSyncResponse(

        /**
         * List of direct-to-device events.
         */
        val events: List<Event>? = null
)
