/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.tags

/**
 * This interface defines methods to handle tags of a room. It's implemented at the room level.
 */
interface TagsService {
    /**
     * Add a tag to a room.
     */
    suspend fun addTag(tag: String, order: Double?)

    /**
     * Remove tag from a room.
     */
    suspend fun deleteTag(tag: String)
}
