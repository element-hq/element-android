/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.domain

import im.vector.app.features.roomprofile.polls.list.data.RoomPollRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus

class LoadMorePollsUseCaseTest {

    private val fakeRoomPollRepository = mockk<RoomPollRepository>()

    private val loadMorePollsUseCase = LoadMorePollsUseCase(
            roomPollRepository = fakeRoomPollRepository,
    )

    @Test
    fun `given repo when execute then correct method of repo is called`() = runTest {
        // Given
        val aRoomId = "roomId"
        val loadedPollsStatus = LoadedPollsStatus(
                canLoadMore = true,
                daysSynced = 10,
                hasCompletedASyncBackward = true,
        )
        coEvery { fakeRoomPollRepository.loadMorePolls(aRoomId) } returns loadedPollsStatus

        // When
        val result = loadMorePollsUseCase.execute(aRoomId)

        // Then
        result shouldBeEqualTo loadedPollsStatus
        coVerify { fakeRoomPollRepository.loadMorePolls(aRoomId) }
    }
}
