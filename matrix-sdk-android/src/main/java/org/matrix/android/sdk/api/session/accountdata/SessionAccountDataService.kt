/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
     * Retrieve the account data with the provided type or null if not found.
     */
    fun getUserAccountDataEvent(type: String): UserAccountDataEvent?

    /**
     * Observe the account data with the provided type.
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
     * Update the account data with the provided type and the provided account data content.
     */
    suspend fun updateUserAccountData(type: String, content: Content)
}
