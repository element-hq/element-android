/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.data

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.FakePollHistoryService
import im.vector.app.test.fakes.givenAsFlow
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val A_ROOM_ID = "room-id"

internal class RoomPollDataSourceTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val roomPollDataSource = RoomPollDataSource(
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Test
    fun `given poll history service when dispose then correct method of service is called`() {
        // Given
        val fakePollHistoryService = givenPollHistoryService()
        fakePollHistoryService.givenDispose()

        // When
        roomPollDataSource.dispose(A_ROOM_ID)

        // Then
        fakePollHistoryService.verifyDispose()
    }

    @Test
    fun `given poll history service when get polls then correct method of service is called and correct result is returned`() = runTest {
        // Given
        val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()
        fakeFlowLiveDataConversions.setup()
        val fakePollHistoryService = givenPollHistoryService()
        val pollEvents = listOf<TimelineEvent>()
        fakePollHistoryService
                .givenGetPollsReturns(pollEvents)
                .givenAsFlow()

        // When
        val result = roomPollDataSource.getPolls(A_ROOM_ID).firstOrNull()

        // Then
        result shouldBeEqualTo pollEvents
        fakePollHistoryService.verifyGetPolls()
        unmockkAll()
    }

    @Test
    fun `given poll history service when get loaded polls then correct method of service is called and correct result is returned`() = runTest {
        // Given
        val fakePollHistoryService = givenPollHistoryService()
        val aLoadedPollsStatus = givenALoadedPollsStatus()
        fakePollHistoryService.givenGetLoadedPollsStatusReturns(aLoadedPollsStatus)

        // When
        val result = roomPollDataSource.getLoadedPollsStatus(A_ROOM_ID)

        // Then
        result shouldBeEqualTo aLoadedPollsStatus
        fakePollHistoryService.verifyGetLoadedPollsStatus()
    }

    @Test
    fun `given poll history service when load more then correct method of service is called and correct result is returned`() = runTest {
        // Given
        val fakePollHistoryService = givenPollHistoryService()
        val aLoadedPollsStatus = givenALoadedPollsStatus()
        fakePollHistoryService.givenLoadMoreReturns(aLoadedPollsStatus)

        // When
        val result = roomPollDataSource.loadMorePolls(A_ROOM_ID)

        // Then
        result shouldBeEqualTo aLoadedPollsStatus
        fakePollHistoryService.verifyLoadMore()
    }

    @Test
    fun `given poll history service when sync polls then correct method of service is called`() = runTest {
        // Given
        val fakePollHistoryService = givenPollHistoryService()
        fakePollHistoryService.givenSyncPollsSuccess()

        // When
        roomPollDataSource.syncPolls(A_ROOM_ID)

        // Then
        fakePollHistoryService.verifySyncPolls()
    }

    private fun givenPollHistoryService(): FakePollHistoryService {
        return fakeActiveSessionHolder
                .fakeSession
                .fakeRoomService
                .getRoom(A_ROOM_ID)
                .pollHistoryService()
    }

    private fun givenALoadedPollsStatus() = LoadedPollsStatus(
            canLoadMore = true,
            daysSynced = 10,
            hasCompletedASyncBackward = true,
    )
}
