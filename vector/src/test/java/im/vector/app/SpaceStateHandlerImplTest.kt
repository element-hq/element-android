/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import im.vector.app.test.fakes.FakeActiveSessionDataSource
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeUiStateRepository
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.fixtures.RoomSummaryFixture.aRoomSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

internal class SpaceStateHandlerImplTest {

    private val spaceId = "spaceId"
    private val spaceSummary = aRoomSummary(spaceId)
    private val session = FakeSession.withRoomSummary(spaceSummary)

    private val sessionDataSource = FakeActiveSessionDataSource()
    private val uiStateRepository = FakeUiStateRepository()
    private val activeSessionHolder = FakeActiveSessionHolder(session)
    private val analyticsTracker = FakeAnalyticsTracker()
    private val vectorPreferences = FakeVectorPreferences()

    private val spaceStateHandler = SpaceStateHandlerImpl(
            sessionDataSource.instance,
            uiStateRepository,
            activeSessionHolder.instance,
            analyticsTracker,
            vectorPreferences.instance,
    )

    @Before
    fun setup() {
        vectorPreferences.givenSpaceBackstack(emptyList())
    }

    @Test
    fun `given selected space doesn't exist, when getCurrentSpace, then return null`() {
        val currentSpace = spaceStateHandler.getCurrentSpace()

        currentSpace shouldBe null
    }

    @Test
    fun `given selected space exists, when getCurrentSpace, then return selected space`() {
        spaceStateHandler.setCurrentSpace(spaceId, session)

        val currentSpace = spaceStateHandler.getCurrentSpace()

        currentSpace shouldBe spaceSummary
    }

    @Test
    fun `given persistNow is true, when setCurrentSpace, then immediately persist to ui state`() {
        spaceStateHandler.setCurrentSpace(spaceId, session, persistNow = true)

        uiStateRepository.verifyStoreSelectedSpace(spaceId, session)
    }

    @Test
    fun `given persistNow is false, when setCurrentSpace, then don't immediately persist to ui state`() {
        spaceStateHandler.setCurrentSpace(spaceId, session, persistNow = false)

        uiStateRepository.verifyStoreSelectedSpace(spaceId, session, inverse = true)
    }

    @Test
    fun `given not in space and is forward navigation, when setCurrentSpace, then null added to backstack`() {
        spaceStateHandler.setCurrentSpace(spaceId, session, isForwardNavigation = true)

        vectorPreferences.verifySetSpaceBackstack(listOf(null))
    }

    @Test
    fun `given in space and is forward navigation, when setCurrentSpace, then previous space added to backstack`() {
        spaceStateHandler.setCurrentSpace(spaceId, session, isForwardNavigation = true)
        spaceStateHandler.setCurrentSpace("secondSpaceId", session, isForwardNavigation = true)

        vectorPreferences.verifySetSpaceBackstack(listOf(spaceId))
    }

    @Test
    fun `given is not forward navigation, when setCurrentSpace, then previous space not added to backstack`() {
        spaceStateHandler.setCurrentSpace(spaceId, session, isForwardNavigation = false)

        vectorPreferences.verifySetSpaceBackstack(listOf(spaceId), inverse = true)
    }

    @Test
    fun `given navigating to all chats, when setCurrentSpace, then backstack cleared`() {
        spaceStateHandler.setCurrentSpace(spaceId, session)
        spaceStateHandler.setCurrentSpace(null, session)

        vectorPreferences.verifySetSpaceBackstack(emptyList())
    }

    @Test
    fun `when setCurrentSpace, then space is emitted to selectedSpaceFlow`() = runTest {
        spaceStateHandler.setCurrentSpace(spaceId, session)

        val currentSpace = spaceStateHandler.getSelectedSpaceFlow().first().orNull()

        currentSpace shouldBeEqualTo spaceSummary
    }

    @Test
    fun `given current space exists, when getSafeActiveSpaceId, then return current space id`() {
        spaceStateHandler.setCurrentSpace(spaceId, session)

        val activeSpaceId = spaceStateHandler.getSafeActiveSpaceId()

        activeSpaceId shouldBeEqualTo spaceId
    }

    @Test
    fun `given current space doesn't exist, when getSafeActiveSpaceId, then return current null`() {
        val activeSpaceId = spaceStateHandler.getSafeActiveSpaceId()

        activeSpaceId shouldBe null
    }
}
