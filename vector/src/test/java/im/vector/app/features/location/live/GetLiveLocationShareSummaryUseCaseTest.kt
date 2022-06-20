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

import androidx.lifecycle.asFlow
import im.vector.app.test.fakes.FakeSession
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.api.util.Optional

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"

class GetLiveLocationShareSummaryUseCaseTest {

    private val fakeSession = FakeSession()

    private val getLiveLocationShareSummaryUseCase = GetLiveLocationShareSummaryUseCase(
            session = fakeSession
    )

    @Before
    fun setUp() {
        mockkStatic("androidx.lifecycle.FlowLiveDataConversions")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a room id and event id when calling use case then live data on summary is returned`() = runTest {
        val summary = LiveLocationShareAggregatedSummary(
                userId = "userId",
                isActive = true,
                endOfLiveTimestampMillis = 123,
                lastLocationDataContent = MessageBeaconLocationDataContent()
        )
        val liveData = fakeSession.roomService()
                .getRoom(A_ROOM_ID)
                .locationSharingService()
                .givenLiveLocationShareSummaryReturns(AN_EVENT_ID, summary)
        every { liveData.asFlow() } returns flowOf(Optional(summary))

        val result = getLiveLocationShareSummaryUseCase.execute(A_ROOM_ID, AN_EVENT_ID).first()

        result shouldBeEqualTo summary
    }
}
