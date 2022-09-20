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

package im.vector.app.features.spaces

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.analytics.plan.Interaction
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeAutoAcceptInvites
import im.vector.app.test.fakes.FakeRoomService
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeSessionAccountDataService
import im.vector.app.test.fakes.FakeSpaceService
import im.vector.app.test.fakes.FakeSpaceStateHandler
import im.vector.app.test.fakes.FakeUserService
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.fixtures.RoomSummaryFixture.aRoomSummary
import im.vector.app.test.fixtures.UserFixture.aUser
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem

internal class SpaceListViewModelTest {

    @get:Rule
    val mvRxTestRule = MvRxTestRule(testDispatcher = UnconfinedTestDispatcher())

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val spaceStateHandler = FakeSpaceStateHandler()
    private val userService = FakeUserService()
    private val accountDataService = FakeSessionAccountDataService()
    private val roomService = FakeRoomService()
    private val spaceService = FakeSpaceService()
    private val session = FakeSession(
            fakeUserService = userService,
            fakeAccountDataService = accountDataService,
            fakeRoomService = roomService,
            fakeSpaceService = spaceService,
    )
    private val vectorPreferences = FakeVectorPreferences()
    private val autoAcceptInvites = FakeAutoAcceptInvites()
    private val analyticsTracker = FakeAnalyticsTracker()

    private lateinit var spaceListViewModel: SpaceListViewModel

    @Before
    fun setup() {
        mockkStatic("org.matrix.android.sdk.flow.FlowSessionKt")
    }

    @Test
    fun `when view model init, then observe user and emit it in state`() = runTest {
        val user = aUser("@userid")
        userService.getUserLiveReturns(user)
        initViewModel()

        val success = spaceListViewModel.awaitState().myMxItem as Success<MatrixItem.UserItem>
        success.invoke().id shouldBeEqualTo user.userId
    }

    @Test
    fun `when view model init, then get space summaries and emit them in state`() = runTest {
        val spaceSummaries = listOf(aRoomSummary("space-1"), aRoomSummary("space-2"))
        givenSpaceSummaries(spaceSummaries)

        initViewModel()

        spaceListViewModel.awaitState().spaces shouldBeEqualTo spaceSummaries
    }

    private fun givenSpaceSummaries(spaceSummaries: List<RoomSummary>) {
        val flowSession = session.givenFlowSession()
        every { flowSession.liveSpaceSummaries(any()) } returns flowOf(spaceSummaries)
        session.accountDataService().getLiveRoomAccountDataEventsReturns(emptyList())
    }

    @Test
    fun `when view model init, then get selected space and emit it in state`() = runTest {
        val currentSpace = aRoomSummary("space-id")
        spaceStateHandler.getSelectedSpaceFlowReturns(currentSpace)
        initViewModel()

        spaceListViewModel.awaitState().selectedSpace shouldBeEqualTo currentSpace
    }

    @Test
    fun `given valid space, when handle SelectSpace, then set and track`() = runTest {
        val spaceSummary = aRoomSummary("space-id")
        initViewModel()

        spaceListViewModel.handle(SpaceListAction.SelectSpace(spaceSummary))

        spaceListViewModel.awaitState().selectedSpace shouldBeEqualTo spaceSummary
        spaceStateHandler.verifySetCurrentSpace(spaceSummary.roomId)
        val interaction = analyticsTracker.verifyCaptureAndGetInteraction()
        interaction shouldBeEqualTo Interaction(null, null, Interaction.Name.SpacePanelSwitchSpace)
    }

    @Test
    fun `given null space, when handle SelectSpace, then track`() = runTest {
        val spaceSummary = null
        initViewModel()

        spaceListViewModel.handle(SpaceListAction.SelectSpace(spaceSummary))

        val interaction = analyticsTracker.verifyCaptureAndGetInteraction()
        interaction shouldBeEqualTo Interaction(null, null, Interaction.Name.SpacePanelSelectedSpace)
    }

    @Test
    fun `when handle LeaveSpace, then do SpaceService leaveSpace`() {
        val spaceSummary = aRoomSummary("space-id")
        initViewModel()

        spaceListViewModel.handle(SpaceListAction.LeaveSpace(spaceSummary))

        spaceService.verifyLeaveSpace(spaceSummary.roomId)
    }

    @Test
    fun `when handle AddSpace, then post AddSpace event`() {
        initViewModel()
        val viewModelTest = spaceListViewModel.test()

        spaceListViewModel.handle(SpaceListAction.AddSpace)

        viewModelTest.assertEvents(SpaceListViewEvents.AddSpace)
    }

    @Test
    fun `when handle ToggleExpand, then update expanded states`() = runTest {
        val spaceSummary = aRoomSummary("space-id")
        initViewModel()

        spaceListViewModel.handle(SpaceListAction.ToggleExpand(spaceSummary))

        spaceListViewModel.awaitState().expandedStates[spaceSummary.roomId] shouldBeEqualTo true
    }

    @Test
    fun `when handle OpenSpaceInvite, then post OpenSpaceInvite event`() {
        val spaceSummary = aRoomSummary("space-id")
        initViewModel()
        val viewModelTest = spaceListViewModel.test()

        spaceListViewModel.handle(SpaceListAction.OpenSpaceInvite(spaceSummary))

        viewModelTest.assertEvents(SpaceListViewEvents.OpenSpaceInvite(spaceSummary.roomId))
    }

    @Test
    fun `when handle StartDragging, then set expanded states to false`() = runTest {
        val spaceSummaries = listOf(aRoomSummary("room1"), aRoomSummary("room2"), aRoomSummary("room3"))
        givenSpaceSummaries(spaceSummaries)
        initViewModel()

        spaceListViewModel.handle(SpaceListAction.ToggleExpand(spaceSummaries[1]))
        spaceListViewModel.handle(SpaceListAction.OnStartDragging(spaceSummaries[1].roomId, true))

        spaceListViewModel.awaitState().expandedStates[spaceSummaries[1].roomId] shouldBeEqualTo false
    }

    @Test
    fun `when handle EndDragging, then set expanded states to normal`() = runTest {
        val spaceSummaries = listOf(aRoomSummary("room1"), aRoomSummary("room2"), aRoomSummary("room3"))
        givenSpaceSummaries(spaceSummaries)
        initViewModel()

        spaceListViewModel.handle(SpaceListAction.ToggleExpand(spaceSummaries[1]))
        spaceListViewModel.handle(SpaceListAction.OnStartDragging(spaceSummaries[1].roomId, true))
        spaceListViewModel.handle(SpaceListAction.OnEndDragging(spaceSummaries[1].roomId, true))

        spaceListViewModel.awaitState().expandedStates[spaceSummaries[1].roomId] shouldBeEqualTo true
    }

    private fun initViewModel() {
        spaceListViewModel = SpaceListViewModel(
                SpaceListViewState(),
                spaceStateHandler,
                session,
                vectorPreferences.instance,
                autoAcceptInvites,
                analyticsTracker,
        )
    }

    @After
    fun teardown() {
        unmockkAll()
    }
}
