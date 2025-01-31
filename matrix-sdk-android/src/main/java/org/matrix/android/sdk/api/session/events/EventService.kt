/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.events

import org.matrix.android.sdk.api.session.events.model.Event

interface EventService {

    /**
     * Ask the homeserver for an event content. The SDK will try to decrypt it if it is possible
     * The result will not be stored into cache
     */
    suspend fun getEvent(
            roomId: String,
            eventId: String
    ): Event
}
