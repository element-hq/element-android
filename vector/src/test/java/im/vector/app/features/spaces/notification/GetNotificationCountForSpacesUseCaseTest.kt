/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.notification

import androidx.paging.PagedList
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAutoAcceptInvites
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.givenAsFlow
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount

internal class GetNotificationCountForSpacesUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeAutoAcceptInvites = FakeAutoAcceptInvites()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()

    private val getNotificationCountForSpacesUseCase = GetNotificationCountForSpacesUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            autoAcceptInvites = fakeAutoAcceptInvites,
    )

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
        mockkStatic("kotlinx.coroutines.flow.FlowKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given space filter and auto accept invites when execute then correct notification count is returned`() = runTest {
        // given
        val spaceFilter = SpaceFilter.NoFilter
        val pagedList = mockk<PagedList<RoomSummary>>()
        val pagedListFlow = fakeActiveSessionHolder.fakeSession
                .fakeRoomService
                .givenGetPagedRoomSummariesLiveReturns(pagedList)
                .givenAsFlow()
        every { pagedListFlow.sample(any<Long>()) } returns pagedListFlow
        val expectedNotificationCount = RoomAggregateNotificationCount(
                notificationCount = 1,
                highlightCount = 0,
        )
        fakeActiveSessionHolder.fakeSession
                .fakeRoomService
                .givenGetNotificationCountForRoomsReturns(expectedNotificationCount)
        fakeAutoAcceptInvites._isEnabled = true

        // When
        val testObserver = getNotificationCountForSpacesUseCase.execute(spaceFilter).test(this)
        advanceUntilIdle()

        // Then
        testObserver
                .assertValues(expectedNotificationCount)
                .finish()
        verify {
            fakeActiveSessionHolder.fakeSession.fakeRoomService.getNotificationCountForRooms(
                    queryParams = match { it.memberships == listOf(Membership.JOIN) && it.spaceFilter == spaceFilter }
            )
            fakeActiveSessionHolder.fakeSession.fakeRoomService.getPagedRoomSummariesLive(
                    queryParams = match { it.memberships == listOf(Membership.JOIN) && it.spaceFilter == spaceFilter },
                    pagedListConfig = any(),
                    sortOrder = RoomSortOrder.NONE,
            )
        }
    }

    @Test
    fun `given space filter and show invites when execute then correct notification count is returned`() = runTest {
        // given
        val spaceFilter = SpaceFilter.NoFilter
        val pagedList = mockk<PagedList<RoomSummary>>()
        val pagedListFlow = fakeActiveSessionHolder.fakeSession
                .fakeRoomService
                .givenGetPagedRoomSummariesLiveReturns(pagedList)
                .givenAsFlow()
        every { pagedListFlow.sample(any<Long>()) } returns pagedListFlow
        val notificationCount = RoomAggregateNotificationCount(
                notificationCount = 1,
                highlightCount = 0,
        )
        fakeActiveSessionHolder.fakeSession
                .fakeRoomService
                .givenGetNotificationCountForRoomsReturns(notificationCount)
        val invitedRooms = listOf<RoomSummary>(mockk())
        fakeActiveSessionHolder.fakeSession
                .fakeRoomService
                .givenGetRoomSummaries(invitedRooms)
        fakeAutoAcceptInvites._isEnabled = false
        val expectedNotificationCount = RoomAggregateNotificationCount(
                notificationCount = notificationCount.notificationCount + invitedRooms.size,
                highlightCount = notificationCount.highlightCount + invitedRooms.size,
        )

        // When
        val testObserver = getNotificationCountForSpacesUseCase.execute(spaceFilter).test(this)
        advanceUntilIdle()

        // Then
        testObserver
                .assertValues(expectedNotificationCount)
                .finish()
        verify {
            fakeActiveSessionHolder.fakeSession.fakeRoomService.getRoomSummaries(
                    queryParams = match { it.memberships == listOf(Membership.INVITE) }
            )
            fakeActiveSessionHolder.fakeSession.fakeRoomService.getNotificationCountForRooms(
                    queryParams = match { it.memberships == listOf(Membership.JOIN) && it.spaceFilter == spaceFilter }
            )
            fakeActiveSessionHolder.fakeSession.fakeRoomService.getPagedRoomSummariesLive(
                    queryParams = match { it.memberships == listOf(Membership.JOIN) && it.spaceFilter == spaceFilter },
                    pagedListConfig = any(),
                    sortOrder = RoomSortOrder.NONE,
            )
        }
    }

    @Test
    fun `given no active session when execute then empty flow is returned`() = runTest {
        // given
        val spaceFilter = SpaceFilter.NoFilter
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        // When
        val testObserver = getNotificationCountForSpacesUseCase.execute(spaceFilter).test(this)

        // Then
        testObserver
                .assertNoValues()
                .finish()
        verify(inverse = true) {
            fakeActiveSessionHolder.fakeSession.fakeRoomService.getPagedRoomSummariesLive(
                    queryParams = any(),
                    pagedListConfig = any(),
                    sortOrder = any(),
            )
        }
    }
}
