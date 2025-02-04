/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import im.vector.app.features.location.LocationData
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.givenAsFlow
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.api.util.MatrixItem

private const val A_ROOM_ID = "room_id"

class GetListOfUserLiveLocationUseCaseTest {

    private val fakeSession = FakeSession()
    private val viewStateMapper = mockk<UserLiveLocationViewStateMapper>()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()

    private val getListOfUserLiveLocationUseCase = GetListOfUserLiveLocationUseCase(
            session = fakeSession,
            userLiveLocationViewStateMapper = viewStateMapper
    )

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a room id then the correct flow of view states list is collected`() = runTest {
        val summary1 = LiveLocationShareAggregatedSummary(
                roomId = A_ROOM_ID,
                userId = "userId1",
                isActive = true,
                endOfLiveTimestampMillis = 123,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        val summary2 = LiveLocationShareAggregatedSummary(
                roomId = A_ROOM_ID,
                userId = "userId2",
                isActive = true,
                endOfLiveTimestampMillis = 1234,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        val summary3 = LiveLocationShareAggregatedSummary(
                roomId = A_ROOM_ID,
                userId = "userId3",
                isActive = true,
                endOfLiveTimestampMillis = 1234,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        val summaries = listOf(summary1, summary2, summary3)
        fakeSession.roomService()
                .getRoom(A_ROOM_ID)
                .locationSharingService()
                .givenRunningLiveLocationShareSummariesReturns(summaries)
                .givenAsFlow()

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

        val viewStates = getListOfUserLiveLocationUseCase.execute(A_ROOM_ID).first()

        viewStates shouldBeEqualTo listOf(viewState1, viewState2)
    }
}
