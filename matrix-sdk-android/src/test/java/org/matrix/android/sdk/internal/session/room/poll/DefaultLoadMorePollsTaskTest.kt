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
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntity
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntityFields
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.FakeTimeline
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindFirst

private const val A_ROOM_ID = "room-id"

/**
 * Timestamp in milliseconds corresponding to 2023/01/26.
 */
private const val A_CURRENT_TIMESTAMP = 1674737619290L

/**
 * Timestamp in milliseconds corresponding to 2023/01/20.
 */
private const val AN_EVENT_TIMESTAMP = 1674169200000L
private const val A_PERIOD_IN_DAYS = 3
private const val A_PAGE_SIZE = 200

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultLoadMorePollsTaskTest {

    private val fakeMonarchy = FakeMonarchy()
    private val fakeTimeline = FakeTimeline()

    private val defaultLoadMorePollsTask = DefaultLoadMorePollsTask(
            monarchy = fakeMonarchy.instance,
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given timeline when execute then more events are fetched in backward direction until has no more to load`() = runTest {
        // Given
        val params = givenTaskParams()
        val oldestEventId = "oldest"
        val pollHistoryStatus = aPollHistoryStatusEntity(
                oldestEventIdReached = oldestEventId,
        )
        fakeMonarchy.fakeRealm
                .givenWhere<PollHistoryStatusEntity>()
                .givenEqualTo(PollHistoryStatusEntityFields.ROOM_ID, A_ROOM_ID)
                .givenFindFirst(pollHistoryStatus)
        fakeTimeline.givenRestartWithEventIdSuccess(oldestEventId)
        val anEventId = "event-id"
        val aTimelineEvent = aTimelineEvent(anEventId, AN_EVENT_TIMESTAMP)
        fakeTimeline.givenAwaitPaginateReturns(
                events = listOf(aTimelineEvent),
                direction = Timeline.Direction.BACKWARDS,
                count = params.eventsPageSize,
        )
        val aPaginationState = aPaginationState(hasMoreToLoad = false)
        fakeTimeline.givenGetPaginationStateReturns(
                paginationState = aPaginationState,
                direction = Timeline.Direction.BACKWARDS,
        )
        val expectedLoadStatus = LoadedPollsStatus(
                canLoadMore = false,
                daysSynced = 6,
                hasCompletedASyncBackward = true,
        )

        // When
        val result = defaultLoadMorePollsTask.execute(params)

        // Then
        coVerifyOrder {
            fakeTimeline.instance.restartWithEventId(oldestEventId)
            fakeTimeline.instance.awaitPaginate(direction = Timeline.Direction.BACKWARDS, count = params.eventsPageSize)
            fakeTimeline.instance.getPaginationState(direction = Timeline.Direction.BACKWARDS)
        }
        pollHistoryStatus.mostRecentEventIdReached shouldBeEqualTo anEventId
        pollHistoryStatus.oldestEventIdReached shouldBeEqualTo anEventId
        pollHistoryStatus.isEndOfPollsBackward shouldBeEqualTo true
        pollHistoryStatus.oldestTimestampTargetReachedMs shouldBeEqualTo AN_EVENT_TIMESTAMP
        result shouldBeEqualTo expectedLoadStatus
    }

    @Test
    fun `given timeline when execute then more events are fetched in backward direction until current target is reached`() = runTest {
        // Given
        val params = givenTaskParams()
        val oldestEventId = "oldest"
        val pollHistoryStatus = aPollHistoryStatusEntity(
                oldestEventIdReached = oldestEventId,
        )
        fakeMonarchy.fakeRealm
                .givenWhere<PollHistoryStatusEntity>()
                .givenEqualTo(PollHistoryStatusEntityFields.ROOM_ID, A_ROOM_ID)
                .givenFindFirst(pollHistoryStatus)
        fakeTimeline.givenRestartWithEventIdSuccess(oldestEventId)
        val anEventId = "event-id"
        val aTimelineEvent = aTimelineEvent(anEventId, AN_EVENT_TIMESTAMP)
        fakeTimeline.givenAwaitPaginateReturns(
                events = listOf(aTimelineEvent),
                direction = Timeline.Direction.BACKWARDS,
                count = params.eventsPageSize,
        )
        val aPaginationState = aPaginationState(hasMoreToLoad = true)
        fakeTimeline.givenGetPaginationStateReturns(
                paginationState = aPaginationState,
                direction = Timeline.Direction.BACKWARDS,
        )
        val expectedLoadStatus = LoadedPollsStatus(
                canLoadMore = true,
                daysSynced = 6,
                hasCompletedASyncBackward = true,
        )

        // When
        val result = defaultLoadMorePollsTask.execute(params)

        // Then
        coVerifyOrder {
            fakeTimeline.instance.restartWithEventId(oldestEventId)
            fakeTimeline.instance.awaitPaginate(direction = Timeline.Direction.BACKWARDS, count = params.eventsPageSize)
            fakeTimeline.instance.getPaginationState(direction = Timeline.Direction.BACKWARDS)
        }
        pollHistoryStatus.mostRecentEventIdReached shouldBeEqualTo anEventId
        pollHistoryStatus.oldestEventIdReached shouldBeEqualTo anEventId
        pollHistoryStatus.isEndOfPollsBackward shouldBeEqualTo false
        pollHistoryStatus.oldestTimestampTargetReachedMs shouldBeEqualTo AN_EVENT_TIMESTAMP
        result shouldBeEqualTo expectedLoadStatus
    }

    private fun givenTaskParams(): LoadMorePollsTask.Params {
        return LoadMorePollsTask.Params(
                timeline = fakeTimeline.instance,
                roomId = A_ROOM_ID,
                currentTimestampMs = A_CURRENT_TIMESTAMP,
                loadingPeriodInDays = A_PERIOD_IN_DAYS,
                eventsPageSize = A_PAGE_SIZE,
        )
    }

    private fun aPollHistoryStatusEntity(
            oldestEventIdReached: String,
    ): PollHistoryStatusEntity {
        return PollHistoryStatusEntity(
                roomId = A_ROOM_ID,
                oldestEventIdReached = oldestEventIdReached,
        )
    }

    private fun aTimelineEvent(eventId: String, timestamp: Long): TimelineEvent {
        val event = mockk<TimelineEvent>()
        every { event.root.originServerTs } returns timestamp
        every { event.root.eventId } returns eventId
        return event
    }

    private fun aPaginationState(hasMoreToLoad: Boolean): Timeline.PaginationState {
        return Timeline.PaginationState(
                hasMoreToLoad = hasMoreToLoad,
        )
    }
}
