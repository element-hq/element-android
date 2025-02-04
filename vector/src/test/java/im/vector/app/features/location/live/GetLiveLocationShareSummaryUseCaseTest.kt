/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.givenAsFlow
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"

class GetLiveLocationShareSummaryUseCaseTest {

    private val fakeSession = FakeSession()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()

    private val getLiveLocationShareSummaryUseCase = GetLiveLocationShareSummaryUseCase(
            session = fakeSession
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
    fun `given a room id and event id when calling use case then flow on summary is returned`() = runTest {
        val summary = LiveLocationShareAggregatedSummary(
                roomId = A_ROOM_ID,
                userId = "userId",
                isActive = true,
                endOfLiveTimestampMillis = 123,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        fakeSession.roomService()
                .getRoom(A_ROOM_ID)
                .locationSharingService()
                .givenLiveLocationShareSummaryReturns(AN_EVENT_ID, summary)
                .givenAsFlow()

        val result = getLiveLocationShareSummaryUseCase.execute(A_ROOM_ID, AN_EVENT_ID).first()

        result shouldBeEqualTo summary
    }

    @Test
    fun `given a room id, event id and a null summary when calling use case then null is emitted in the flow`() = runTest {
        fakeSession.roomService()
                .getRoom(A_ROOM_ID)
                .locationSharingService()
                .givenLiveLocationShareSummaryReturns(AN_EVENT_ID, null)
                .givenAsFlow()

        val result = getLiveLocationShareSummaryUseCase.execute(A_ROOM_ID, AN_EVENT_ID).first()

        result shouldBeEqualTo null
    }
}
