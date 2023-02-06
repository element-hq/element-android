/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.list.data

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val A_ROOM_ID = "room-id"

class RoomPollRepositoryTest {

    private val fakeRoomPollDataSource = mockk<RoomPollDataSource>()

    private val roomPollRepository = RoomPollRepository(
            roomPollDataSource = fakeRoomPollDataSource,
    )

    @Test
    fun `given data source when dispose then correct method of data source is called`() {
        // Given
        justRun { fakeRoomPollDataSource.dispose(A_ROOM_ID) }

        // When
        roomPollRepository.dispose(A_ROOM_ID)

        // Then
        verify { fakeRoomPollDataSource.dispose(A_ROOM_ID) }
    }

    @Test
    fun `given data source when getting polls then correct method of data source is called`() = runTest {
        // Given
        val expectedPolls = listOf<TimelineEvent>()
        every { fakeRoomPollDataSource.getPolls(A_ROOM_ID) } returns flowOf(expectedPolls)

        // When
        val result = roomPollRepository.getPolls(A_ROOM_ID).firstOrNull()

        // Then
        result shouldBeEqualTo expectedPolls
        verify { fakeRoomPollDataSource.getPolls(A_ROOM_ID) }
    }

    @Test
    fun `given data source when getting loaded polls status then correct method of data source is called`() = runTest {
        // Given
        val expectedStatus = LoadedPollsStatus(
                canLoadMore = true,
                daysSynced = 10,
                hasCompletedASyncBackward = false,
        )
        coEvery { fakeRoomPollDataSource.getLoadedPollsStatus(A_ROOM_ID) } returns expectedStatus

        // When
        val result = roomPollRepository.getLoadedPollsStatus(A_ROOM_ID)

        // Then
        result shouldBeEqualTo expectedStatus
        coVerify { fakeRoomPollDataSource.getLoadedPollsStatus(A_ROOM_ID) }
    }

    @Test
    fun `given data source when loading more polls then correct method of data source is called`() = runTest {
        // Given
        coJustRun { fakeRoomPollDataSource.loadMorePolls(A_ROOM_ID) }

        // When
        roomPollRepository.loadMorePolls(A_ROOM_ID)

        // Then
        coVerify { fakeRoomPollDataSource.loadMorePolls(A_ROOM_ID) }
    }

    @Test
    fun `given data source when syncing polls then correct method of data source is called`() = runTest {
        // Given
        coJustRun { fakeRoomPollDataSource.syncPolls(A_ROOM_ID) }

        // When
        roomPollRepository.syncPolls(A_ROOM_ID)

        // Then
        coVerify { fakeRoomPollDataSource.syncPolls(A_ROOM_ID) }
    }
}
