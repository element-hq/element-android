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

//  PresenceSyncResponse represents the updates to the presence status of other users during server sync v2.
@JsonClass(generateAdapter = true)
data class PresenceSyncResponse(

        /**
         * List of presence events (array of Event with type m.presence).
         */
        val events: List<Event>? = null
)
