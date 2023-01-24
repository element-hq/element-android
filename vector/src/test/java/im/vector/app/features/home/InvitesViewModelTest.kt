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

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.home.room.list.home.invites.InvitesAction
import im.vector.app.features.home.room.list.home.invites.InvitesViewEvents
import im.vector.app.features.home.room.list.home.invites.InvitesViewModel
import im.vector.app.features.home.room.list.home.invites.InvitesViewState
import im.vector.app.test.fakes.FakeDrawableProvider
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fixtures.RoomSummaryFixture
import im.vector.app.test.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.Membership

class InvitesViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val fakeSession = FakeSession()
    private val fakeStringProvider = FakeStringProvider()
    private val fakeDrawableProvider = FakeDrawableProvider()

    private var initialState = InvitesViewState()
    private lateinit var viewModel: InvitesViewModel

    private val anInvite = RoomSummaryFixture.aRoomSummary("invite")

    @Before
    fun setUp() {
        mockkStatic("org.matrix.android.sdk.flow.FlowSessionKt")

        every {
            fakeSession.fakeRoomService.getPagedRoomSummariesLive(
                    queryParams = match {
                        it.memberships == listOf(Membership.INVITE)
                    },
                    pagedListConfig = any(),
                    sortOrder = any()
            )
        } returns mockk()

        viewModelWith(initialState)
    }

    @Test
    fun `when invite accepted then membership map is updated and open event posted`() = runTest {
        val test = viewModel.test()

        viewModel.handle(InvitesAction.AcceptInvitation(anInvite))

        test.assertEvents(
                InvitesViewEvents.OpenRoom(
                        roomSummary = anInvite,
                        shouldCloseInviteView = false,
                        isInviteAlreadySelected = true
                )
        ).finish()
    }

    @Test
    fun `when invite rejected then membership map is updated and open event posted`() = runTest {
        coEvery { fakeSession.roomService().leaveRoom(any(), any()) } returns Unit

        viewModel.handle(InvitesAction.RejectInvitation(anInvite))

        coVerify {
            fakeSession.roomService().leaveRoom(anInvite.roomId)
        }
    }

    private fun viewModelWith(state: InvitesViewState) {
        InvitesViewModel(
                state,
                session = fakeSession,
                stringProvider = fakeStringProvider.instance,
                drawableProvider = fakeDrawableProvider.instance,
                ).also {
            viewModel = it
            initialState = state
        }
    }
}
