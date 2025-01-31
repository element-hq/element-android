/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync

var initialSyncStrategy: InitialSyncStrategy = InitialSyncStrategy.Optimized()

sealed class InitialSyncStrategy {
    /**
     * Parse the result in its entirety.
     * Pros:
     * - Faster to handle parsed data
     * Cons:
     * - Slower to download and parse data
     * - big RAM usage
     * - not robust to crash
     */
    object Legacy : InitialSyncStrategy()

    /**
     * Optimized.
     * First store the request result in a file, to avoid doing it again in case of crash.
     */
    data class Optimized(
            /**
             * Limit to reach to decide to split the init sync response into smaller files.
             * Empiric value: 1 megabytes.
             */
            val minSizeToSplit: Long = 1_048_576, // 1024 * 1024
            /**
             * Limit per room to reach to decide to store a join room ephemeral Events into a file.
             * Empiric value: 1 kilobytes.
             */
            val minSizeToStoreInFile: Long = 1024,
            /**
             * Max number of rooms to insert at a time in database (to avoid too much RAM usage).
             */
            val maxRoomsToInsert: Int = 100
    ) : InitialSyncStrategy()
}
