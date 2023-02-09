/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.poll

import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntity
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntityFields
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.FakeTimeline
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindFirst

private const val A_ROOM_ID = "room-id"
private const val A_TIMESTAMP = 123L
private const val A_PAGE_SIZE = 200

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSyncPollsTaskTest {

    private val fakeMonarchy = FakeMonarchy()
    private val fakeTimeline = FakeTimeline()

    private val defaultSyncPollsTask = DefaultSyncPollsTask(
            monarchy = fakeMonarchy.instance,
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given timeline when execute then more events are fetched in forward direction after the most recent event id reached`() = runTest {
        // Given
        val params = givenTaskParams()
        val mostRecentEventId = "most-recent"
        val oldestEventId = "oldest"
        val pollHistoryStatus = aPollHistoryStatusEntity(
                mostRecentEventIdReached = mostRecentEventId,
                oldestEventIdReached = oldestEventId,
        )
        fakeMonarchy.fakeRealm
                .givenWhere<PollHistoryStatusEntity>()
                .givenEqualTo(PollHistoryStatusEntityFields.ROOM_ID, A_ROOM_ID)
                .givenFindFirst(pollHistoryStatus)
        fakeTimeline.givenRestartWithEventIdSuccess(mostRecentEventId)
        fakeTimeline.givenRestartWithEventIdSuccess(oldestEventId)
        val anEventId = "event-id"
        val aTimelineEvent = aTimelineEvent(anEventId)
        fakeTimeline.givenAwaitPaginateReturns(
                events = listOf(aTimelineEvent),
                direction = Timeline.Direction.FORWARDS,
                count = params.eventsPageSize,
        )
        fakeTimeline.givenGetPaginationStateReturns(
                paginationState = aPaginationState(),
                direction = Timeline.Direction.FORWARDS,
        )

        // When
        defaultSyncPollsTask.execute(params)

        // Then
        coVerifyOrder {
            fakeTimeline.instance.restartWithEventId(mostRecentEventId)
            fakeTimeline.instance.awaitPaginate(direction = Timeline.Direction.FORWARDS, count = params.eventsPageSize)
            fakeTimeline.instance.getPaginationState(direction = Timeline.Direction.FORWARDS)
            fakeTimeline.instance.restartWithEventId(oldestEventId)
        }
        pollHistoryStatus.mostRecentEventIdReached shouldBeEqualTo anEventId
    }

    private fun givenTaskParams(): SyncPollsTask.Params {
        return SyncPollsTask.Params(
                timeline = fakeTimeline.instance,
                roomId = A_ROOM_ID,
                currentTimestampMs = A_TIMESTAMP,
                eventsPageSize = A_PAGE_SIZE,
        )
    }

    private fun aPollHistoryStatusEntity(
            mostRecentEventIdReached: String,
            oldestEventIdReached: String,
    ): PollHistoryStatusEntity {
        return PollHistoryStatusEntity(
                roomId = A_ROOM_ID,
                mostRecentEventIdReached = mostRecentEventIdReached,
                oldestEventIdReached = oldestEventIdReached,
        )
    }

    private fun aTimelineEvent(eventId: String): TimelineEvent {
        val event = mockk<TimelineEvent>()
        every { event.root.originServerTs } returns 123L
        every { event.root.eventId } returns eventId
        return event
    }

    private fun aPaginationState(): Timeline.PaginationState {
        return Timeline.PaginationState(
                hasMoreToLoad = false,
        )
    }
}
