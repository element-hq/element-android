/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.data

import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class RoomPollRepository @Inject constructor(
        private val roomPollDataSource: RoomPollDataSource,
) {

    fun dispose(roomId: String) {
        roomPollDataSource.dispose(roomId)
    }

    fun getPolls(roomId: String): Flow<List<TimelineEvent>> {
        return roomPollDataSource.getPolls(roomId)
    }

    suspend fun getLoadedPollsStatus(roomId: String): LoadedPollsStatus {
        return roomPollDataSource.getLoadedPollsStatus(roomId)
    }

    suspend fun loadMorePolls(roomId: String): LoadedPollsStatus {
        return roomPollDataSource.loadMorePolls(roomId)
    }

    suspend fun syncPolls(roomId: String) {
        return roomPollDataSource.syncPolls(roomId)
    }
}
