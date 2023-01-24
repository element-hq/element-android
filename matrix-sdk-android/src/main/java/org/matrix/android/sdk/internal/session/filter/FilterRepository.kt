/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.filter

/**
 * Repository for request filters.
 */
internal interface FilterRepository {

    /**
     * Stores sync filter and room filter.
     * Note: It looks like we could use [Filter.room.timeline] instead of a separate [RoomEventFilter], but it's not clear if it's safe, so research is needed
     * @return true if the filterBody has changed, or need to be sent to the server.
     */
    suspend fun storeSyncFilter(filter: Filter, filterId: String, roomEventFilter: RoomEventFilter)

    /**
     * Returns stored sync filter's JSON body if it exists.
     */
    suspend fun getStoredSyncFilterBody(): String?

    /**
     * Returns stored sync filter's ID if it exists.
     */
    suspend fun getStoredSyncFilterId(): String?

    /**
     * Return the room filter.
     */
    suspend fun getRoomFilterBody(): String
}
