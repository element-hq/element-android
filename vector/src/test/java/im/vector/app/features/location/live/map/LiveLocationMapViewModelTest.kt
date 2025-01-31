/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.live.StopLiveLocationShareUseCase
import im.vector.app.test.fakes.FakeLocationSharingServiceConnection
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.util.MatrixItem

private const val A_ROOM_ID = "room_id"

class LiveLocationMapViewModelTest {

    @get:Rule
    val mvRxTestRule = MvRxTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val args = LiveLocationMapViewArgs(roomId = A_ROOM_ID)

    private val getListOfUserLiveLocationUseCase = mockk<GetListOfUserLiveLocationUseCase>()
    private val locationServiceConnection = FakeLocationSharingServiceConnection()
    private val stopLiveLocationShareUseCase = mockk<StopLiveLocationShareUseCase>()

    private fun createViewModel(): LiveLocationMapViewModel {
        return LiveLocationMapViewModel(
                LiveLocationMapViewState(args),
                getListOfUserLiveLocationUseCase,
                locationServiceConnection.instance,
                stopLiveLocationShareUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel has been initialized then viewState contains user locations list`() = runTest {
        val userLocations = listOf(
                UserLiveLocationViewState(
                        MatrixItem.UserItem(id = "@userId1:matrix.org", displayName = "User 1", avatarUrl = ""),
                        pinDrawable = mockk(),
                        locationData = LocationData(latitude = 1.0, longitude = 2.0, uncertainty = null),
                        endOfLiveTimestampMillis = 123,
                        locationTimestampMillis = 123,
                        showStopSharingButton = false
                )
        )
        locationServiceConnection.givenBind()
        every { getListOfUserLiveLocationUseCase.execute(A_ROOM_ID) } returns flowOf(userLocations)

        val viewModel = createViewModel()
        viewModel
                .test()
                .assertState(
                        LiveLocationMapViewState(args).copy(
                                userLocations = userLocations
                        )
                )
                .finish()

        locationServiceConnection.verifyBind(viewModel)
    }
}
