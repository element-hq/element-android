/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

internal class NewHomeDetailViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val initialState = NewHomeDetailViewState()
    private val fakeGetSpacesNotificationBadgeStateUseCase = mockk<GetSpacesNotificationBadgeStateUseCase>()

    private fun createViewModel(): NewHomeDetailViewModel {
        return NewHomeDetailViewModel(
                initialState = initialState,
                getSpacesNotificationBadgeStateUseCase = fakeGetSpacesNotificationBadgeStateUseCase,
        )
    }

    @Test
    fun `given the viewModel is created then viewState is updated with space notifications badge state`() {
        // Given
        val aBadgeState = UnreadCounterBadgeView.State.Count(count = 1, highlighted = false)
        every { fakeGetSpacesNotificationBadgeStateUseCase.execute() } returns flowOf(aBadgeState)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()

        // Then
        val expectedViewState = initialState.copy(
                spacesNotificationCounterBadgeState = aBadgeState,
        )
        viewModelTest
                .assertLatestState(expectedViewState)
                .finish()
        verify {
            fakeGetSpacesNotificationBadgeStateUseCase.execute()
        }
    }
}
