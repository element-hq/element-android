/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.search

/**
 * This interface defines methods to search messages in rooms.
 */
interface SearchService {

    /**
     * Generic function to search a term in a room.
     * Ref: https://matrix.org/docs/spec/client_server/latest#module-search
     * @param searchTerm the term to search
     * @param roomId the roomId to search term inside
     * @param nextBatch the token that retrieved from the previous response. Should be provided to get the next batch of results
     * @param orderByRecent if true, the most recent message events will return in the first places of the list
     * @param limit the maximum number of events to return.
     * @param beforeLimit how many events before the result are returned.
     * @param afterLimit how many events after the result are returned.
     * @param includeProfile requests that the server returns the historic profile information for the users that sent the events that were returned.
     */
    suspend fun search(
            searchTerm: String,
            roomId: String,
            nextBatch: String?,
            orderByRecent: Boolean,
            limit: Int,
            beforeLimit: Int,
            afterLimit: Int,
            includeProfile: Boolean
    ): SearchResult
}
