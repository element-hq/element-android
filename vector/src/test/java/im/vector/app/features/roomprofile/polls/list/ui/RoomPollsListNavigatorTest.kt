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
