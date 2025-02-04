/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
