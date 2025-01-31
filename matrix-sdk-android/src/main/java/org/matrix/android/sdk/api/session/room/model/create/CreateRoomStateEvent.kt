/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.create

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content

@JsonClass(generateAdapter = true)
data class CreateRoomStateEvent(
        /**
         * Required. The type of event to send.
         */
        val type: String,

        /**
         * Required. The content of the event.
         */
        val content: Content,

        /**
         * The state_key of the state event. Defaults to an empty string.
         */
        val stateKey: String = ""
)
