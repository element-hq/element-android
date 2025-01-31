/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.filter

internal interface FilterRepository {

    /**
     * Return true if the filterBody has changed, or need to be sent to the server.
     */
    suspend fun storeFilter(filter: Filter, roomEventFilter: RoomEventFilter): Boolean

    /**
     * Set the filterId of this filter.
     */
    suspend fun storeFilterId(filter: Filter, filterId: String)

    /**
     * Return filter json or filter id.
     */
    suspend fun getFilter(): String

    /**
     * Return the room filter.
     */
    suspend fun getRoomFilter(): String
}
