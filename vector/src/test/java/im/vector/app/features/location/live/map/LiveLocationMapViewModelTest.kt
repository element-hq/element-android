/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.live.StopLiveLocationShareUseCase
import im.vector.app.test.fakes.FakeLocationSharingServiceConnection
import im.vector.app.test.fakes.FakeLocationTracker
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.util.MatrixItem

private const val A_ROOM_ID = "room_id"

class LiveLocationMapViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val args = LiveLocationMapViewArgs(roomId = A_ROOM_ID)

    private val fakeSession = FakeSession()
    private val fakeGetListOfUserLiveLocationUseCase = mockk<GetListOfUserLiveLocationUseCase>()
    private val fakeLocationSharingServiceConnection = FakeLocationSharingServiceConnection()
    private val fakeStopLiveLocationShareUseCase = mockk<StopLiveLocationShareUseCase>()
    private val fakeLocationTracker = FakeLocationTracker()

    private fun createViewModel(): LiveLocationMapViewModel {
        return LiveLocationMapViewModel(
                LiveLocationMapViewState(args),
                session = fakeSession,
                getListOfUserLiveLocationUseCase = fakeGetListOfUserLiveLocationUseCase,
                locationSharingServiceConnection = fakeLocationSharingServiceConnection.instance,
                stopLiveLocationShareUseCase = fakeStopLiveLocationShareUseCase,
                locationTracker = fakeLocationTracker.instance,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel has been initialized then viewState contains user locations list and location tracker is setup`() {
        // Given
        val userLocations = listOf(givenAUserLiveLocationViewState(userId = "@userId1:matrix.org"))
        fakeLocationSharingServiceConnection.givenBind()
        every { fakeGetListOfUserLiveLocationUseCase.execute(A_ROOM_ID) } returns flowOf(userLocations)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()

        // Then
        viewModelTest
                .assertState(
                        LiveLocationMapViewState(args).copy(
                                userLocations = userLocations,
                                showLocateUserButton = true,
                        )
                ).finish()
        fakeLocationSharingServiceConnection.verifyBind(viewModel)
        fakeLocationTracker.verifyAddCallback(viewModel)
    }

    @Test
    fun `given the viewModel when it is cleared cleanUp are done`() {
        // Given
        fakeLocationSharingServiceConnection.givenBind()
        fakeLocationSharingServiceConnection.givenUnbind()
        every { fakeGetListOfUserLiveLocationUseCase.execute(A_ROOM_ID) } returns flowOf(emptyList())
        val viewModel = createViewModel()

        // When
        viewModel.onCleared()

        // Then
        fakeLocationSharingServiceConnection.verifyUnbind(viewModel)
        fakeLocationTracker.verifyRemoveCallback(viewModel)
    }

    @Test
    fun `given current user shares their live location then locate button should not be shown`() {
        // Given
        val userLocations = listOf(givenAUserLiveLocationViewState(userId = fakeSession.myUserId))
        fakeLocationSharingServiceConnection.givenBind()
        every { fakeGetListOfUserLiveLocationUseCase.execute(A_ROOM_ID) } returns flowOf(userLocations)
        val viewModel = createViewModel()

        // When
        val viewModelTest = viewModel.test()

        // Then
        viewModelTest
                .assertState(
                        LiveLocationMapViewState(args).copy(
                                userLocations = userLocations,
                                showLocateUserButton = false,
                        )
                )
                .finish()
    }

    @Test
    fun `given current user does not share their live location then locate button should be shown`() {
        // Given
        val userLocations = listOf(givenAUserLiveLocationViewState(userId = "@userId1:matrix.org"))
        fakeLocationSharingServiceConnection.givenBind()
        every { fakeGetListOfUserLiveLocationUseCase.execute(A_ROOM_ID) } returns flowOf(userLocations)
        val viewModel = createViewModel()

        // When
        val viewModelTest = viewModel.test()

        // Then
        viewModelTest
                .assertState(
                        LiveLocationMapViewState(args).copy(
                                userLocations = userLocations,
                                showLocateUserButton = true,
                        )
                )
                .finish()
    }

    private fun givenAUserLiveLocationViewState(userId: String) = UserLiveLocationViewState(
            MatrixItem.UserItem(id = userId, displayName = "User 1", avatarUrl = ""),
            pinDrawable = mockk(),
            locationData = LocationData(latitude = 1.0, longitude = 2.0, uncertainty = null),
            endOfLiveTimestampMillis = 123,
            locationTimestampMillis = 123,
            showStopSharingButton = false
    )
}
