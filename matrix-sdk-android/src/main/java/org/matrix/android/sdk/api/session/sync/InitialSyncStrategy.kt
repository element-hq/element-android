/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.sync

var initialSyncStrategy: InitialSyncStrategy = InitialSyncStrategy.Optimized()

sealed class InitialSyncStrategy {
    /**
     * Parse the result in its entirety
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
     * First store the request result in a file, to avoid doing it again in case of crash
     */
    data class Optimized(
            /**
             * Limit to reach to decide to split the init sync response into smaller files
             * Empiric value: 1 megabytes
             */
            val minSizeToSplit: Long = 1_048_576, // 1024 * 1024
            /**
             * Limit per room to reach to decide to store a join room ephemeral Events into a file
             * Empiric value: 1 kilobytes
             */
            val minSizeToStoreInFile: Long = 1024,
            /**
             * Max number of rooms to insert at a time in database (to avoid too much RAM usage)
             */
            val maxRoomsToInsert: Int = 100
    ) : InitialSyncStrategy()
}
