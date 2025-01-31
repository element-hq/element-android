/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.uploads

data class GetUploadsResult(
        // List of fetched Events, most recent first
        val uploadEvents: List<UploadEvent>,
        // token to get more events
        val nextToken: String,
        // True if there are more event to load
        val hasMore: Boolean
)
