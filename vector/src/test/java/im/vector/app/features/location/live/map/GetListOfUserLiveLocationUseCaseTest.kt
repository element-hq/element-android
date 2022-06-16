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

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.location.LocationData
import im.vector.app.test.fakes.FakeSession
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.api.util.MatrixItem

class GetListOfUserLiveLocationUseCaseTest {

    @get:Rule
    val mvRxTestRule = MvRxTestRule()

    private val fakeSession = FakeSession()

    private val viewStateMapper = mockk<UserLiveLocationViewStateMapper>()

    private val getListOfUserLiveLocationUseCase = GetListOfUserLiveLocationUseCase(fakeSession, viewStateMapper)

    @Before
    fun setUp() {
        mockkStatic("androidx.lifecycle.FlowLiveDataConversions")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a room id then the correct flow of view states list is collected`() = runTest {
        val roomId = "roomId"

        val summary1 = LiveLocationShareAggregatedSummary(
                userId = "userId1",
                isActive = true,
                endOfLiveTimestampMillis = 123,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        val summary2 = LiveLocationShareAggregatedSummary(
                userId = "userId2",
                isActive = true,
                endOfLiveTimestampMillis = 1234,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        val summary3 = LiveLocationShareAggregatedSummary(
                userId = "userId3",
                isActive = true,
                endOfLiveTimestampMillis = 1234,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        val summaries = listOf(summary1, summary2, summary3)
        val liveData = fakeSession.roomService()
                .getRoom(roomId)
                .locationSharingService()
                .givenRunningLiveLocationShareSummaries(summaries)

        every { liveData.asFlow() } returns flowOf(summaries)

        val viewState1 = UserLiveLocationViewState(
                matrixItem = MatrixItem.UserItem(id = "@userId1:matrix.org", displayName = "User 1", avatarUrl = ""),
                pinDrawable = mockk(),
                locationData = LocationData(latitude = 1.0, longitude = 2.0, uncertainty = null),
                endOfLiveTimestampMillis = 123,
                locationTimestampMillis = 123,
                showStopSharingButton = false
        )
        val viewState2 = UserLiveLocationViewState(
                matrixItem = MatrixItem.UserItem(id = "@userId2:matrix.org", displayName = "User 2", avatarUrl = ""),
                pinDrawable = mockk(),
                locationData = LocationData(latitude = 1.0, longitude = 2.0, uncertainty = null),
                endOfLiveTimestampMillis = 1234,
                locationTimestampMillis = 1234,
                showStopSharingButton = false
        )
        coEvery { viewStateMapper.map(summary1) } returns viewState1
        coEvery { viewStateMapper.map(summary2) } returns viewState2
        coEvery { viewStateMapper.map(summary3) } returns null

        val viewStates = getListOfUserLiveLocationUseCase.execute(roomId).first()

        assertEquals(listOf(viewState1, viewState2), viewStates)
    }
}
