/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.ui

import android.content.Intent
import im.vector.app.features.roomprofile.polls.detail.ui.RoomPollDetailActivity
import im.vector.app.test.fakes.FakeContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class RoomPollsListNavigatorTest {

    private val fakeContext = FakeContext()
    private val roomPollsListNavigator = RoomPollsListNavigator()

    @Before
    fun setUp() {
        mockkObject(RoomPollDetailActivity.Companion)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given info about poll when goToPollDetails then it starts the correct activity`() {
        // Given
        val aPollId = "pollId"
        val aRoomId = "roomId"
        val isEnded = true
        val intent = givenIntentForPollDetails(aPollId, aRoomId, isEnded)
        fakeContext.givenStartActivity(intent)

        // When
        roomPollsListNavigator.goToPollDetails(fakeContext.instance, aPollId, aRoomId, isEnded)

        // Then
        fakeContext.verifyStartActivity(intent)
    }

    private fun givenIntentForPollDetails(pollId: String, roomId: String, isEnded: Boolean): Intent {
        val intent = mockk<Intent>()
        every { RoomPollDetailActivity.newIntent(fakeContext.instance, pollId, roomId, isEnded) } returns intent
        return intent
    }
}
