/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.accountdata

import androidx.lifecycle.LiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataEvent
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataService
import org.matrix.android.sdk.api.util.Optional

internal class DefaultRoomAccountDataService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val dataSource: RoomAccountDataDataSource,
        private val updateRoomAccountDataTask: UpdateRoomAccountDataTask
) : RoomAccountDataService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultRoomAccountDataService
    }

    override fun getAccountDataEvent(type: String): RoomAccountDataEvent? {
        return dataSource.getAccountDataEvent(roomId, type)
    }

    override fun getLiveAccountDataEvent(type: String): LiveData<Optional<RoomAccountDataEvent>> {
        return dataSource.getLiveAccountDataEvent(roomId, type)
    }

    override fun getAccountDataEvents(types: Set<String>): List<RoomAccountDataEvent> {
        return dataSource.getAccountDataEvents(roomId, types)
    }

    override fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<RoomAccountDataEvent>> {
        return dataSource.getLiveAccountDataEvents(roomId, types)
    }

    override suspend fun updateAccountData(type: String, content: Content) {
        val params = UpdateRoomAccountDataTask.Params(roomId, type, content)
        return updateRoomAccountDataTask.execute(params)
    }
}
