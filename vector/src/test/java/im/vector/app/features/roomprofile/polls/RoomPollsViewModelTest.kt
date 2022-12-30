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
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.Rule
import org.junit.Test

private const val ROOM_ID = "room-id"

class RoomPollsViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val fakeGetPollsUseCase = mockk<GetPollsUseCase>()
    private val initialState = RoomPollsViewState(ROOM_ID)

    private fun createViewModel(): RoomPollsViewModel {
        return RoomPollsViewModel(
                initialState = initialState,
                getPollsUseCase = fakeGetPollsUseCase,
        )
    }

    @Test
    fun `given SetFilter action when handle then useCase is called with given filter and viewState is updated`() {
        // Given
        val filter = RoomPollsFilter.ACTIVE
        val action = RoomPollsAction.SetFilter(filter = filter)
        val polls = listOf(givenAPollSummary())
        every { fakeGetPollsUseCase.execute(any()) } returns flowOf(polls)
        val viewModel = createViewModel()
        val expectedViewState = initialState.copy(polls = polls)

        // When
        val viewModelTest = viewModel.test()
        viewModel.pollsCollectionJob = null
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertLatestState(expectedViewState)
                .finish()
        viewModel.pollsCollectionJob.shouldNotBeNull()
        verify {
            viewModel.pollsCollectionJob?.cancel()
            fakeGetPollsUseCase.execute(filter)
        }
    }

    private fun givenAPollSummary(): PollSummary {
        return mockk()
    }
}
