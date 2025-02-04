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

class GetLoadedPollsStatusUseCaseTest {

    private val fakeRoomPollRepository = mockk<RoomPollRepository>()

    private val getLoadedPollsStatusUseCase = GetLoadedPollsStatusUseCase(
            roomPollRepository = fakeRoomPollRepository,
    )

    @Test
    fun `given repo when execute then correct method of repo is called`() = runTest {
        // Given
        val aRoomId = "roomId"
        val expectedStatus = LoadedPollsStatus(
                canLoadMore = true,
                daysSynced = 10,
                hasCompletedASyncBackward = true,
        )
        coEvery { fakeRoomPollRepository.getLoadedPollsStatus(aRoomId) } returns expectedStatus

        // When
        val status = getLoadedPollsStatusUseCase.execute(aRoomId)

        // Then
        status shouldBeEqualTo expectedStatus
        coVerify { fakeRoomPollRepository.getLoadedPollsStatus(aRoomId) }
    }
}
