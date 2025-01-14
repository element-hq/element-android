/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.domain

import im.vector.app.features.roomprofile.polls.list.data.RoomPollRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus

class SyncPollsUseCaseTest {

    private val fakeRoomPollRepository = mockk<RoomPollRepository>()
    private val fakeGetLoadedPollsStatusUseCase = mockk<GetLoadedPollsStatusUseCase>()
    private val fakeLoadMorePollsUseCase = mockk<LoadMorePollsUseCase>()

    private val syncPollsUseCase = SyncPollsUseCase(
            roomPollRepository = fakeRoomPollRepository,
            getLoadedPollsStatusUseCase = fakeGetLoadedPollsStatusUseCase,
            loadMorePollsUseCase = fakeLoadMorePollsUseCase,
    )

    @Test
    fun `given it has completed a sync backward when execute then only sync process is called`() = runTest {
        // Given
        val aRoomId = "roomId"
        val aLoadedStatus = LoadedPollsStatus(
                canLoadMore = true,
                daysSynced = 10,
                hasCompletedASyncBackward = true,
        )
        coJustRun { fakeRoomPollRepository.syncPolls(aRoomId) }
        coEvery { fakeGetLoadedPollsStatusUseCase.execute(aRoomId) } returns aLoadedStatus

        // When
        val result = syncPollsUseCase.execute(aRoomId)

        // Then
        result shouldBeEqualTo aLoadedStatus
        coVerifyOrder {
            fakeRoomPollRepository.syncPolls(aRoomId)
            fakeGetLoadedPollsStatusUseCase.execute(aRoomId)
        }
        coVerify(inverse = true) {
            fakeLoadMorePollsUseCase.execute(any())
        }
    }

    @Test
    fun `given it has not completed a sync backward when execute then sync process and load more is called`() = runTest {
        // Given
        val aRoomId = "roomId"
        val aLoadedStatus = LoadedPollsStatus(
                canLoadMore = true,
                daysSynced = 10,
                hasCompletedASyncBackward = false,
        )
        val anUpdatedLoadedStatus = LoadedPollsStatus(
                canLoadMore = true,
                daysSynced = 10,
                hasCompletedASyncBackward = true,
        )
        coJustRun { fakeRoomPollRepository.syncPolls(aRoomId) }
        coEvery { fakeGetLoadedPollsStatusUseCase.execute(aRoomId) } returns aLoadedStatus
        coEvery { fakeLoadMorePollsUseCase.execute(aRoomId) } returns anUpdatedLoadedStatus

        // When
        val result = syncPollsUseCase.execute(aRoomId)

        // Then
        result shouldBeEqualTo anUpdatedLoadedStatus
        coVerifyOrder {
            fakeRoomPollRepository.syncPolls(aRoomId)
            fakeGetLoadedPollsStatusUseCase.execute(aRoomId)
            fakeLoadMorePollsUseCase.execute(aRoomId)
        }
    }
}
