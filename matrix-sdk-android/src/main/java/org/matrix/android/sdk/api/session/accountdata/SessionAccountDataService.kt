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

package org.matrix.android.sdk.api.session.accountdata

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataEvent
import org.matrix.android.sdk.api.util.Optional

/**
 * This service is attached globally to the session.
 */
interface SessionAccountDataService {
    /**
     * Retrieve the account data with the provided type or null if not found
     */
    fun getUserAccountDataEvent(type: String): UserAccountDataEvent?

    /**
     * Observe the account data with the provided type
     */
    fun getLiveUserAccountDataEvent(type: String): LiveData<Optional<UserAccountDataEvent>>

    /**
     * Retrieve the account data with the provided types. The return list can have a different size that
     * the size of the types set, because some AccountData may not exist.
     * If an empty set is provided, all the AccountData are retrieved
     */
    fun getUserAccountDataEvents(types: Set<String>): List<UserAccountDataEvent>

    /**
     * Observe the account data with the provided types. If an empty set is provided, all the AccountData are observed
     */
    fun getLiveUserAccountDataEvents(types: Set<String>): LiveData<List<UserAccountDataEvent>>

    /**
     * Retrieve the room account data with the provided types. The return list can have a different size that
     * the size of the types set, because some AccountData may not exist.
     * If an empty set is provided, all the room AccountData are retrieved
     */
    fun getRoomAccountDataEvents(types: Set<String>): List<RoomAccountDataEvent>

    /**
     * Observe the room account data with the provided types. If an empty set is provided, AccountData of every room are observed
     */
    fun getLiveRoomAccountDataEvents(types: Set<String>): LiveData<List<RoomAccountDataEvent>>

    /**
     * Update the account data with the provided type and the provided account data content
     */
    suspend fun updateUserAccountData(type: String, content: Content)
}
