/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.reporting

/**
 * This interface defines methods to report content of an event.
 */
interface ReportingService {

    /**
     * Report content
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-rooms-roomid-report-eventid
     */
    suspend fun reportContent(eventId: String, score: Int, reason: String)
}
