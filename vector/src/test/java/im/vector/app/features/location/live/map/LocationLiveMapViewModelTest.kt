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

package im.vector.app.features.location.live.map

import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.location.LocationData
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class LocationLiveMapViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val fakeRoomId = ""

    private val args = LocationLiveMapViewArgs(roomId = fakeRoomId)

    private val getListOfUserLiveLocationUseCase = mockk<GetListOfUserLiveLocationUseCase>()

    private fun createViewModel(): LocationLiveMapViewModel {
        return LocationLiveMapViewModel(
                LocationLiveMapViewState(args),
                getListOfUserLiveLocationUseCase
        )
    }

    @Test
    fun `given the viewModel has been initialized then viewState contains user locations list`() = runTest {
        val userLocations = listOf(
                UserLiveLocationViewState(
                        userId = "",
                        pinDrawable = mockk(),
                        locationData = LocationData(latitude = 1.0, longitude = 2.0, uncertainty = null),
                        endOfLiveTimestampMillis = 123
                )
        )

        every { getListOfUserLiveLocationUseCase.execute(fakeRoomId) } returns flowOf(userLocations)

        val viewModel = createViewModel()
        viewModel
                .test()
                .assertState(
                        LocationLiveMapViewState(args).copy(
                                userLocations = userLocations
                        )
                )
                .finish()
    }
}
