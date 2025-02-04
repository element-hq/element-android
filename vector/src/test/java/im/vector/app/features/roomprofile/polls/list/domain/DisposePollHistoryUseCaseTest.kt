/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.domain

import im.vector.app.features.roomprofile.polls.list.data.RoomPollRepository
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Test

internal class DisposePollHistoryUseCaseTest {

    private val fakeRoomPollRepository = mockk<RoomPollRepository>()

    private val disposePollHistoryUseCase = DisposePollHistoryUseCase(
            roomPollRepository = fakeRoomPollRepository,
    )

    @Test
    fun `given repo when execute then correct method of repo is called`() {
        // Given
        val aRoomId = "roomId"
        justRun { fakeRoomPollRepository.dispose(aRoomId) }

        // When
        disposePollHistoryUseCase.execute(aRoomId)

        // Then
        coVerify { fakeRoomPollRepository.dispose(aRoomId) }
    }
}
