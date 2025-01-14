/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.core.event.GetTimelineEventUseCase
import im.vector.app.features.home.room.detail.poll.VoteToPollUseCase
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val A_POLL_ID = "poll-id"
private const val A_ROOM_ID = "room-id"

internal class RoomPollDetailViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val initialState = RoomPollDetailViewState(pollId = A_POLL_ID, roomId = A_ROOM_ID)
    private val fakeGetTimelineEventUseCase = mockk<GetTimelineEventUseCase>()
    private val fakeRoomPollDetailMapper = mockk<RoomPollDetailMapper>()
    private val fakeVoteToPollUseCase = mockk<VoteToPollUseCase>()

    private fun createViewModel(): RoomPollDetailViewModel {
        return RoomPollDetailViewModel(
                initialState = initialState,
                getTimelineEventUseCase = fakeGetTimelineEventUseCase,
                roomPollDetailMapper = fakeRoomPollDetailMapper,
                voteToPollUseCase = fakeVoteToPollUseCase,
        )
    }

    @Test
    fun `given viewModel when created then poll detail is observed and viewState is updated`() {
        // Given
        val aPollEvent = givenAPollEvent()
        val pollDetail = givenAPollDetail()
        every { fakeGetTimelineEventUseCase.execute(A_ROOM_ID, A_POLL_ID) } returns flowOf(aPollEvent)
        every { fakeRoomPollDetailMapper.map(aPollEvent) } returns pollDetail
        val expectedViewState = initialState.copy(pollDetail = pollDetail)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()

        // Then
        viewModelTest
                .assertLatestState(expectedViewState)
                .finish()
        verify {
            fakeGetTimelineEventUseCase.execute(A_ROOM_ID, A_POLL_ID)
            fakeRoomPollDetailMapper.map(aPollEvent)
        }
    }

    @Test
    fun `given viewModel when handle vote action then correct use case is called`() {
        // Given
        val aPollEvent = givenAPollEvent()
        val pollDetail = givenAPollDetail()
        every { fakeGetTimelineEventUseCase.execute(A_ROOM_ID, A_POLL_ID) } returns flowOf(aPollEvent)
        every { fakeRoomPollDetailMapper.map(aPollEvent) } returns pollDetail
        val viewModel = createViewModel()
        val optionId = "option-id"
        justRun {
            fakeVoteToPollUseCase.execute(
                    roomId = A_ROOM_ID,
                    pollEventId = A_POLL_ID,
                    optionId = optionId,
            )
        }
        val action = RoomPollDetailAction.Vote(
                pollEventId = A_POLL_ID,
                optionId = optionId,
        )

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest.finish()
        verify {
            fakeVoteToPollUseCase.execute(
                    roomId = A_ROOM_ID,
                    pollEventId = A_POLL_ID,
                    optionId = optionId,
            )
        }
    }

    private fun givenAPollEvent(): TimelineEvent {
        return mockk()
    }

    private fun givenAPollDetail(): RoomPollDetail {
        return RoomPollDetail(
                creationTimestamp = 123L,
                isEnded = false,
                endedPollEventId = null,
                pollItemViewState = mockk(),
        )
    }
}
