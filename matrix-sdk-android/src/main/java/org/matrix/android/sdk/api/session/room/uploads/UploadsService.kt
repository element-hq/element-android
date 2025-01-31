/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.uploads

/**
 * This interface defines methods to get event with uploads (= attachments) sent to a room. It's implemented at the room level.
 */
interface UploadsService {

    /**
     * Get a list of events containing URL sent to a room, from most recent to oldest one.
     * @param numberOfEvents the expected number of events to retrieve. The result can contain less events.
     * @param since token to get next page, or null to get the first page
     */
    suspend fun getUploads(numberOfEvents: Int, since: String?): GetUploadsResult
}
