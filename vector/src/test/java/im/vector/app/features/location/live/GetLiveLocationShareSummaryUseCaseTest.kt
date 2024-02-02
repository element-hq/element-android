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
