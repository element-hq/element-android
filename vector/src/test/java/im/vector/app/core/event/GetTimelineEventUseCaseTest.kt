/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.core.event

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.givenAsFlow
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val A_ROOM_ID = "room-id"
private const val AN_EVENT_ID = "event-id"

internal class GetTimelineEventUseCaseTest {

    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val getTimelineEventUseCase = GetTimelineEventUseCase(fakeActiveSessionHolder.instance)

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a non existing room id, when execute, then returns an empty flow`() = runTest {
        // Given
        every { fakeActiveSessionHolder.instance.getActiveSession().roomService().getRoom(A_ROOM_ID) } returns null

        // When
        val result = getTimelineEventUseCase.execute(A_ROOM_ID, AN_EVENT_ID).firstOrNull()

        // Then
        result.shouldBeNull()
    }

    @Test
    fun `given a LiveData of TimelineEvent, when execute, then returns the expected Flow`() = runTest {
        // Given
        val aTimelineEvent1: TimelineEvent = mockk()
        val aTimelineEvent2: TimelineEvent? = null
        val timelineService = fakeActiveSessionHolder.fakeSession
                .fakeRoomService
                .getRoom(A_ROOM_ID)
                .timelineService()

        // When
        timelineService.givenTimelineEventLiveReturns(AN_EVENT_ID, aTimelineEvent1).givenAsFlow()
        val result1 = getTimelineEventUseCase.execute(A_ROOM_ID, AN_EVENT_ID).test(this)

        timelineService.givenTimelineEventLiveReturns(AN_EVENT_ID, aTimelineEvent2).givenAsFlow()
        val result2 = getTimelineEventUseCase.execute(A_ROOM_ID, AN_EVENT_ID).test(this)

        // Then
        runCurrent()
        result1.assertLatestValue(aTimelineEvent1)
        result2.assertNoValues()
    }
}
