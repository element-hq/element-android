/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.cache

/**
 * This interface defines a method to clear the cache. It's implemented at the session level.
 */
interface CacheService {

    /**
     * Clear the whole cached data, except credentials. Once done, the sync has to be restarted by the sdk user.
     */
    suspend fun clearCache()
}
