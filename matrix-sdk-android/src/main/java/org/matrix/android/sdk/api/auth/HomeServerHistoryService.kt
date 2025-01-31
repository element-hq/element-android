/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth

/**
 * A simple service to remember homeservers you already connected to.
 */
interface HomeServerHistoryService {
    /**
     * Get a list of stored homeserver urls.
     */
    fun getKnownServersUrls(): List<String>

    /**
     * Add a homeserver url to the list of stored homeserver urls.
     * Will not be added again if already present in the list.
     */
    fun addHomeServerToHistory(url: String)

    /**
     * Delete the list of stored homeserver urls.
     */
    fun clearHistory()
}
