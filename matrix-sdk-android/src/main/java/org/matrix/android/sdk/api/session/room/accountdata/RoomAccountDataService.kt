/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.accountdata

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.util.Optional

/**
 * This service is attached to a single room.
 */
interface RoomAccountDataService {
    /**
     * Retrieve the account data with the provided type or null if not found.
     */
    fun getAccountDataEvent(type: String): RoomAccountDataEvent?

    /**
     * Observe the account data with the provided type.
     */
    fun getLiveAccountDataEvent(type: String): LiveData<Optional<RoomAccountDataEvent>>

    /**
     * Retrieve the account data with the provided types. The return list can have a different size that
     * the size of the types set, because some AccountData may not exist.
     * If an empty set is provided, all the AccountData are retrieved.
     */
    fun getAccountDataEvents(types: Set<String>): List<RoomAccountDataEvent>

    /**
     * Observe the account data with the provided types. If an empty set is provided, all the AccountData are observed.
     */
    fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<RoomAccountDataEvent>>

    /**
     * Update the account data with the provided type and the provided account data content.
     */
    suspend fun updateAccountData(type: String, content: Content)
}
