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

package im.vector.app.features.home

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.spaces.GetSpacesUseCase
import im.vector.app.features.spaces.notification.GetNotificationCountForSpacesUseCase
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount

internal class NewHomeDetailViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val initialState = NewHomeDetailViewState()
    private val fakeGetNotificationCountForSpacesUseCase = mockk<GetNotificationCountForSpacesUseCase>()
    private val fakeGetSpacesUseCase = mockk<GetSpacesUseCase>()

    private fun createViewModel(): NewHomeDetailViewModel {
        return NewHomeDetailViewModel(
                initialState = initialState,
                getNotificationCountForSpacesUseCase = fakeGetNotificationCountForSpacesUseCase,
                getSpacesUseCase = fakeGetSpacesUseCase,
        )
    }

    @Test
    fun `given the viewModel is created then viewState is updated with space notifications count and pending space invites`() {
        // Given
        val spacesNotificationCount = RoomAggregateNotificationCount(
                notificationCount = 1,
                highlightCount = 1,
        )
        every { fakeGetNotificationCountForSpacesUseCase.execute(any()) } returns flowOf(spacesNotificationCount)
        val spaceInvites = listOf<RoomSummary>(mockk())
        every { fakeGetSpacesUseCase.execute(any()) } returns flowOf(spaceInvites)
        val expectedViewState = initialState.copy(
                spacesNotificationCount = spacesNotificationCount,
                hasPendingSpaceInvites = true,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()

        // Then
        viewModelTest
                .assertLatestState(expectedViewState)
                .finish()
        verify {
            fakeGetNotificationCountForSpacesUseCase.execute(SpaceFilter.NoFilter)
            fakeGetSpacesUseCase.execute(match { it.memberships == listOf(Membership.INVITE) })
        }
    }
}
