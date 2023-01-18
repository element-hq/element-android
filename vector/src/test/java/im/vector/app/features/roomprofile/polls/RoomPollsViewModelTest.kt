/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.roomprofile.polls.list.data.LoadedPollsStatus
import im.vector.app.features.roomprofile.polls.list.domain.GetLoadedPollsStatusUseCase
import im.vector.app.features.roomprofile.polls.list.domain.GetPollsUseCase
import im.vector.app.features.roomprofile.polls.list.domain.LoadMorePollsUseCase
import im.vector.app.features.roomprofile.polls.list.domain.SyncPollsUseCase
import im.vector.app.features.roomprofile.polls.list.ui.PollSummary
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

private const val A_ROOM_ID = "room-id"

class RoomPollsViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val fakeGetPollsUseCase = mockk<GetPollsUseCase>()
    private val fakeGetLoadedPollsStatusUseCase = mockk<GetLoadedPollsStatusUseCase>()
    private val fakeLoadMorePollsUseCase = mockk<LoadMorePollsUseCase>()
    private val fakeSyncPollsUseCase = mockk<SyncPollsUseCase>()
    private val initialState = RoomPollsViewState(A_ROOM_ID)

    private fun createViewModel(): RoomPollsViewModel {
        return RoomPollsViewModel(
                initialState = initialState,
                getPollsUseCase = fakeGetPollsUseCase,
                getLoadedPollsStatusUseCase = fakeGetLoadedPollsStatusUseCase,
                loadMorePollsUseCase = fakeLoadMorePollsUseCase,
                syncPollsUseCase = fakeSyncPollsUseCase,
        )
    }

    @Test
    fun `given viewModel when created then polls list is observed, sync is launched and viewState is updated`() {
        // Given
        val loadedPollsStatus = givenGetLoadedPollsStatusSuccess()
        givenSyncPollsWithSuccess()
        val polls = listOf(givenAPollSummary())
        every { fakeGetPollsUseCase.execute(A_ROOM_ID) } returns flowOf(polls)
        val expectedViewState = initialState.copy(
                polls = polls,
                canLoadMore = loadedPollsStatus.canLoadMore,
                nbLoadedDays = loadedPollsStatus.nbLoadedDays,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()

        // Then
        viewModelTest
                .assertLatestState(expectedViewState)
                .finish()
        verify {
            fakeGetPollsUseCase.execute(A_ROOM_ID)
        }
        coVerify { fakeSyncPollsUseCase.execute(A_ROOM_ID) }
    }

    @Test
    fun `given viewModel and error during sync process when created then error is raised in view event`() {
        // Given
        givenGetLoadedPollsStatusSuccess()
        givenSyncPollsWithError(Exception())
        val polls = listOf(givenAPollSummary())
        every { fakeGetPollsUseCase.execute(A_ROOM_ID) } returns flowOf(polls)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()

        // Then
        viewModelTest
                .assertEvents(RoomPollsViewEvent.LoadingError)
                .finish()
        coVerify { fakeSyncPollsUseCase.execute(A_ROOM_ID) }
    }

    @Test
    fun `given viewModel when handle load more action then viewState is updated`() {
        // Given
        val loadedPollsStatus = givenGetLoadedPollsStatusSuccess()
        givenSyncPollsWithSuccess()
        val polls = listOf(givenAPollSummary())
        every { fakeGetPollsUseCase.execute(A_ROOM_ID) } returns flowOf(polls)
        val newLoadedPollsStatus = givenLoadMoreWithSuccess()
        val viewModel = createViewModel()
        val stateAfterInit = initialState.copy(
                polls = polls,
                canLoadMore = loadedPollsStatus.canLoadMore,
                nbLoadedDays = loadedPollsStatus.nbLoadedDays,
        )

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(RoomPollsAction.LoadMorePolls)

        // Then
        viewModelTest
                .assertStatesChanges(
                        stateAfterInit,
                        { copy(isLoadingMore = true) },
                        { copy(canLoadMore = newLoadedPollsStatus.canLoadMore, nbLoadedDays = newLoadedPollsStatus.nbLoadedDays) },
                        { copy(isLoadingMore = false) },
                )
                .finish()
        coVerify { fakeLoadMorePollsUseCase.execute(A_ROOM_ID) }
    }

    private fun givenAPollSummary(): PollSummary {
        return mockk()
    }

    private fun givenSyncPollsWithSuccess() {
        coJustRun { fakeSyncPollsUseCase.execute(A_ROOM_ID) }
    }

    private fun givenSyncPollsWithError(error: Exception) {
        coEvery { fakeSyncPollsUseCase.execute(A_ROOM_ID) } throws error
    }

    private fun givenLoadMoreWithSuccess(): LoadedPollsStatus {
        val loadedPollsStatus = givenALoadedPollsStatus(canLoadMore = false, nbLoadedDays = 20)
        coEvery { fakeLoadMorePollsUseCase.execute(A_ROOM_ID) } returns loadedPollsStatus
        return loadedPollsStatus
    }

    private fun givenGetLoadedPollsStatusSuccess(): LoadedPollsStatus {
        val loadedPollsStatus = givenALoadedPollsStatus()
        every { fakeGetLoadedPollsStatusUseCase.execute(A_ROOM_ID) } returns loadedPollsStatus
        return loadedPollsStatus
    }

    private fun givenALoadedPollsStatus(canLoadMore: Boolean = true, nbLoadedDays: Int = 10) =
            LoadedPollsStatus(
                    canLoadMore = canLoadMore,
                    nbLoadedDays = nbLoadedDays,
            )
}
