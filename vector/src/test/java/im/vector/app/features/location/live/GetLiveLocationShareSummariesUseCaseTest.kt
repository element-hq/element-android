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

private const val A_ROOM_ID = "room_id"

class GetLiveLocationShareSummariesUseCaseTest {

    private val fakeSession = FakeSession()

    private val getLiveLocationShareSummariesUseCase = GetLiveLocationShareSummariesUseCase(
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
    fun `given a room id when calling use case then the current live is stopped with success`() = runTest {
        val eventIds = listOf("event_id_1", "event_id_2", "event_id_3")
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
                .getRoom(A_ROOM_ID)
                .locationSharingService()
                .givenLiveLocationShareSummaries(eventIds, summaries)
        every { liveData.asFlow() } returns flowOf(summaries)

        val result = getLiveLocationShareSummariesUseCase.execute(A_ROOM_ID, eventIds).first()

        result shouldBeEqualTo listOf(summary1, summary2, summary3)
    }
}
