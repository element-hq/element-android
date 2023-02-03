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

package im.vector.app.features.home

import android.widget.ImageView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.R
import im.vector.app.core.platform.StateView
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.room.list.home.HomeRoomListAction
import im.vector.app.features.home.room.list.home.HomeRoomListViewModel
import im.vector.app.features.home.room.list.home.HomeRoomListViewState
import im.vector.app.features.home.room.list.home.header.HomeRoomFilter
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeDrawableProvider
import im.vector.app.test.fakes.FakeHomeLayoutPreferencesStore
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeSpaceStateHandler
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fixtures.RoomSummaryFixture.aRoomSummary
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.FlowSession

class RoomsListViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    @get:Rule
    var rule = InstantTaskExecutorRule()

    private val fakeSession = FakeSession()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker()
    private val fakeStringProvider = FakeStringProvider()
    private val fakeDrawableProvider = FakeDrawableProvider()
    private val fakeSpaceStateHandler = FakeSpaceStateHandler()
    private val fakeHomeLayoutPreferencesStore = FakeHomeLayoutPreferencesStore()

    private var initialState = HomeRoomListViewState()
    private lateinit var viewModel: HomeRoomListViewModel
    private lateinit var fakeFLowSession: FlowSession

    @Before
    fun setUp() {
        mockkStatic("org.matrix.android.sdk.flow.FlowSessionKt")
        fakeFLowSession = fakeSession.givenFlowSession()

        every { fakeSpaceStateHandler.getSelectedSpaceFlow() } returns flowOf(Optional.empty())
        every { fakeSpaceStateHandler.getCurrentSpace() } returns null
        every { fakeFLowSession.liveRoomSummaries(any(), any()) } returns flowOf(emptyList())

        val roomA = aRoomSummary("room_a")
        val roomB = aRoomSummary("room_b")
        val roomC = aRoomSummary("room_c")
        val allRooms = listOf(roomA, roomB, roomC)

        every {
            fakeFLowSession.liveRoomSummaries(
                    match {
                        it.roomCategoryFilter == null &&
                                it.roomTagQueryFilter == null &&
                                it.memberships == listOf(Membership.JOIN) &&
                                it.spaceFilter is SpaceFilter.NoFilter
                    }, any()
            )
        } returns flowOf(allRooms)

        viewModelWith(initialState)
    }

    @Test
    fun `when recents are enabled then updates state`() = runTest {
        val fakeFLowSession = fakeSession.givenFlowSession()
        every { fakeFLowSession.liveRoomSummaries(any()) } returns flowOf(emptyList())
        val test = viewModel.test()

        val roomA = aRoomSummary("room_a")
        val roomB = aRoomSummary("room_b")
        val roomC = aRoomSummary("room_c")
        val recentRooms = listOf(roomA, roomB, roomC)

        every { fakeFLowSession.liveBreadcrumbs(any()) } returns flowOf(recentRooms)
        fakeHomeLayoutPreferencesStore.givenRecentsEnabled(true)

        val userName = fakeSession.getUserOrDefault(fakeSession.myUserId).toMatrixItem().getBestName()
        val allEmptyState = StateView.State.Empty(
                title = fakeStringProvider.instance.getString(R.string.home_empty_no_rooms_title, userName),
                message = fakeStringProvider.instance.getString(R.string.home_empty_no_rooms_message),
                image = fakeDrawableProvider.instance.getDrawable(R.drawable.ill_empty_all_chats),
                isBigImage = true
        )

        test.assertLatestState(
                initialState.copy(emptyState = allEmptyState, headersData = initialState.headersData.copy(recents = recentRooms))
        )
    }

    @Test
    fun `when filter tabs are enabled then updates state`() = runTest {
        val test = viewModel.test()

        fakeHomeLayoutPreferencesStore.givenFiltersEnabled(true)

        val filtersData = mutableListOf(
                HomeRoomFilter.ALL,
                HomeRoomFilter.UNREADS
        )

        val userName = fakeSession.getUserOrDefault(fakeSession.myUserId).toMatrixItem().getBestName()
        val allEmptyState = StateView.State.Empty(
                title = fakeStringProvider.instance.getString(R.string.home_empty_no_rooms_title, userName),
                message = fakeStringProvider.instance.getString(R.string.home_empty_no_rooms_message),
                image = fakeDrawableProvider.instance.getDrawable(R.drawable.ill_empty_all_chats),
                isBigImage = true
        )

        test.assertLatestState(
                initialState.copy(emptyState = allEmptyState, headersData = initialState.headersData.copy(filtersList = filtersData))
        )
    }

    @Test
    fun `when filter tab is selected then updates state`() = runTest {
        val test = viewModel.test()

        val aFilter = HomeRoomFilter.UNREADS
        viewModel.handle(HomeRoomListAction.ChangeRoomFilter(filter = aFilter))

        val unreadsEmptyState = StateView.State.Empty(
                title = fakeStringProvider.instance.getString(R.string.home_empty_no_unreads_title),
                message = fakeStringProvider.instance.getString(R.string.home_empty_no_unreads_message),
                image = fakeDrawableProvider.instance.getDrawable(R.drawable.ill_empty_unreads),
                isBigImage = true,
                imageScaleType = ImageView.ScaleType.CENTER_INSIDE
        )

        test.assertLatestState(
                initialState.copy(emptyState = unreadsEmptyState, headersData = initialState.headersData.copy(currentFilter = aFilter))
        )
    }

    private fun viewModelWith(state: HomeRoomListViewState) {
        HomeRoomListViewModel(
                state,
                session = fakeSession,
                spaceStateHandler = fakeSpaceStateHandler,
                preferencesStore = fakeHomeLayoutPreferencesStore.instance,
                stringProvider = fakeStringProvider.instance,
                drawableProvider = fakeDrawableProvider.instance,
                analyticsTracker = fakeAnalyticsTracker

        ).also {
            viewModel = it
            initialState = state
        }
    }
}
