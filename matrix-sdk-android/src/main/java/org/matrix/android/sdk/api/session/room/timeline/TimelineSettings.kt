/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.timeline

/**
 * Data class holding setting values for a [Timeline] instance.
 */
data class TimelineSettings(
        /**
         * The initial number of events to retrieve from cache. You might get less events if you don't have loaded enough yet.
         */
        val initialSize: Int,
        /**
         * If true, will build read receipts for each event.
         */
        val buildReadReceipts: Boolean = true,
        /**
         * The root thread eventId if this is a thread timeline, or null if this is NOT a thread timeline.
         */
        val rootThreadEventId: String? = null,
        /**
         * If true Sender Info shown in room will get the latest data information (avatar + displayName).
         */
        val useLiveSenderInfo: Boolean = false,
) {

    /**
     * Returns true if this is a thread timeline or false otherwise.
     */
    fun isThreadTimeline() = rootThreadEventId != null
}
