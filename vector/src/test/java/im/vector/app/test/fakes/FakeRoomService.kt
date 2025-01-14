/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount

class FakeRoomService(
        private val fakeRoom: FakeRoom = FakeRoom()
) : RoomService by mockk() {

    override fun getRoom(roomId: String) = fakeRoom

    fun getRoomSummaryReturns(roomSummary: RoomSummary?) {
        every { getRoomSummary(any()) } returns roomSummary
    }

    fun set(roomSummary: RoomSummary?) {
        every { getRoomSummary(any()) } returns roomSummary
    }

    fun givenGetPagedRoomSummariesLiveReturns(pagedList: PagedList<RoomSummary>): LiveData<PagedList<RoomSummary>> {
        return MutableLiveData(pagedList).also {
            every { getPagedRoomSummariesLive(queryParams = any(), pagedListConfig = any(), sortOrder = any()) } returns it
        }
    }

    fun givenGetNotificationCountForRoomsReturns(roomAggregateNotificationCount: RoomAggregateNotificationCount) {
        every { getNotificationCountForRooms(queryParams = any()) } returns roomAggregateNotificationCount
    }

    fun givenGetRoomSummaries(roomSummaries: List<RoomSummary>) {
        every { getRoomSummaries(any()) } returns roomSummaries
    }
}
